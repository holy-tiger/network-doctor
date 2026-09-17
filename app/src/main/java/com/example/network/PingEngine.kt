package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PingPacket(
    val seq: Int,
    val bytes: Int,
    val rttMs: Float,
    val ttl: Int,
    val ip: String,
    val isSuccess: Boolean
)

data class PingSummary(
    val host: String,
    val ip: String,
    val transmitted: Int,
    val received: Int,
    val lossPercentage: Float,
    val minRtt: Float,
    val avgRtt: Float,
    val maxRtt: Float,
    val mdevRtt: Float,
    val packets: List<PingPacket>
)

class PingEngine {

    suspend fun runPing(
        host: String,
        count: Int = 4,
        timeoutSec: Int = 2
    ): Flow<Pair<PingPacket, PingSummary?>> = flow {
        val resolvedIp = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(host).hostAddress ?: host
            } catch (_: Exception) {
                host
            }
        }

        val packets = mutableListOf<PingPacket>()
        var processWorked = false

        // Attempt ICMP ping process first
        try {
            val process = Runtime.getRuntime().exec("ping -c $count -W $timeoutSec $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var seqCounter = 1

            while (reader.readLine().also { line = it } != null) {
                val curLine = line ?: continue
                // e.g. "64 bytes from 8.8.8.8: icmp_seq=1 ttl=118 time=14.3 ms"
                if (curLine.contains("bytes from") && curLine.contains("time=")) {
                    processWorked = true
                    val rtt = extractRtt(curLine)
                    val ttl = extractTtl(curLine)
                    val bytes = extractBytes(curLine)

                    val packet = PingPacket(
                        seq = seqCounter++,
                        bytes = bytes,
                        rttMs = rtt,
                        ttl = ttl,
                        ip = resolvedIp,
                        isSuccess = true
                    )
                    packets.add(packet)
                    emit(Pair(packet, null))
                }
            }
            process.waitFor()
        } catch (_: Exception) {
            processWorked = false
        }

        // If system ping command was restricted or returned no packets, fallback to socket probe
        if (!processWorked || packets.isEmpty()) {
            for (i in 1..count) {
                val start = System.nanoTime()
                var success = false
                var rtt = 0f
                try {
                    val socket = Socket()
                    val socketAddress = InetSocketAddress(host, 443) // probe HTTPS port or fallback 80
                    socket.connect(socketAddress, timeoutSec * 1000)
                    val end = System.nanoTime()
                    rtt = ((end - start) / 1_000_000f)
                    socket.close()
                    success = true
                } catch (_: Exception) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(host, 80), timeoutSec * 1000)
                        val end = System.nanoTime()
                        rtt = ((end - start) / 1_000_000f)
                        socket.close()
                        success = true
                    } catch (_: Exception) {
                        success = false
                    }
                }

                val packet = PingPacket(
                    seq = i,
                    bytes = if (success) 64 else 0,
                    rttMs = if (success) (rtt * 10).roundToInt() / 10f else -1f,
                    ttl = if (success) 64 else 0,
                    ip = resolvedIp,
                    isSuccess = success
                )
                packets.add(packet)
                emit(Pair(packet, null))
                kotlinx.coroutines.delay(150)
            }
        }

        val transmitted = packets.size
        val successful = packets.filter { it.isSuccess && it.rttMs >= 0 }
        val received = successful.size
        val lossPercentage = if (transmitted > 0) ((transmitted - received).toFloat() / transmitted) * 100f else 100f

        val rtts = successful.map { it.rttMs }
        val minRtt = if (rtts.isNotEmpty()) rtts.minOrNull() ?: 0f else 0f
        val maxRtt = if (rtts.isNotEmpty()) rtts.maxOrNull() ?: 0f else 0f
        val avgRtt = if (rtts.isNotEmpty()) rtts.average().toFloat() else 0f

        // Calculate mdev (mean deviation / standard deviation)
        val mdev = if (rtts.size > 1) {
            val variance = rtts.map { (it - avgRtt) * (it - avgRtt) }.average()
            sqrt(variance).toFloat()
        } else 0f

        val summary = PingSummary(
            host = host,
            ip = resolvedIp,
            transmitted = transmitted,
            received = received,
            lossPercentage = (lossPercentage * 10).roundToInt() / 10f,
            minRtt = (minRtt * 10).roundToInt() / 10f,
            avgRtt = (avgRtt * 10).roundToInt() / 10f,
            maxRtt = (maxRtt * 10).roundToInt() / 10f,
            mdevRtt = (mdev * 10).roundToInt() / 10f,
            packets = packets
        )

        // Final emit with complete summary
        emit(Pair(packets.last(), summary))
    }.flowOn(Dispatchers.IO)

    private fun extractRtt(line: String): Float {
        val match = Regex("time=([0-9.]+)").find(line)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    private fun extractTtl(line: String): Int {
        val match = Regex("ttl=([0-9]+)").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 64
    }

    private fun extractBytes(line: String): Int {
        val match = Regex("([0-9]+) bytes").find(line)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 64
    }
}
