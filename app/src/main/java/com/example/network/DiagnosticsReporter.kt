package com.example.network

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsReporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun createPingReportJson(
        netInfo: NetworkInterfaceInfo,
        summary: PingSummary
    ): String {
        val root = JSONObject()
        root.put("report_type", "PING")
        root.put("timestamp", System.currentTimeMillis())
        root.put("formatted_time", dateFormat.format(Date()))
        
        val netObj = networkInfoToJson(netInfo)
        root.put("network_interface", netObj)

        val pingObj = JSONObject()
        pingObj.put("target_host", summary.host)
        pingObj.put("resolved_ip", summary.ip)
        pingObj.put("transmitted", summary.transmitted)
        pingObj.put("received", summary.received)
        pingObj.put("packet_loss_pct", summary.lossPercentage)
        pingObj.put("min_rtt_ms", summary.minRtt)
        pingObj.put("avg_rtt_ms", summary.avgRtt)
        pingObj.put("max_rtt_ms", summary.maxRtt)
        pingObj.put("mdev_ms", summary.mdevRtt)

        val packetsArr = JSONArray()
        summary.packets.forEach { pkt ->
            val pObj = JSONObject()
            pObj.put("seq", pkt.seq)
            pObj.put("bytes", pkt.bytes)
            pObj.put("rtt_ms", pkt.rttMs)
            pObj.put("ttl", pkt.ttl)
            pObj.put("success", pkt.isSuccess)
            packetsArr.put(pObj)
        }
        pingObj.put("packets", packetsArr)
        root.put("ping_results", pingObj)

        return root.toString(2)
    }

    fun createTcpReportJson(
        netInfo: NetworkInterfaceInfo,
        summary: TcpSummary
    ): String {
        val root = JSONObject()
        root.put("report_type", "TCP_CONNECT")
        root.put("timestamp", System.currentTimeMillis())
        root.put("formatted_time", dateFormat.format(Date()))
        root.put("network_interface", networkInfoToJson(netInfo))

        val tcpObj = JSONObject()
        tcpObj.put("target_host", summary.host)
        tcpObj.put("target_port", summary.port)
        tcpObj.put("resolved_ip", summary.resolvedIp)
        tcpObj.put("status", summary.status)
        tcpObj.put("attempts", summary.attempts)
        tcpObj.put("successes", summary.successes)
        tcpObj.put("avg_handshake_ms", summary.avgHandshakeMs)
        tcpObj.put("min_handshake_ms", summary.minHandshakeMs)
        tcpObj.put("max_handshake_ms", summary.maxHandshakeMs)

        val probeArr = JSONArray()
        summary.results.forEach { r ->
            val pObj = JSONObject()
            pObj.put("attempt", r.attempt)
            pObj.put("handshake_ms", r.handshakeTimeMs)
            pObj.put("connected", r.isConnected)
            pObj.put("status_text", r.statusText)
            probeArr.put(pObj)
        }
        tcpObj.put("handshakes", probeArr)
        root.put("tcp_results", tcpObj)

        return root.toString(2)
    }

    fun createDnsReportJson(
        netInfo: NetworkInterfaceInfo,
        dnsReport: DnsTestReport
    ): String {
        val root = JSONObject()
        root.put("report_type", "DNS_BENCHMARK")
        root.put("timestamp", System.currentTimeMillis())
        root.put("formatted_time", dateFormat.format(Date()))
        root.put("network_interface", networkInfoToJson(netInfo))

        val dnsObj = JSONObject()
        dnsObj.put("query_domain", dnsReport.domain)
        dnsObj.put("local_dns_servers", JSONArray(dnsReport.localDnsServers))
        dnsObj.put("local_lookup_duration_ms", dnsReport.localLookupDurationMs)
        dnsObj.put("resolved_ips", JSONArray(dnsReport.resolvedIps))

        val benchmarksArr = JSONArray()
        dnsReport.benchmarks.forEach { b ->
            val bObj = JSONObject()
            bObj.put("server_name", b.serverName)
            bObj.put("server_ip", b.serverIp)
            bObj.put("is_local_dns", b.isLocalDns)
            bObj.put("duration_ms", b.durationMs)
            bObj.put("success", b.isSuccess)
            bObj.put("records", JSONArray(b.records))
            b.errorMessage?.let { bObj.put("error", it) }
            benchmarksArr.put(bObj)
        }
        dnsObj.put("benchmarks", benchmarksArr)
        root.put("dns_results", dnsObj)

        return root.toString(2)
    }

    fun createTracerouteReportJson(
        netInfo: NetworkInterfaceInfo,
        report: TracerouteReport
    ): String {
        val root = JSONObject()
        root.put("report_type", "TRACEROUTE")
        root.put("timestamp", System.currentTimeMillis())
        root.put("formatted_time", dateFormat.format(Date()))
        root.put("network_interface", networkInfoToJson(netInfo))

        val traceObj = JSONObject()
        traceObj.put("target_host", report.targetHost)
        traceObj.put("resolved_target_ip", report.resolvedTargetIp)
        traceObj.put("total_hops", report.totalHops)
        traceObj.put("target_reached", report.isTargetReached)

        val hopsArr = JSONArray()
        report.hops.forEach { h ->
            val hObj = JSONObject()
            hObj.put("hop", h.hopIndex)
            hObj.put("ip", h.ip)
            h.hostname?.let { hObj.put("hostname", it) }
            hObj.put("rtt_ms", h.rttMs)
            hObj.put("timeout", h.isTimeout)
            hObj.put("reached", h.isReached)
            hopsArr.put(hObj)
        }
        traceObj.put("hops", hopsArr)
        root.put("traceroute_results", traceObj)

        return root.toString(2)
    }

    fun createFullDiagnosticJson(
        netInfo: NetworkInterfaceInfo,
        targetHost: String,
        pingSummary: PingSummary?,
        tcpSummary: TcpSummary?,
        dnsReport: DnsTestReport?,
        traceReport: TracerouteReport?
    ): String {
        val root = JSONObject()
        root.put("report_type", "FULL_DIAGNOSTIC")
        root.put("timestamp", System.currentTimeMillis())
        root.put("formatted_time", dateFormat.format(Date()))
        root.put("target_host", targetHost)
        root.put("network_interface", networkInfoToJson(netInfo))

        pingSummary?.let { root.put("ping", JSONObject(createPingReportJson(netInfo, it)).getJSONObject("ping_results")) }
        tcpSummary?.let { root.put("tcp", JSONObject(createTcpReportJson(netInfo, it)).getJSONObject("tcp_results")) }
        dnsReport?.let { root.put("dns", JSONObject(createDnsReportJson(netInfo, it)).getJSONObject("dns_results")) }
        traceReport?.let { root.put("traceroute", JSONObject(createTracerouteReportJson(netInfo, it)).getJSONObject("traceroute_results")) }

        return root.toString(2)
    }

    fun formatReportToText(jsonString: String): String {
        val sb = StringBuilder()
        try {
            val json = JSONObject(jsonString)
            val type = json.optString("report_type", "DIAGNOSTIC")
            val time = json.optString("formatted_time", "")

            sb.appendLine("==========================================")
            sb.appendLine(" NETDIAG NETWORK DIAGNOSTIC REPORT")
            sb.appendLine(" Type: $type | Date: $time")
            sb.appendLine("==========================================")
            sb.appendLine()

            val net = json.optJSONObject("network_interface")
            if (net != null) {
                sb.appendLine("--- NETWORK INTERFACE ---")
                sb.appendLine("• Status:       ${if (net.optBoolean("connected")) "Connected" else "Disconnected"}")
                sb.appendLine("• Type:         ${net.optString("type")}")
                sb.appendLine("• Local IPv4:   ${net.optString("local_ipv4")}")
                sb.appendLine("• Local IPv6:   ${net.optString("local_ipv6")}")
                sb.appendLine("• Gateway:      ${net.optString("gateway")}")
                val dnsList = net.optJSONArray("local_dns")
                val dnsStr = (0 until (dnsList?.length() ?: 0)).joinToString(", ") { dnsList!!.getString(it) }
                sb.appendLine("• Local DNS:    $dnsStr")
                if (net.has("wifi_ssid") && !net.isNull("wifi_ssid")) {
                    sb.appendLine("• Wi-Fi SSID:   ${net.optString("wifi_ssid")} (${net.optInt("link_speed_mbps")} Mbps)")
                }
                sb.appendLine()
            }

            if (json.has("ping_results") || json.has("ping")) {
                val ping = json.optJSONObject("ping_results") ?: json.optJSONObject("ping")
                if (ping != null) {
                    sb.appendLine("--- ICMP PING METRICS ---")
                    sb.appendLine("• Target:       ${ping.optString("target_host")} (${ping.optString("resolved_ip")})")
                    sb.appendLine("• Loss:         ${ping.optDouble("packet_loss_pct")}% (${ping.optInt("received")}/${ping.optInt("transmitted")})")
                    sb.appendLine("• RTT Min/Avg:  ${ping.optDouble("min_rtt_ms")} ms / ${ping.optDouble("avg_rtt_ms")} ms")
                    sb.appendLine("• RTT Max/Mdev: ${ping.optDouble("max_rtt_ms")} ms / ${ping.optDouble("mdev_ms")} ms")
                    sb.appendLine()
                }
            }

            if (json.has("tcp_results") || json.has("tcp")) {
                val tcp = json.optJSONObject("tcp_results") ?: json.optJSONObject("tcp")
                if (tcp != null) {
                    sb.appendLine("--- TCP CONNECT METRICS ---")
                    sb.appendLine("• Endpoint:     ${tcp.optString("target_host")}:${tcp.optInt("target_port")}")
                    sb.appendLine("• Status:       ${tcp.optString("status")}")
                    sb.appendLine("• Handshake:    Avg ${tcp.optDouble("avg_handshake_ms")} ms (Min ${tcp.optDouble("min_handshake_ms")} / Max ${tcp.optDouble("max_handshake_ms")})")
                    sb.appendLine()
                }
            }

            if (json.has("dns_results") || json.has("dns")) {
                val dns = json.optJSONObject("dns_results") ?: json.optJSONObject("dns")
                if (dns != null) {
                    sb.appendLine("--- DNS RESOLUTION & BENCHMARK ---")
                    sb.appendLine("• Domain:       ${dns.optString("query_domain")}")
                    val ips = dns.optJSONArray("resolved_ips")
                    val ipStr = (0 until (ips?.length() ?: 0)).joinToString(", ") { ips!!.getString(it) }
                    sb.appendLine("• Resolved IPs: $ipStr")
                    sb.appendLine("• Local Lookup: ${dns.optDouble("local_lookup_duration_ms")} ms")
                    sb.appendLine("• Benchmarks:")
                    val bArr = dns.optJSONArray("benchmarks")
                    if (bArr != null) {
                        for (i in 0 until bArr.length()) {
                            val b = bArr.getJSONObject(i)
                            val star = if (b.optBoolean("is_local_dns")) "[LOCAL] " else ""
                            sb.appendLine("  - $star${b.optString("server_name")} (${b.optString("server_ip")}): ${b.optDouble("duration_ms")} ms")
                        }
                    }
                    sb.appendLine()
                }
            }

            if (json.has("traceroute_results") || json.has("traceroute")) {
                val tr = json.optJSONObject("traceroute_results") ?: json.optJSONObject("traceroute")
                if (tr != null) {
                    sb.appendLine("--- TRACEROUTE HOPS ---")
                    sb.appendLine("• Target:       ${tr.optString("target_host")} (${tr.optString("resolved_target_ip")})")
                    sb.appendLine("• Total Hops:   ${tr.optInt("total_hops")} (Reached: ${tr.optBoolean("target_reached")})")
                    val hops = tr.optJSONArray("hops")
                    if (hops != null) {
                        for (i in 0 until hops.length()) {
                            val h = hops.getJSONObject(i)
                            val hostStr = if (h.has("hostname")) " [${h.optString("hostname")}]" else ""
                            val rttStr = if (h.optBoolean("timeout")) "* * *" else "${h.optDouble("rtt_ms")} ms"
                            sb.appendLine(String.format(Locale.US, "  %2d:  %-18s %-10s%s", h.optInt("hop"), h.optString("ip"), rttStr, hostStr))
                        }
                    }
                    sb.appendLine()
                }
            }

            sb.appendLine("==========================================")
            sb.appendLine("Generated by NetDiag on Android")
        } catch (_: Exception) {
            return jsonString
        }
        return sb.toString()
    }

    private fun networkInfoToJson(netInfo: NetworkInterfaceInfo): JSONObject {
        val netObj = JSONObject()
        netObj.put("connected", netInfo.isConnected)
        netObj.put("type", netInfo.networkType)
        netObj.put("local_ipv4", netInfo.localIpv4)
        netObj.put("local_ipv6", netInfo.localIpv6)
        netObj.put("gateway", netInfo.gatewayIp)
        netObj.put("local_dns", JSONArray(netInfo.localDnsServers))
        netInfo.wifiSsid?.let { netObj.put("wifi_ssid", it) }
        netObj.put("link_speed_mbps", netInfo.linkSpeedMbps)
        netObj.put("mtu", netInfo.mtu)
        return netObj
    }
}
