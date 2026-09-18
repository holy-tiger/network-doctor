package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ReportEntity
import com.example.data.ReportRepository
import com.example.network.DiagnosticsReporter
import com.example.network.DiagnosticReportUploader
import com.example.network.DiagnosticStatus
import com.example.network.DiagnosticSubResult
import com.example.network.DnsBenchmarkResult
import com.example.network.DnsEngine
import com.example.network.DnsTestReport
import com.example.network.NetworkInfoManager
import com.example.network.NetworkInterfaceInfo
import com.example.network.PingEngine
import com.example.network.PingPacket
import com.example.network.PingSummary
import com.example.network.ServerDiagnosticEngine
import com.example.network.TargetInspectionResult
import com.example.network.TcpConnectEngine
import com.example.network.TcpProbeResult
import com.example.network.TcpSummary
import com.example.network.TracerouteEngine
import com.example.network.TracerouteHop
import com.example.network.TracerouteReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DiagnosticTab {
    DNS_SERVER,
    OVERVIEW,
    PING,
    TCP,
    DNS,
    TRACEROUTE,
    HISTORY
}

enum class AppLanguage(val code: String, val label: String) {
    SYSTEM("", "Auto"),
    ENGLISH("en", "English"),
    CHINESE("zh", "简体中文"),
    ARABIC("ar", "العربية")
}

enum class InspectionFilter {
    ALL,
    FAILED_ONLY,
    HEALTHY_ONLY
}

fun createDefaultInspectionResults(): List<TargetInspectionResult> = listOf(
    TargetInspectionResult(
        id = "sample-baidu",
        rawInput = "baidu.com",
        host = "baidu.com",
        port = 443,
        isHttps = true,
        isConnected = true,
        totalDurationMs = 4861L,
        resolvedIp = "110.242.74.102",
        allResolvedIps = listOf("110.242.74.102", "110.242.74.101"),
        dnsResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 1711,
            summary = "IP: 110.242.74.102",
            details = mapOf("A/AAAA Records" to "110.242.74.102, 110.242.74.101")
        ),
        tcpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 241,
            summary = "端口: 443"
        ),
        tlsResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 730,
            summary = "有效期剩 167 天"
        ),
        certExpiryDays = 167L,
        certValidUntil = "2026-08-30",
        certIssuer = "CN=GlobalSign RSA OV SSL CA 2018",
        httpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 2177,
            summary = "HTTP 状态: 301 Moved Permanently"
        ),
        httpStatusLine = "301 Moved Permanently",
        httpHeaders = mapOf("Server" to "BWS", "Location" to "https://www.baidu.com/")
    ),
    TargetInspectionResult(
        id = "sample-github",
        rawInput = "github.com",
        host = "github.com",
        port = 443,
        isHttps = true,
        isConnected = true,
        totalDurationMs = 240L,
        resolvedIp = "20.205.243.166",
        allResolvedIps = listOf("20.205.243.166"),
        dnsResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 85,
            summary = "IP: 20.205.243.166",
            details = mapOf("A/AAAA Records" to "20.205.243.166")
        ),
        tcpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 180,
            summary = "端口: 443"
        ),
        tlsResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 215,
            summary = "有效期剩 290 天"
        ),
        certExpiryDays = 290L,
        certValidUntil = "2026-12-20",
        certIssuer = "CN=DigiCert Global G2 TLS RSA SHA256 2020 CA1",
        httpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 240,
            summary = "HTTP 状态: 200 OK"
        ),
        httpStatusLine = "200 OK",
        httpHeaders = mapOf("Server" to "GitHub.com", "Content-Type" to "text/html; charset=utf-8")
    ),
    TargetInspectionResult(
        id = "sample-invalid",
        rawInput = "invalid-domain-notfound-999.xyz",
        host = "invalid-domain-notfound-999.xyz",
        port = 443,
        isHttps = true,
        isConnected = false,
        totalDurationMs = 64L,
        resolvedIp = null,
        dnsResult = DiagnosticSubResult(
            status = DiagnosticStatus.ERROR,
            latencyMs = 64,
            summary = "解析失败 (NXDOMAIN)",
            errorMessage = "Unknown host (NXDOMAIN)"
        ),
        tcpResult = DiagnosticSubResult(
            status = DiagnosticStatus.ERROR,
            latencyMs = 0,
            summary = "跳过 (DNS失败)",
            errorMessage = "Skipped"
        ),
        tlsResult = DiagnosticSubResult(
            status = DiagnosticStatus.ERROR,
            latencyMs = 0,
            summary = "跳过 (TCP未通)",
            errorMessage = "Skipped"
        ),
        httpResult = DiagnosticSubResult(
            status = DiagnosticStatus.ERROR,
            latencyMs = 0,
            summary = "跳过 (TCP未通)",
            errorMessage = "Skipped"
        )
    ),
    TargetInspectionResult(
        id = "sample-expired",
        rawInput = "expired.badssl.com",
        host = "expired.badssl.com",
        port = 443,
        isHttps = true,
        isConnected = false,
        totalDurationMs = 79L,
        resolvedIp = "104.154.89.105",
        allResolvedIps = listOf("104.154.89.105"),
        dnsResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 45,
            summary = "IP: 104.154.89.105"
        ),
        tcpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 190,
            summary = "端口: 443"
        ),
        tlsResult = DiagnosticSubResult(
            status = DiagnosticStatus.ERROR,
            latencyMs = 350,
            summary = "证书已过期",
            errorMessage = "Certificate expired"
        ),
        certExpiryDays = -320L,
        certValidUntil = "2024-04-12",
        certIssuer = "CN=COMODO RSA Domain Validation Secure Server CA",
        httpResult = DiagnosticSubResult(
            status = DiagnosticStatus.SUCCESS,
            latencyMs = 320,
            summary = "HTTP 状态: 200 OK"
        ),
        httpStatusLine = "200 OK"
    )
)

