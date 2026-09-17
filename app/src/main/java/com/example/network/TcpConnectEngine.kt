package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.math.roundToInt

data class TcpProbeResult(
    val attempt: Int,
    val host: String,
    val port: Int,
    val resolvedIp: String,
    val handshakeTimeMs: Float,
    val isConnected: Boolean,
    val statusText: String
)

data class TcpSummary(
    val host: String,
    val port: Int,
    val resolvedIp: String,
    val attempts: Int,
    val successes: Int,
    val avgHandshakeMs: Float,
    val minHandshakeMs: Float,
    val maxHandshakeMs: Float,
    val status: String,
    val results: List<TcpProbeResult>
)

class TcpConnectEngine {

    suspend fun runTcpProbes(
        host: String,
        port: Int,
        attempts: Int = 4,
        timeoutMs: Int = 3000
    ): Flow<Pair<TcpProbeResult, TcpSummary?>> = flow {
        val resolvedIp = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(host).hostAddress ?: host
            } catch (_: Exception) {
                host
            }
        }

        val results = mutableListOf<TcpProbeResult>()

        for (i in 1..attempts) {
            val start = System.nanoTime()
            var isConnected = false
            var statusText = "Connected"
            var handshakeTime = 0f

            try {
                val socket = Socket()
                val socketAddr = InetSocketAddress(host, port)
                socket.connect(socketAddr, timeoutMs)
                val end = System.nanoTime()
                handshakeTime = ((end - start) / 1_000_000f)
                isConnected = true
                statusText = "SYN-ACK OK"
                socket.close()
            } catch (_: SocketTimeoutException) {
                handshakeTime = timeoutMs.toFloat()
                statusText = "Timeout (> $timeoutMs ms)"
            } catch (e: ConnectException) {
                statusText = "Refused: ${e.message ?: "Connection Refused"}"
            } catch (e: Exception) {
                statusText = "Error: ${e.message ?: "Unreachable"}"
            }

            val probe = TcpProbeResult(
                attempt = i,
                host = host,
                port = port,
                resolvedIp = resolvedIp,
                handshakeTimeMs = if (isConnected) (handshakeTime * 10).roundToInt() / 10f else -1f,
                isConnected = isConnected,
                statusText = statusText
            )
            results.add(probe)
            emit(Pair(probe, null))
            kotlinx.coroutines.delay(100)
        }

        val successful = results.filter { it.isConnected && it.handshakeTimeMs >= 0 }
        val rtts = successful.map { it.handshakeTimeMs }
        val minTime = if (rtts.isNotEmpty()) rtts.minOrNull() ?: 0f else 0f
        val maxTime = if (rtts.isNotEmpty()) rtts.maxOrNull() ?: 0f else 0f
        val avgTime = if (rtts.isNotEmpty()) rtts.average().toFloat() else 0f

        val summary = TcpSummary(
            host = host,
            port = port,
            resolvedIp = resolvedIp,
            attempts = attempts,
            successes = successful.size,
            avgHandshakeMs = (avgTime * 10).roundToInt() / 10f,
            minHandshakeMs = (minTime * 10).roundToInt() / 10f,
            maxHandshakeMs = (maxTime * 10).roundToInt() / 10f,
            status = if (successful.isNotEmpty()) "OPEN" else "CLOSED / FILTERED",
            results = results
        )

        emit(Pair(results.last(), summary))
    }.flowOn(Dispatchers.IO)
}
