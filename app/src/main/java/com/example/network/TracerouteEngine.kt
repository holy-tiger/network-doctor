package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import kotlin.math.roundToInt

data class TracerouteHop(
    val hopIndex: Int,
    val ip: String,
    val hostname: String?,
    val rttMs: Float,
    val isReached: Boolean,
    val isTimeout: Boolean
)

data class TracerouteReport(
    val targetHost: String,
    val resolvedTargetIp: String,
    val totalHops: Int,
    val isTargetReached: Boolean,
    val hops: List<TracerouteHop>
)

class TracerouteEngine {

    suspend fun runTraceroute(
        targetHost: String,
        maxHops: Int = 30,
        timeoutSec: Int = 2
    ): Flow<Pair<TracerouteHop, TracerouteReport?>> = flow {
        val resolvedTargetIp = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(targetHost).hostAddress ?: targetHost
            } catch (_: Exception) {
                targetHost
            }
        }

        val hops = mutableListOf<TracerouteHop>()
        var destinationReached = false

        for (ttl in 1..maxHops) {
            if (destinationReached) break

            val hopResult = probeHop(targetHost, resolvedTargetIp, ttl, timeoutSec)
            hops.add(hopResult)

            if (hopResult.isReached || hopResult.ip == resolvedTargetIp) {
                destinationReached = true
            }

            if (destinationReached || ttl == maxHops) {
                val report = TracerouteReport(
                    targetHost = targetHost,
                    resolvedTargetIp = resolvedTargetIp,
                    totalHops = hops.size,
                    isTargetReached = destinationReached,
                    hops = hops
                )
                emit(Pair(hopResult, report))
                break
            } else {
                emit(Pair(hopResult, null))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun probeHop(
        host: String,
        targetIp: String,
        ttl: Int,
        timeoutSec: Int
    ): TracerouteHop {
        val start = System.nanoTime()
        var hopIp: String? = null
        var isReached = false
        var rttMs = 0f

        try {
            // Using ping with TTL (-t on Linux ping)
            val process = Runtime.getRuntime().exec("ping -c 1 -t $ttl -W $timeoutSec $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val cur = line ?: continue

                // Check for Time to live exceeded: "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                if (cur.contains("Time to live exceeded", ignoreCase = true) || cur.contains("Time exceeded", ignoreCase = true)) {
                    hopIp = extractIpFromPingLine(cur)
                }
                // Check if target responded: "64 bytes from 8.8.8.8: icmp_seq=1 ttl=... time=..."
                else if (cur.contains("bytes from", ignoreCase = true) && cur.contains("time=")) {
                    hopIp = extractIpFromPingLine(cur) ?: targetIp
                    isReached = true
                    val rttMatch = Regex("time=([0-9.]+)").find(cur)
                    rttMatch?.groupValues?.get(1)?.toFloatOrNull()?.let {
                        rttMs = it
                    }
                }
            }
            process.waitFor()
        } catch (_: Exception) {}

        val end = System.nanoTime()
        if (rttMs <= 0f) {
            rttMs = ((end - start) / 1_000_000f)
        }

        if (hopIp != null && (hopIp == targetIp || hopIp == host)) {
            isReached = true
        }

        val hostname = if (hopIp != null) {
            try {
                val addr = InetAddress.getByName(hopIp)
                val canonical = addr.canonicalHostName
                if (canonical != hopIp) canonical else null
            } catch (_: Exception) {
                null
            }
        } else null

        return TracerouteHop(
            hopIndex = ttl,
            ip = hopIp ?: "* * *",
            hostname = hostname,
            rttMs = if (hopIp != null) (rttMs * 10).roundToInt() / 10f else -1f,
            isReached = isReached,
            isTimeout = hopIp == null
        )
    }

    private fun extractIpFromPingLine(line: String): String? {
        // "From 10.0.2.2: icmp_seq=1" or "64 bytes from 1.1.1.1: icmp_seq=1"
        val match = Regex("(?:From|from)\\s+([a-zA-Z0-9.\\-:]+?)(?::|\\s)").find(line)
        val extracted = match?.groupValues?.get(1)?.replace(Regex("[():]"), "")
        if (extracted != null && (extracted.contains(".") || extracted.contains(":"))) {
            return extracted
        }
        // Fallback IPv4 regex
        val ipMatch = Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b").find(line)
        return ipMatch?.value
    }
}
