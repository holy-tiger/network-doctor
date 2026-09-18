package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class DiagnosticStatus {
    SUCCESS,
    WARNING,
    ERROR,
    PENDING
}

data class DiagnosticSubResult(
    val status: DiagnosticStatus = DiagnosticStatus.PENDING,
    val latencyMs: Long = 0,
    val summary: String = "",
    val details: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

data class TargetInspectionResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawInput: String,
    val host: String,
    val port: Int = 443,
    val isHttps: Boolean = true,
    val isConnected: Boolean = false,
    val totalDurationMs: Long = 0,
    val resolvedIp: String? = null,
    val allResolvedIps: List<String> = emptyList(),
    val dnsResult: DiagnosticSubResult = DiagnosticSubResult(),
    val tcpResult: DiagnosticSubResult = DiagnosticSubResult(),
    val tlsResult: DiagnosticSubResult = DiagnosticSubResult(),
    val httpResult: DiagnosticSubResult = DiagnosticSubResult(),
    val certIssuer: String? = null,
    val certExpiryDays: Long? = null,
    val certValidUntil: String? = null,
    val httpStatusLine: String? = null,
    val httpHeaders: Map<String, String> = emptyMap(),
    val isExpanded: Boolean = false
)

class ServerDiagnosticEngine {

