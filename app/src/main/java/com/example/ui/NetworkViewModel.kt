package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ReportEntity
import com.example.data.ReportRepository
import com.example.network.DiagnosticsReporter
import com.example.network.DnsBenchmarkResult
import com.example.network.DnsEngine
import com.example.network.DnsTestReport
import com.example.network.NetworkInfoManager
import com.example.network.NetworkInterfaceInfo
import com.example.network.PingEngine
import com.example.network.PingPacket
import com.example.network.PingSummary
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
    CHINESE("zh", "简体中文")
}

data class DiagnosticUiState(
    val currentTab: DiagnosticTab = DiagnosticTab.OVERVIEW,
    val selectedLanguage: AppLanguage = AppLanguage.SYSTEM,
    val networkInfo: NetworkInterfaceInfo? = null,
    val isNetworkRefreshing: Boolean = false,

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
    val infoMessage: String? = null
)

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val netInfoManager = NetworkInfoManager(application)
    private val pingEngine = PingEngine()
    private val tcpEngine = TcpConnectEngine()
    private val dnsEngine = DnsEngine()
    private val tracerouteEngine = TracerouteEngine()

    private val db = AppDatabase.getDatabase(application)
    private val repository = ReportRepository(db.reportDao())

    val reportsList: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

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
        // uri format: netdiag://test?type=ping&host=...&port=...
        // or https://netdiag.app/test?type=...
        val type = uri.getQueryParameter("type")?.lowercase() ?: "ping"
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
                selectTab(DiagnosticTab.OVERVIEW)
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
}
