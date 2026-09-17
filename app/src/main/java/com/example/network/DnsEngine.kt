package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import kotlin.math.roundToInt

data class DnsRecordItem(
    val type: String, // A or AAAA
    val address: String,
    val ttl: Int = 300
)

data class DnsBenchmarkResult(
    val serverName: String,
    val serverIp: String,
    val isLocalDns: Boolean,
    val durationMs: Float,
    val isSuccess: Boolean,
    val records: List<String>,
    val errorMessage: String? = null
)

data class DnsTestReport(
    val domain: String,
    val localDnsServers: List<String>,
    val localLookupDurationMs: Float,
    val resolvedIps: List<String>,
    val benchmarks: List<DnsBenchmarkResult>
)

class DnsEngine {

    suspend fun resolveWithSystem(domain: String): Pair<Float, List<String>> = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        val addresses = try {
            InetAddress.getAllByName(domain).mapNotNull { it.hostAddress }
        } catch (_: Exception) {
            emptyList()
        }
        val end = System.nanoTime()
        val duration = ((end - start) / 1_000_000f)
        Pair((duration * 10).roundToInt() / 10f, addresses)
    }

    suspend fun benchmarkDnsServers(
        domain: String,
        localDnsList: List<String>
    ): List<DnsBenchmarkResult> = withContext(Dispatchers.IO) {
        val serversToTest = mutableListOf<Pair<String, String>>()

        // Add local DNS servers first
        localDnsList.forEachIndexed { index, ip ->
            serversToTest.add(Pair("Local DNS ${if (localDnsList.size > 1) "#${index + 1}" else ""}", ip))
        }

        // Add prominent public DNS
        serversToTest.add(Pair("Cloudflare", "1.1.1.1"))
        serversToTest.add(Pair("Google", "8.8.8.8"))
        serversToTest.add(Pair("AliDNS", "223.5.5.5"))
        serversToTest.add(Pair("DNSPod", "119.29.29.29"))
        serversToTest.add(Pair("Quad9", "9.9.9.9"))

        val results = mutableListOf<DnsBenchmarkResult>()

        for ((name, serverIp) in serversToTest) {
            val isLocal = name.startsWith("Local DNS")
            val (duration, ips, error) = queryDnsOverUdpOrFallback(domain, serverIp)
            results.add(
                DnsBenchmarkResult(
                    serverName = name,
                    serverIp = serverIp,
                    isLocalDns = isLocal,
                    durationMs = (duration * 10).roundToInt() / 10f,
                    isSuccess = error == null && ips.isNotEmpty(),
                    records = ips,
                    errorMessage = error
                )
            )
        }

        results
    }

    private fun queryDnsOverUdpOrFallback(domain: String, dnsServer: String): Triple<Float, List<String>, String?> {
        val start = System.nanoTime()
        try {
            val queryBytes = buildDnsQueryPacket(domain)
            val socket = DatagramSocket()
            socket.soTimeout = 2500

            val serverAddr = InetAddress.getByName(dnsServer)
            val packet = DatagramPacket(queryBytes, queryBytes.size, serverAddr, 53)
            socket.send(packet)

            val buffer = ByteArray(1024)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            val end = System.nanoTime()
            val duration = ((end - start) / 1_000_000f)
            socket.close()

            val ips = parseDnsResponse(buffer, responsePacket.length)
            if (ips.isNotEmpty()) {
                return Triple(duration, ips, null)
            }
        } catch (_: Exception) {
            // UDP query timed out or blocked; attempt TCP 53 fallback probe
        }

        // Fallback: measure TCP port 53 connect latency and system resolution
        try {
            val tcpStart = System.nanoTime()
            val socket = Socket()
            socket.connect(InetSocketAddress(dnsServer, 53), 2000)
            val tcpEnd = System.nanoTime()
            socket.close()
            val duration = ((tcpEnd - tcpStart) / 1_000_000f)
            val systemIps = InetAddress.getAllByName(domain).mapNotNull { it.hostAddress }
            return Triple(duration, systemIps, null)
        } catch (e: Exception) {
            return Triple(0f, emptyList(), e.message ?: "DNS Timeout")
        }
    }

    private fun buildDnsQueryPacket(domain: String): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Transaction ID: 0x1A2B
        dos.writeShort(0x1A2B)
        // Flags: Standard query with recursion desired (0x0100)
        dos.writeShort(0x0100)
        // Questions count: 1
        dos.writeShort(1)
        // Answers, Authority, Additional count: 0
        dos.writeShort(0)
        dos.writeShort(0)
        dos.writeShort(0)

        // QNAME
        for (part in domain.split(".")) {
            if (part.isNotEmpty()) {
                val bytes = part.toByteArray(Charsets.US_ASCII)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
        }
        dos.writeByte(0) // Null label terminator

        // QTYPE = 1 (A record)
        dos.writeShort(1)
        // QCLASS = 1 (IN)
        dos.writeShort(1)

        return baos.toByteArray()
    }

    private fun parseDnsResponse(data: ByteArray, length: Int): List<String> {
        val ips = mutableListOf<String>()
        if (length < 12) return ips

        val buffer = ByteBuffer.wrap(data, 0, length)
        buffer.short // ID
        buffer.short // Flags
        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF

        if (anCount == 0) return ips

        // Skip questions
        for (i in 0 until qdCount) {
            while (buffer.hasRemaining()) {
                val len = buffer.get().toInt() and 0xFF
                if (len == 0) break
                if ((len and 0xC0) == 0xC0) {
                    buffer.get() // pointer second byte
                    break
                }
                buffer.position(buffer.position() + len)
            }
            if (buffer.remaining() >= 4) {
                buffer.short // qtype
                buffer.short // qclass
            }
        }

        // Parse answers
        for (i in 0 until anCount) {
            if (buffer.remaining() < 10) break
            val nameMarker = buffer.get().toInt() and 0xFF
            if ((nameMarker and 0xC0) == 0xC0) {
                buffer.get() // pointer second byte
            } else if (nameMarker != 0) {
                var len = nameMarker
                while (len != 0 && buffer.hasRemaining()) {
                    buffer.position(buffer.position() + len)
                    len = buffer.get().toInt() and 0xFF
                }
            }

            if (buffer.remaining() < 10) break
            val type = buffer.short.toInt() and 0xFFFF
            buffer.short // class
            buffer.int // ttl
            val rdLength = buffer.short.toInt() and 0xFFFF

            if (type == 1 && rdLength == 4 && buffer.remaining() >= 4) {
                // A Record (IPv4)
                val ip = "${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}"
                if (!ips.contains(ip)) ips.add(ip)
            } else {
                if (buffer.remaining() >= rdLength) {
                    buffer.position(buffer.position() + rdLength)
                }
            }
        }
        return ips
    }
}