data class DiagnosticUiState(
    val currentTab: DiagnosticTab = DiagnosticTab.DNS_SERVER,
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
    val networkInfo: NetworkInterfaceInfo? = null,
    val isNetworkRefreshing: Boolean = false,

    // DNS & Server Diagnostic Suite (Matching Reference Design)
    val targetInputText: String = "baidu.com\ngithub.com\ninvalid-domain-notfound-999.xyz\nexpired.badssl.com",
    val isInspectionRunning: Boolean = false,
    val inspectionResults: List<TargetInspectionResult> = createDefaultInspectionResults(),
    val inspectionSearchQuery: String = "",
    val inspectionFilter: InspectionFilter = InspectionFilter.ALL,
    val isAdvancedSettingsOpen: Boolean = false,
    val isMarkdownReportOpen: Boolean = false,

    // Ping State
    val pingHost: String = "1.1.1.1",
    val pingCount: Int = 4,
    val isPingRunning: Boolean = false,
    val pingPackets: List<PingPacket> = emptyList(),
    val pingSummary: PingSummary? = null,

    // TCP State
    val tcpHost: String = "google.com",
    val tcpPort: Int = 443,
    val isTcpRunning: Boolean = false,
    val tcpResults: List<TcpProbeResult> = emptyList(),
    val tcpSummary: TcpSummary? = null,

    // DNS State
    val dnsDomain: String = "google.com",
    val isDnsRunning: Boolean = false,
    val dnsReport: DnsTestReport? = null,

    // Traceroute State
    val traceHost: String = "8.8.8.8",
    val isTraceRunning: Boolean = false,
    val traceHops: List<TracerouteHop> = emptyList(),
    val traceReport: TracerouteReport? = null,

    // Full Diagnostic
    val isFullDiagRunning: Boolean = false,
    val fullDiagStep: String = "",
    val fullDiagProgress: Float = 0f,

    // Active Selected Report for Dialog Detail
    val activeDetailReport: ReportEntity? = null,
    val isDeepLinkDialogVisible: Boolean = false,
    val infoMessage: String? = null,

    // Auto Upload / Report Endpoint State
    val reportUrl: String = "",
    val isAutoReportEnabled: Boolean = true,
    val isReportUploading: Boolean = false,
    val reportUploadStatus: String? = null,
    val isReportSuccess: Boolean? = null
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val netInfoManager = NetworkInfoManager(application)
    private val pingEngine = PingEngine()
    private val tcpEngine = TcpConnectEngine()
    private val dnsEngine = DnsEngine()
    private val tracerouteEngine = TracerouteEngine()
    private val serverDiagnosticEngine = ServerDiagnosticEngine()

    private val db = AppDatabase.getDatabase(application)
    private val repository = ReportRepository(db.reportDao())

    val reportsList: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var inspectionJob: Job? = null

    init {
        refreshNetworkInfo()
    }

    fun selectTab(tab: DiagnosticTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(selectedLanguage = lang) }
    }

    fun setDeepLinkDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isDeepLinkDialogVisible = visible) }
    }

    fun setActiveDetailReport(report: ReportEntity?) {
        _uiState.update { it.copy(activeDetailReport = report) }
    }

    fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun refreshNetworkInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isNetworkRefreshing = true) }
            val info = withContext(Dispatchers.IO) {
                netInfoManager.getNetworkInfo()
            }
            _uiState.update { it.copy(networkInfo = info, isNetworkRefreshing = false) }
        }
    }

    // --- Ping Logic ---
    fun updatePingHost(host: String) {
        _uiState.update { it.copy(pingHost = host.trim()) }
    }

    fun updatePingCount(count: Int) {
        _uiState.update { it.copy(pingCount = count) }
    }

    fun startPing(hostToPing: String = _uiState.value.pingHost) {
        if (_uiState.value.isPingRunning) return
        activeJob?.cancel()

        val host = hostToPing.ifBlank { "1.1.1.1" }
        _uiState.update {
            it.copy(
                pingHost = host,
                isPingRunning = true,
                pingPackets = emptyList(),
                pingSummary = null
            )
        }

        activeJob = viewModelScope.launch {
            try {
                pingEngine.runPing(host, _uiState.value.pingCount).collect { (pkt, summary) ->
                    _uiState.update { state ->
                        val currentPackets = state.pingPackets.toMutableList()
                        currentPackets.add(pkt)
                        state.copy(
                            pingPackets = currentPackets,
                            pingSummary = summary
                        )
                    }

                    if (summary != null) {
                        savePingReport(summary)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isPingRunning = false) }
            }
        }
    }

    private suspend fun savePingReport(summary: PingSummary) {
        val net = _uiState.value.networkInfo ?: netInfoManager.getNetworkInfo()
        val json = DiagnosticsReporter.createPingReportJson(net, summary)
        val status = if (summary.lossPercentage == 0f) "SUCCESS" else if (summary.lossPercentage < 50f) "WARNING" else "FAILED"
        val report = ReportEntity(
            testType = "PING",
            targetHost = summary.host,
            status = status,
            summary = "${summary.avgRtt} ms | Loss ${summary.lossPercentage}%",
            detailsJson = json
        )
        repository.insert(report)
    }

    // --- TCP Logic ---
    fun updateTcpHost(host: String) {
        _uiState.update { it.copy(tcpHost = host.trim()) }
    }

    fun updateTcpPort(port: Int) {
        _uiState.update { it.copy(tcpPort = port) }
    }

    fun startTcpTest(
        hostToTest: String = _uiState.value.tcpHost,
        portToTest: Int = _uiState.value.tcpPort
    ) {
        if (_uiState.value.isTcpRunning) return
        activeJob?.cancel()

        val host = hostToTest.ifBlank { "google.com" }
        val port = if (portToTest > 0) portToTest else 443

        _uiState.update {
            it.copy(
                tcpHost = host,
                tcpPort = port,
                isTcpRunning = true,
                tcpResults = emptyList(),
                tcpSummary = null
            )
        }

        activeJob = viewModelScope.launch {
            try {
                tcpEngine.runTcpProbes(host, port).collect { (probe, summary) ->
                    _uiState.update { state ->
                        val list = state.tcpResults.toMutableList()
                        list.add(probe)
                        state.copy(
                            tcpResults = list,
                            tcpSummary = summary
                        )
                    }

                    if (summary != null) {
                        saveTcpReport(summary)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isTcpRunning = false) }
            }
        }
    }

    private suspend fun saveTcpReport(summary: TcpSummary) {
        val net = _uiState.value.networkInfo ?: netInfoManager.getNetworkInfo()
        val json = DiagnosticsReporter.createTcpReportJson(net, summary)
        val status = if (summary.successes == summary.attempts) "SUCCESS" else if (summary.successes > 0) "WARNING" else "FAILED"
        val report = ReportEntity(
            testType = "TCP",
            targetHost = summary.host,
            targetPort = summary.port,
            status = status,
            summary = "${summary.status} | Avg ${summary.avgHandshakeMs} ms (${summary.successes}/${summary.attempts})",
            detailsJson = json
        )
        repository.insert(report)
    }

    // --- DNS Logic ---
    fun updateDnsDomain(domain: String) {
        _uiState.update { it.copy(dnsDomain = domain.trim()) }
    }

    fun startDnsBenchmark(domainToQuery: String = _uiState.value.dnsDomain) {
        if (_uiState.value.isDnsRunning) return
        activeJob?.cancel()

        val domain = domainToQuery.ifBlank { "google.com" }
        _uiState.update {
            it.copy(
                dnsDomain = domain,
                isDnsRunning = true,
                dnsReport = null
            )
        }

        activeJob = viewModelScope.launch {
            try {
                val net = _uiState.value.networkInfo ?: netInfoManager.getNetworkInfo()
                val (localDuration, resolvedIps) = dnsEngine.resolveWithSystem(domain)
                val benchmarks = dnsEngine.benchmarkDnsServers(domain, net.localDnsServers)

                val report = DnsTestReport(
                    domain = domain,
                    localDnsServers = net.localDnsServers,
                    localLookupDurationMs = localDuration,
                    resolvedIps = resolvedIps,
                    benchmarks = benchmarks
                )

                _uiState.update { it.copy(dnsReport = report) }
                saveDnsReport(report)
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isDnsRunning = false) }
            }
        }
    }

    private suspend fun saveDnsReport(report: DnsTestReport) {
        val net = _uiState.value.networkInfo ?: netInfoManager.getNetworkInfo()
        val json = DiagnosticsReporter.createDnsReportJson(net, report)
        val status = if (report.resolvedIps.isNotEmpty()) "SUCCESS" else "FAILED"
        val entity = ReportEntity(
            testType = "DNS",
            targetHost = report.domain,
            status = status,
            summary = "Local ${report.localLookupDurationMs} ms | ${report.resolvedIps.size} IPs resolved",
            detailsJson = json
        )
        repository.insert(entity)
    }

    // --- Traceroute Logic ---
    fun updateTraceHost(host: String) {
        _uiState.update { it.copy(traceHost = host.trim()) }
    }

    fun startTraceroute(hostToTrace: String = _uiState.value.traceHost) {
        if (_uiState.value.isTraceRunning) return
        activeJob?.cancel()

        val host = hostToTrace.ifBlank { "8.8.8.8" }
        _uiState.update {
            it.copy(
                traceHost = host,
                isTraceRunning = true,
                traceHops = emptyList(),
                traceReport = null
            )
        }

        activeJob = viewModelScope.launch {
            try {
                tracerouteEngine.runTraceroute(host).collect { (hop, report) ->
                    _uiState.update { state ->
                        val hops = state.traceHops.toMutableList()
                        hops.add(hop)
                        state.copy(
                            traceHops = hops,
                            traceReport = report
                        )
                    }

                    if (report != null) {
                        saveTraceReport(report)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isTraceRunning = false) }
            }
        }
    }

    private suspend fun saveTraceReport(report: TracerouteReport) {
        val net = _uiState.value.networkInfo ?: netInfoManager.getNetworkInfo()
        val json = DiagnosticsReporter.createTracerouteReportJson(net, report)
        val status = if (report.isTargetReached) "SUCCESS" else "WARNING"
        val entity = ReportEntity(
            testType = "TRACEROUTE",
            targetHost = report.targetHost,
            status = status,
            summary = "${report.totalHops} hops | ${if (report.isTargetReached) "Destination Reached" else "Partial"}",
            detailsJson = json
        )
        repository.insert(entity)
    }

    // --- One-Key Full Diagnostic ---
    fun runFullDiagnostic(targetHost: String = "google.com") {
        if (_uiState.value.isFullDiagRunning) return
        activeJob?.cancel()

        val host = targetHost.ifBlank { "google.com" }
        _uiState.update {
            it.copy(
                isFullDiagRunning = true,
                fullDiagProgress = 0.05f,
                fullDiagStep = "Extracting Local DNS & Network Info...",
                currentTab = DiagnosticTab.OVERVIEW
            )
        }

        activeJob = viewModelScope.launch {
            try {
                // Step 1: Network & Local DNS
                val net = withContext(Dispatchers.IO) { netInfoManager.getNetworkInfo() }
                _uiState.update {
                    it.copy(
                        networkInfo = net,
                        fullDiagProgress = 0.25f,
                        fullDiagStep = "Running ICMP Ping to $host..."
                    )
                }

                // Step 2: Ping
                var pingSummary: PingSummary? = null
                pingEngine.runPing(host, count = 4).collect { (_, summary) ->
                    if (summary != null) pingSummary = summary
                }

                _uiState.update {
                    it.copy(
                        fullDiagProgress = 0.50f,
                        fullDiagStep = "Testing TCP Port 443 Handshake..."
                    )
                }

                // Step 3: TCP
                var tcpSummary: TcpSummary? = null
                tcpEngine.runTcpProbes(host, port = 443, attempts = 3).collect { (_, summary) ->
                    if (summary != null) tcpSummary = summary
                }

                _uiState.update {
                    it.copy(
                        fullDiagProgress = 0.75f,
                        fullDiagStep = "Benchmarking Local & Public DNS..."
                    )
                }

                // Step 4: DNS
                val (dnsDuration, resolvedIps) = dnsEngine.resolveWithSystem(host)
                val benchmarks = dnsEngine.benchmarkDnsServers(host, net.localDnsServers)
                val dnsReport = DnsTestReport(
                    domain = host,
                    localDnsServers = net.localDnsServers,
                    localLookupDurationMs = dnsDuration,
                    resolvedIps = resolvedIps,
                    benchmarks = benchmarks
                )

                _uiState.update {
                    it.copy(
                        fullDiagProgress = 0.90f,
                        fullDiagStep = "Tracing initial hops to $host..."
                    )
                }

                // Step 5: Quick Traceroute (max 10 hops for speed in full diag)
                var traceReport: TracerouteReport? = null
                tracerouteEngine.runTraceroute(host, maxHops = 10, timeoutSec = 1).collect { (_, report) ->
                    if (report != null) traceReport = report
                }

                _uiState.update {
                    it.copy(
                        fullDiagProgress = 1.0f,
                        fullDiagStep = "Diagnostic Complete! Report saved."
                    )
                }

                // Save consolidated report
                val json = DiagnosticsReporter.createFullDiagnosticJson(
                    netInfo = net,
                    targetHost = host,
                    pingSummary = pingSummary,
                    tcpSummary = tcpSummary,
                    dnsReport = dnsReport,
                    traceReport = traceReport
                )

                val reportEntity = ReportEntity(
                    testType = "FULL_DIAG",
                    targetHost = host,
                    status = "SUCCESS",
                    summary = "Ping: ${pingSummary?.avgRtt ?: 0f}ms | TCP: ${tcpSummary?.avgHandshakeMs ?: 0f}ms | DNS: ${dnsReport.localLookupDurationMs}ms",
                    detailsJson = json
                )
                repository.insert(reportEntity)
                _uiState.update { it.copy(activeDetailReport = reportEntity) }

            } catch (_: Exception) {
            } finally {
                _uiState.update {
                    it.copy(isFullDiagRunning = false)
                }
            }
        }
    }

    fun stopCurrentTest() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(
                isPingRunning = false,
                isTcpRunning = false,
                isDnsRunning = false,
                isTraceRunning = false,
                isFullDiagRunning = false,
                fullDiagStep = "Stopped"
            )
        }
    }

    // --- Deep Link Handler ---
    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        val scheme = uri.scheme?.lowercase() ?: return
        if (scheme != "netdiag" && scheme != "http" && scheme != "https") return

        val uriHost = uri.host?.lowercase() ?: ""
        val type = uri.getQueryParameter("type")?.lowercase()
        val dataParam = uri.getQueryParameter("data")
            ?: uri.getQueryParameter("targets")
            ?: uri.getQueryParameter("domains")
            ?: uri.getQueryParameter("host")
            ?: uri.getQueryParameter("domain")

        // 1. Check if this is a DNS / Server inspection deep link
        // format: netdiag://inspect?data=<url-safe-base64>&report_url=<url>
        // or netdiag://test?type=server&data=...&report_url=...
        val reportUrlParam = uri.getQueryParameter("report_url")
            ?: uri.getQueryParameter("callback_url")
            ?: uri.getQueryParameter("reportUrl")
            ?: uri.getQueryParameter("callback")

        if (uriHost == "inspect" || uriHost == "dns_server" || uriHost == "server" ||
            type == "server" || type == "inspect" || (scheme == "netdiag" && uri.getQueryParameter("data") != null)
        ) {
            val targetsText = if (!dataParam.isNullOrBlank()) {
                try {
                    val bytes = android.util.Base64.decode(dataParam, android.util.Base64.URL_SAFE)
                    val decoded = String(bytes, Charsets.UTF_8)
                    if (decoded.isNotBlank()) decoded else Uri.decode(dataParam)
                } catch (e: Exception) {
                    Uri.decode(dataParam)
                }
            } else {
                _uiState.value.targetInputText
            }
            selectTab(DiagnosticTab.DNS_SERVER)
            updateTargetInputText(targetsText)
            if (!reportUrlParam.isNullOrBlank()) {
                val decodedReportUrl = try {
                    val urlBytes = android.util.Base64.decode(reportUrlParam.trim(), android.util.Base64.URL_SAFE)
                    val decoded = String(urlBytes, Charsets.UTF_8).trim()
                    if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                        decoded
                    } else {
                        Uri.decode(reportUrlParam.trim())
                    }
                } catch (e: Exception) {
                    Uri.decode(reportUrlParam.trim())
                }
                updateReportUrl(decodedReportUrl)
                setAutoReportEnabled(true)
            }
            startServerInspection()
            return
        }

        val host = uri.getQueryParameter("host") ?: uri.getQueryParameter("domain") ?: ""
        val portStr = uri.getQueryParameter("port")
        val port = portStr?.toIntOrNull() ?: 443

        when (type) {
            "ping" -> {
                selectTab(DiagnosticTab.PING)
                if (host.isNotBlank()) {
                    updatePingHost(host)
                    startPing(host)
                }
            }
            "tcp" -> {
                selectTab(DiagnosticTab.TCP)
                if (host.isNotBlank()) {
                    updateTcpHost(host)
                    updateTcpPort(port)
                    startTcpTest(host, port)
                }
            }
            "dns" -> {
                selectTab(DiagnosticTab.DNS)
                if (host.isNotBlank()) {
                    updateDnsDomain(host)
                    startDnsBenchmark(host)
                }
            }
            "trace", "traceroute" -> {
                selectTab(DiagnosticTab.TRACEROUTE)
                if (host.isNotBlank()) {
                    updateTraceHost(host)
                    startTraceroute(host)
                }
            }
            "full", "fulldiag", "all" -> {
                selectTab(DiagnosticTab.OVERVIEW)
                runFullDiagnostic(if (host.isNotBlank()) host else "google.com")
            }
            else -> {
                if (uriHost.contains("inspect") || uri.path?.contains("inspect") == true) {
                    selectTab(DiagnosticTab.DNS_SERVER)
                    startServerInspection()
                } else {
                    selectTab(DiagnosticTab.OVERVIEW)
                }
            }
        }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- DNS & Server Inspection Suite ---

    fun updateTargetInputText(text: String) {
        _uiState.update { it.copy(targetInputText = text) }
    }

    fun clearTargetInput() {
        _uiState.update { it.copy(targetInputText = "") }
    }

    fun loadPreset(isAnomalyGroup: Boolean) {
        val preset = if (isAnomalyGroup) {
            "invalid-domain-notfound-999.xyz\nexpired.badssl.com\n10.255.255.1"
        } else {
            "baidu.com\ngithub.com\ngoogle.com\ncloudflare.com"
        }
        _uiState.update { it.copy(targetInputText = preset) }
    }

    fun startServerInspection() {
        val currentText = _uiState.value.targetInputText
        val targets = currentText.lines()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (targets.isEmpty()) return

        inspectionJob?.cancel()

        val initialPlaceholders = targets.map { t ->
            TargetInspectionResult(
                rawInput = t,
                host = t.replace(Regex("^(https?://)"), "").substringBefore('/').substringBefore(':'),
                dnsResult = DiagnosticSubResult(DiagnosticStatus.PENDING)
            )
        }

        _uiState.update {
            it.copy(
                isInspectionRunning = true,
                inspectionResults = initialPlaceholders
            )
        }

        inspectionJob = viewModelScope.launch {
            targets.forEach { target ->
                val result = serverDiagnosticEngine.inspectTarget(target)
                _uiState.update { state ->
                    val updated = state.inspectionResults.map { item ->
                        if (item.rawInput == target) result else item
                    }
                    state.copy(inspectionResults = updated)
                }
            }
            _uiState.update { it.copy(isInspectionRunning = false) }

            // Auto-report if enabled and reportUrl is configured
            val stateNow = _uiState.value
            if (stateNow.isAutoReportEnabled && stateNow.reportUrl.isNotBlank()) {
                uploadInspectionReport(stateNow.reportUrl.trim())
            }
        }
    }

    fun uploadInspectionReport(targetUrl: String? = null) {
        val urlToUse = (targetUrl ?: _uiState.value.reportUrl).trim()
        if (urlToUse.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReportUploading = true,
                    reportUploadStatus = "正在上报检测结果...",
                    isReportSuccess = null
                )
            }

            val payloadJson = DiagnosticReportUploader.buildReportPayloadJson(
                context = getApplication(),
                netInfo = _uiState.value.networkInfo,
                results = _uiState.value.inspectionResults
            )

            val uploadResult = DiagnosticReportUploader.uploadReport(urlToUse, payloadJson)
            when (uploadResult) {
                is DiagnosticReportUploader.UploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isReportUploading = false,
                            reportUploadStatus = "上报成功 (HTTP ${uploadResult.statusCode})",
                            isReportSuccess = true
                        )
                    }
                }
                is DiagnosticReportUploader.UploadResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isReportUploading = false,
                            reportUploadStatus = "上报失败: ${uploadResult.errorMessage}",
                            isReportSuccess = false
                        )
                    }
                }
            }
        }
    }

    fun updateReportUrl(url: String) {
        _uiState.update { it.copy(reportUrl = url) }
    }

    fun setAutoReportEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAutoReportEnabled = enabled) }
    }

    fun dismissReportStatus() {
        _uiState.update { it.copy(reportUploadStatus = null, isReportSuccess = null) }
    }

    fun stopServerInspection() {
        inspectionJob?.cancel()
        _uiState.update { it.copy(isInspectionRunning = false) }
    }

    fun retestSingleTarget(target: String) {
        viewModelScope.launch {
            val result = serverDiagnosticEngine.inspectTarget(target)
            _uiState.update { state ->
                val updated = state.inspectionResults.map { item ->
                    if (item.rawInput == target || item.host == target) result else item
                }
                state.copy(inspectionResults = updated)
            }
        }
    }

    fun toggleExpandTarget(id: String) {
        _uiState.update { state ->
            val updated = state.inspectionResults.map {
                if (it.id == id) it.copy(isExpanded = !it.isExpanded) else it
            }
            state.copy(inspectionResults = updated)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(inspectionSearchQuery = query) }
    }

    fun updateFilter(filter: InspectionFilter) {
        _uiState.update { it.copy(inspectionFilter = filter) }
    }

    fun setAdvancedSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isAdvancedSettingsOpen = open) }
    }

    fun setMarkdownReportOpen(open: Boolean) {
        _uiState.update { it.copy(isMarkdownReportOpen = open) }
    }

    fun generateTestLink(customReportUrl: String? = null): String {
        val rawText = _uiState.value.targetInputText.trim()
        val base64Data = android.util.Base64.encodeToString(
            rawText.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        val reportUrlToUse = (customReportUrl ?: _uiState.value.reportUrl).trim()
        return if (reportUrlToUse.isNotBlank()) {
            val base64ReportUrl = android.util.Base64.encodeToString(
                reportUrlToUse.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
            "netdiag://inspect?data=$base64Data&report_url=$base64ReportUrl"
        } else {
            "netdiag://inspect?data=$base64Data"
        }
    }

    fun generateTestDeepLink(customReportUrl: String? = null): String = generateTestLink(customReportUrl)

    fun generateBriefSummary(): String {
        val results = _uiState.value.inspectionResults
        val total = results.size
        val healthy = results.count { it.isConnected }
        val failed = total - healthy
        val avgLatency = if (results.isNotEmpty()) results.map { it.totalDurationMs }.average().toInt() else 0

        val sb = StringBuilder()
        sb.appendLine("=== DNS & 服务器诊断简报 ===")
        sb.appendLine("总检测地址: $total | 正常: $healthy | 异常: $failed | 平均耗时: ${avgLatency}ms")
        sb.appendLine("---------------------------")
        results.forEach { item ->
            val statusStr = if (item.isConnected) "[正常]" else "[异常]"
            sb.appendLine("$statusStr ${item.host}:${item.port} - ${item.totalDurationMs}ms")
            sb.appendLine("  DNS: ${item.dnsResult.summary} (${item.dnsResult.latencyMs}ms)")
            sb.appendLine("  TCP: ${item.tcpResult.summary} (${item.tcpResult.latencyMs}ms)")
            sb.appendLine("  TLS: ${item.tlsResult.summary} (${item.tlsResult.latencyMs}ms)")
            sb.appendLine("  HTTP: ${item.httpResult.summary} (${item.httpResult.latencyMs}ms)")
        }
        return sb.toString()
    }

    fun generateExportJson(): String {
        val results = _uiState.value.inspectionResults
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"timestamp\": ${System.currentTimeMillis()},")
        sb.appendLine("  \"total\": ${results.size},")
        sb.appendLine("  \"healthy\": ${results.count { it.isConnected }},")
        sb.appendLine("  \"failed\": ${results.count { !it.isConnected }},")
        sb.appendLine("  \"results\": [")
        results.forEachIndexed { index, item ->
            sb.appendLine("    {")
            sb.appendLine("      \"host\": \"${item.host}\",")
            sb.appendLine("      \"port\": ${item.port},")
            sb.appendLine("      \"connected\": ${item.isConnected},")
            sb.appendLine("      \"totalDurationMs\": ${item.totalDurationMs},")
            sb.appendLine("      \"resolvedIp\": \"${item.resolvedIp ?: ""}\",")
            sb.appendLine("      \"dns\": {\"status\": \"${item.dnsResult.status}\", \"latencyMs\": ${item.dnsResult.latencyMs}, \"summary\": \"${item.dnsResult.summary}\"},")
            sb.appendLine("      \"tcp\": {\"status\": \"${item.tcpResult.status}\", \"latencyMs\": ${item.tcpResult.latencyMs}, \"summary\": \"${item.tcpResult.summary}\"},")
            sb.appendLine("      \"tls\": {\"status\": \"${item.tlsResult.status}\", \"latencyMs\": ${item.tlsResult.latencyMs}, \"summary\": \"${item.tlsResult.summary}\"},")
            sb.appendLine("      \"http\": {\"status\": \"${item.httpResult.status}\", \"latencyMs\": ${item.httpResult.latencyMs}, \"summary\": \"${item.httpResult.summary}\"}")
            sb.append("    }")
            if (index < results.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    fun generateMarkdownReport(): String {
        val results = _uiState.value.inspectionResults
        val total = results.size
        val healthy = results.count { it.isConnected }
        val failed = total - healthy
        val avgLatency = if (results.isNotEmpty()) results.map { it.totalDurationMs }.average().toInt() else 0

        val sb = StringBuilder()
        sb.appendLine("# DNS & 服务器检测报告")
        sb.appendLine("")
        sb.appendLine("> 生成时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine("")
        sb.appendLine("## 核心统计")
        sb.appendLine("- **总检测目标**: $total")
        sb.appendLine("- **正常连通**: $healthy")
        sb.appendLine("- **异常/失败**: $failed")
        sb.appendLine("- **平均耗时**: ${avgLatency}ms")
        sb.appendLine("")
        sb.appendLine("## 详细诊断明细")
        sb.appendLine("| 域名 / 目标 | 状态 | 总耗时 | DNS 解析 | TCP 握手 | TLS 证书 | HTTP 响应 |")
        sb.appendLine("| :--- | :---: | :---: | :---: | :---: | :---: | :---: |")
        results.forEach { item ->
            val statusBadge = if (item.isConnected) "🟢 正常" else "🔴 异常"
            sb.appendLine("| `${item.host}:${item.port}` | $statusBadge | ${item.totalDurationMs}ms | ${item.dnsResult.summary} | ${item.tcpResult.summary} | ${item.tlsResult.summary} | ${item.httpResult.summary} |")
        }
        sb.appendLine("")
        sb.appendLine("---")
        sb.appendLine("*Generated by DNS & Server Check Diagnostic Suite*")
        return sb.toString()
    }

    fun saveCurrentInspectionToHistory() {
        val results = _uiState.value.inspectionResults
        if (results.isEmpty()) return
        val healthy = results.count { it.isConnected }
        val failed = results.size - healthy
        val report = ReportEntity(
            testType = "DNS_SERVER",
            targetHost = "${results.size} targets ($healthy healthy, $failed abnormal)",
            targetPort = 443,
            status = if (failed == 0) "SUCCESS" else if (healthy > 0) "WARNING" else "FAILED",
            summary = "Checked ${results.size} targets: $healthy healthy, $failed failed",
            detailsJson = generateExportJson()
        )
        viewModelScope.launch {
            repository.insert(report)
        }
    }
}