    suspend fun inspectTarget(rawTarget: String): TargetInspectionResult = withContext(Dispatchers.IO) {
        val totalStart = System.currentTimeMillis()
        val parsed = parseTarget(rawTarget)
        val host = parsed.host
        val port = parsed.port
        val isHttps = parsed.isHttps

        if (host.isBlank()) {
            return@withContext TargetInspectionResult(
                rawInput = rawTarget,
                host = rawTarget,
                dnsResult = DiagnosticSubResult(DiagnosticStatus.ERROR, summary = "域名不能为空", errorMessage = "Empty host")
            )
        }

        // 1. DNS Resolution
        val dnsStart = System.currentTimeMillis()
        var resolvedIps = emptyList<String>()
        var primaryIp: String? = null
        var dnsResult: DiagnosticSubResult

        try {
            val addrs = InetAddress.getAllByName(host)
            resolvedIps = addrs.mapNotNull { it.hostAddress }
            primaryIp = resolvedIps.firstOrNull()
            val dnsTime = System.currentTimeMillis() - dnsStart

            if (primaryIp != null) {
                dnsResult = DiagnosticSubResult(
                    status = DiagnosticStatus.SUCCESS,
                    latencyMs = dnsTime,
                    summary = "IP: $primaryIp",
                    details = mapOf("A/AAAA Records" to resolvedIps.joinToString(", "))
                )
            } else {
                dnsResult = DiagnosticSubResult(
                    status = DiagnosticStatus.ERROR,
                    latencyMs = dnsTime,
                    summary = "解析无记录",
                    errorMessage = "No IP records found"
                )
            }
        } catch (e: Exception) {
            val dnsTime = System.currentTimeMillis() - dnsStart
            dnsResult = DiagnosticSubResult(
                status = DiagnosticStatus.ERROR,
                latencyMs = dnsTime,
                summary = "解析失败",
                errorMessage = e.message ?: "Unknown host"
            )
        }

        // 2. TCP Handshake
        var tcpResult: DiagnosticSubResult = DiagnosticSubResult()
        var tcpSuccess = false
        if (primaryIp != null && dnsResult.status == DiagnosticStatus.SUCCESS) {
            val tcpStart = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(primaryIp, port), 3500)
                    val tcpTime = System.currentTimeMillis() - tcpStart
                    tcpSuccess = true
                    tcpResult = DiagnosticSubResult(
                        status = DiagnosticStatus.SUCCESS,
                        latencyMs = tcpTime,
                        summary = "端口: $port"
                    )
                }
            } catch (e: Exception) {
                val tcpTime = System.currentTimeMillis() - tcpStart
                tcpResult = DiagnosticSubResult(
                    status = DiagnosticStatus.ERROR,
                    latencyMs = tcpTime,
                    summary = "连接超时/拒绝",
                    errorMessage = e.message ?: "Connect timeout"
                )
            }
        } else {
            tcpResult = DiagnosticSubResult(
                status = DiagnosticStatus.ERROR,
                summary = "未连通 (DNS失败)",
                errorMessage = "Skipped"
            )
        }

        // 3. TLS Certificate Inspection
        var tlsResult: DiagnosticSubResult = DiagnosticSubResult()
        var certIssuer: String? = null
        var certExpiryDays: Long? = null
        var certValidUntil: String? = null

        if (isHttps && tcpSuccess && primaryIp != null) {
            val tlsStart = System.currentTimeMillis()
            try {
                var capturedChain: Array<X509Certificate>? = null
                var certError: String? = null

                val trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        capturedChain = chain?.filterIsInstance<X509Certificate>()?.toTypedArray()
                        try {
                            chain?.firstOrNull()?.checkValidity()
                        } catch (e: Exception) {
                            certError = e.message
                        }
                    }
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

                val sslSocket = sslContext.socketFactory.createSocket() as SSLSocket
                sslSocket.soTimeout = 4000
                sslSocket.connect(InetSocketAddress(primaryIp, port), 4000)
                sslSocket.startHandshake()

                val tlsTime = System.currentTimeMillis() - tlsStart
                val cert = capturedChain?.firstOrNull() ?: sslSocket.session.peerCertificates.firstOrNull() as? X509Certificate

                if (cert != null) {
                    val now = System.currentTimeMillis()
                    val notAfter = cert.notAfter.time
                    val daysRemaining = (notAfter - now) / (1000 * 60 * 60 * 24)
                    certExpiryDays = daysRemaining
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    certValidUntil = sdf.format(cert.notAfter)
                    certIssuer = cert.issuerX500Principal?.name ?: "Unknown"

                    if (daysRemaining < 0 || certError != null) {
                        tlsResult = DiagnosticSubResult(
                            status = DiagnosticStatus.ERROR,
                            latencyMs = tlsTime,
                            summary = if (daysRemaining < 0) "证书已过期" else "证书异常",
                            errorMessage = certError ?: "Expired"
                        )
                    } else {
                        tlsResult = DiagnosticSubResult(
                            status = DiagnosticStatus.SUCCESS,
                            latencyMs = tlsTime,
                            summary = "有效剩 $daysRemaining 天"
                        )
                    }
                } else {
                    tlsResult = DiagnosticSubResult(
                        status = DiagnosticStatus.WARNING,
                        latencyMs = tlsTime,
                        summary = "无对端证书",
                        errorMessage = "No peer cert"
                    )
                }
                sslSocket.close()
            } catch (e: Exception) {
                val tlsTime = System.currentTimeMillis() - tlsStart
                tlsResult = DiagnosticSubResult(
                    status = DiagnosticStatus.ERROR,
                    latencyMs = tlsTime,
                    summary = "握手失败",
                    errorMessage = e.message ?: "TLS failed"
                )
            }
        } else if (!isHttps) {
            tlsResult = DiagnosticSubResult(
                status = DiagnosticStatus.SUCCESS,
                latencyMs = 0,
                summary = "无需TLS (HTTP)"
            )
        } else {
            tlsResult = DiagnosticSubResult(
                status = DiagnosticStatus.ERROR,
                summary = "跳过 (TCP未通)",
                errorMessage = "TCP failed"
            )
        }

        // 4. HTTP Response
        var httpResult: DiagnosticSubResult = DiagnosticSubResult()
        var httpStatusLine: String? = null
        val httpHeaders = mutableMapOf<String, String>()

        if (tcpSuccess) {
            val httpStart = System.currentTimeMillis()
            try {
                val protocol = if (isHttps) "https" else "http"
                val url = URL("$protocol://$host:$port/")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false // Keep 301/302 as-is for accurate testing
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "HEAD"
                conn.setRequestProperty("User-Agent", "NetDiag/2.0 (Android Mobile Diagnostic)")

                val responseCode = try {
                    conn.responseCode
                } catch (e: Exception) {
                    // Fallback to GET if HEAD method not allowed
                    val getConn = url.openConnection() as HttpURLConnection
                    getConn.instanceFollowRedirects = false
                    getConn.connectTimeout = 4000
                    getConn.readTimeout = 4000
                    getConn.requestMethod = "GET"
                    getConn.setRequestProperty("User-Agent", "NetDiag/2.0")
                    getConn.responseCode
                }

                val responseMsg = conn.responseMessage ?: ""
                httpStatusLine = "$responseCode $responseMsg".trim()
                val httpTime = System.currentTimeMillis() - httpStart

                conn.headerFields.forEach { (key, values) ->
                    if (key != null && values.isNotEmpty()) {
                        httpHeaders[key] = values.joinToString("; ")
                    }
                }

                val status = when (responseCode) {
                    in 200..399 -> DiagnosticStatus.SUCCESS
                    in 400..499 -> DiagnosticStatus.WARNING
                    else -> DiagnosticStatus.ERROR
                }

                httpResult = DiagnosticSubResult(
                    status = status,
                    latencyMs = httpTime,
                    summary = "HTTP 状态: $responseCode $responseMsg".trim()
                )
                conn.disconnect()
            } catch (e: Exception) {
                val httpTime = System.currentTimeMillis() - httpStart
                httpResult = DiagnosticSubResult(
                    status = DiagnosticStatus.ERROR,
                    latencyMs = httpTime,
                    summary = "请求异常/超时",
                    errorMessage = e.message ?: "HTTP error"
                )
            }
        } else {
            httpResult = DiagnosticSubResult(
                status = DiagnosticStatus.ERROR,
                summary = "跳过 (TCP未通)",
                errorMessage = "TCP failed"
            )
        }

        val totalDuration = System.currentTimeMillis() - totalStart
        val overallConnected = dnsResult.status == DiagnosticStatus.SUCCESS &&
                tcpResult.status == DiagnosticStatus.SUCCESS &&
                (tlsResult.status == DiagnosticStatus.SUCCESS || !isHttps) &&
                (httpResult.status == DiagnosticStatus.SUCCESS || httpResult.status == DiagnosticStatus.WARNING)

        TargetInspectionResult(
            rawInput = rawTarget,
            host = host,
            port = port,
            isHttps = isHttps,
            isConnected = overallConnected,
            totalDurationMs = totalDuration,
            resolvedIp = primaryIp,
            allResolvedIps = resolvedIps,
            dnsResult = dnsResult,
            tcpResult = tcpResult,
            tlsResult = tlsResult,
            httpResult = httpResult,
            certIssuer = certIssuer,
            certExpiryDays = certExpiryDays,
            certValidUntil = certValidUntil,
            httpStatusLine = httpStatusLine,
            httpHeaders = httpHeaders
        )
    }

    private data class ParsedTarget(val host: String, val port: Int, val isHttps: Boolean)

    private fun parseTarget(raw: String): ParsedTarget {
        var trimmed = raw.trim()
        var isHttps = true
        var defaultPort = 443

        if (trimmed.startsWith("http://", ignoreCase = true)) {
            isHttps = false
            defaultPort = 80
            trimmed = trimmed.substring(7)
        } else if (trimmed.startsWith("https://", ignoreCase = true)) {
            isHttps = true
            defaultPort = 443
            trimmed = trimmed.substring(8)
        }

        // Strip path, query params, hash
        val slashIndex = trimmed.indexOfAny(charArrayOf('/', '?', '#'))
        if (slashIndex != -1) {
            trimmed = trimmed.substring(0, slashIndex)
        }

        var host = trimmed
        var port = defaultPort

        // Check if port is appended like host:port
        val colonIndex = trimmed.lastIndexOf(':')
        if (colonIndex != -1 && !trimmed.contains(']')) { // Not IPv6 literal
            val portStr = trimmed.substring(colonIndex + 1)
            val parsedPort = portStr.toIntOrNull()
            if (parsedPort != null && parsedPort in 1..65535) {
                port = parsedPort
                host = trimmed.substring(0, colonIndex)
                if (port == 80) isHttps = false
            }
        }

        return ParsedTarget(host = host.trim(), port = port, isHttps = isHttps)
    }
}
