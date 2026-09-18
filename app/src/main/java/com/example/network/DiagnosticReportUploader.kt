package com.example.network

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Result report structure for diagnostic findings.
 * Includes client environment, network state, target diagnostics, and aggregate statistics.
 */
object DiagnosticReportUploader {

    private const val TAG = "ReportUploader"
    private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    sealed class UploadResult {
        data class Success(val statusCode: Int, val message: String, val responseBody: String?) : UploadResult()
        data class Failed(val statusCode: Int?, val errorMessage: String) : UploadResult()
    }

    /**
     * Builds standard JSON payload following network diagnostics telemetry schema:
     * - taskId / reportId / timestamp
     * - clientInfo (OS version, device model, app version)
     * - networkInfo (network type, local IPs, DNS servers, gateway)
     * - summary (total, healthy, failed, avgLatencyMs)
     * - results: List of target diagnostic results
     */
    fun buildReportPayloadJson(
        context: Context,
        netInfo: NetworkInterfaceInfo?,
        results: List<TargetInspectionResult>,
        taskId: String = UUID.randomUUID().toString()
    ): String {
        val root = JSONObject()
        val timestamp = System.currentTimeMillis()

        root.put("report_id", UUID.randomUUID().toString())
        root.put("task_id", taskId)
        root.put("report_type", "DNS_SERVER_INSPECTION")
        root.put("timestamp", timestamp)
        root.put("iso_timestamp", ISO_DATE_FORMAT.format(Date(timestamp)))

        // Client Environment Info
        val clientInfo = JSONObject().apply {
            put("platform", "Android")
            put("os_version", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("device_manufacturer", Build.MANUFACTURER)
            put("device_model", Build.MODEL)
            put("app_id", context.packageName)
            put("app_version", "1.0")
        }
        root.put("client_info", clientInfo)

        // Network Interface State
        val networkObj = JSONObject().apply {
            put("connected", netInfo?.isConnected ?: false)
            put("network_type", netInfo?.networkType ?: "UNKNOWN")
            put("local_ipv4", netInfo?.localIpv4 ?: "")
            put("local_ipv6", netInfo?.localIpv6 ?: "")
            put("gateway", netInfo?.gatewayIp ?: "")
            put("local_dns_servers", JSONArray(netInfo?.localDnsServers ?: emptyList<String>()))
            if (netInfo?.wifiSsid != null) {
                put("wifi_ssid", netInfo.wifiSsid)
                put("link_speed_mbps", netInfo.linkSpeedMbps)
            }
        }
        root.put("network_info", networkObj)

        // Aggregate summary metrics
        val total = results.size
        val healthy = results.count { it.isConnected }
        val failed = total - healthy
        val avgLatency = if (results.isNotEmpty()) results.map { it.totalDurationMs }.average().toInt() else 0

        val summaryObj = JSONObject().apply {
            put("total_targets", total)
            put("healthy_count", healthy)
            put("failed_count", failed)
            put("avg_latency_ms", avgLatency)
            put("overall_status", if (failed == 0) "HEALTHY" else if (healthy > 0) "PARTIAL" else "FAILED")
        }
        root.put("summary", summaryObj)

        // Target items list
        val resultsArray = JSONArray()
        results.forEach { item ->
            val targetObj = JSONObject().apply {
                put("id", item.id)
                put("raw_input", item.rawInput)
                put("host", item.host)
                put("port", item.port)
                put("is_https", item.isHttps)
                put("connected", item.isConnected)
                put("total_duration_ms", item.totalDurationMs)
                put("resolved_ip", item.resolvedIp ?: "")
                put("all_resolved_ips", JSONArray(item.allResolvedIps))

                // DNS sub-result
                put("dns", JSONObject().apply {
                    put("status", item.dnsResult.status.name)
                    put("latency_ms", item.dnsResult.latencyMs)
                    put("summary", item.dnsResult.summary)
                    if (item.dnsResult.errorMessage != null) {
                        put("error", item.dnsResult.errorMessage)
                    }
                })

                // TCP sub-result
                put("tcp", JSONObject().apply {
                    put("status", item.tcpResult.status.name)
                    put("latency_ms", item.tcpResult.latencyMs)
                    put("summary", item.tcpResult.summary)
                    if (item.tcpResult.errorMessage != null) {
                        put("error", item.tcpResult.errorMessage)
                    }
                })

                // TLS sub-result
                put("tls", JSONObject().apply {
                    put("status", item.tlsResult.status.name)
                    put("latency_ms", item.tlsResult.latencyMs)
                    put("summary", item.tlsResult.summary)
                    if (item.tlsResult.errorMessage != null) {
                        put("error", item.tlsResult.errorMessage)
                    }
                    if (item.certExpiryDays != null) {
                        put("cert_expiry_days", item.certExpiryDays)
                    }
                    if (item.certValidUntil != null) {
                        put("cert_valid_until", item.certValidUntil)
                    }
                    if (item.certIssuer != null) {
                        put("cert_issuer", item.certIssuer)
                    }
                })

                // HTTP sub-result
                put("http", JSONObject().apply {
                    put("status", item.httpResult.status.name)
                    put("latency_ms", item.httpResult.latencyMs)
                    put("summary", item.httpResult.summary)
                    if (item.httpStatusLine != null) {
                        put("status_line", item.httpStatusLine)
                    }
                    if (item.httpResult.errorMessage != null) {
                        put("error", item.httpResult.errorMessage)
                    }
                })
            }
            resultsArray.put(targetObj)
        }
        root.put("results", resultsArray)

        return root.toString(2)
    }

    /**
     * Uploads the diagnostic report payload JSON via HTTP/HTTPS POST.
     */
    suspend fun uploadReport(
        endpointUrl: String,
        jsonPayload: String,
        timeoutMs: Int = 10000
    ): UploadResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("User-Agent", "NetDiag-Android/1.0")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }

            val statusCode = conn.responseCode
            val responseStream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

            if (statusCode in 200..299) {
                Log.d(TAG, "Report uploaded successfully. HTTP $statusCode: $responseText")
                UploadResult.Success(
                    statusCode = statusCode,
                    message = "HTTP $statusCode OK",
                    responseBody = responseText
                )
            } else {
                Log.w(TAG, "Report upload received error status: $statusCode: $responseText")
                UploadResult.Failed(
                    statusCode = statusCode,
                    errorMessage = "HTTP $statusCode ${conn.responseMessage ?: ""}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading diagnostic report to $endpointUrl: ${e.message}", e)
            UploadResult.Failed(
                statusCode = null,
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        } finally {
            conn?.disconnect()
        }
    }
}
