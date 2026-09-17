package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ReportEntity
import com.example.network.DiagnosticsReporter
import com.example.network.DnsBenchmarkResult
import com.example.network.NetworkInterfaceInfo
import com.example.network.PingPacket
import com.example.network.PingSummary
import com.example.network.TcpProbeResult
import com.example.network.TcpSummary
import com.example.network.TracerouteHop
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MintNeon
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TechNavyBorder
import com.example.ui.theme.TechNavyCard
import com.example.ui.theme.TechNavySurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NetworkViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reports by viewModel.reportsList.collectAsStateWithLifecycle()

    var showLangMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.networkInfo?.isConnected == true) MintNeon else StatusError
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = uiState.networkInfo?.let {
                                    "${it.networkType} • ${it.localIpv4}"
                                } ?: stringResource(R.string.status_ready),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon
                            )
                        }
                    }
                },
                actions = {
                    // DeepLink Info Button
                    IconButton(
                        onClick = { viewModel.setDeepLinkDialogVisible(true) },
                        modifier = Modifier.testTag("btn_deeplink_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(R.string.btn_deeplink_info),
                            tint = CyanNeon
                        )
                    }

                    // Refresh Button
                    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "spin"
                    )

                    IconButton(
                        onClick = { viewModel.refreshNetworkInfo() },
                        modifier = Modifier.testTag("btn_refresh_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.net_refresh),
                            tint = TextPrimary,
                            modifier = if (uiState.isNetworkRefreshing) Modifier.rotate(rotation) else Modifier
                        )
                    }

                    // Language Selector
                    Box {
                        IconButton(onClick = { showLangMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.language_switch),
                                tint = TextSecondary
                            )
                        }
                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lang_english)) },
                                onClick = {
                                    viewModel.setLanguage(AppLanguage.ENGLISH)
                                    showLangMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lang_chinese)) },
                                onClick = {
                                    viewModel.setLanguage(AppLanguage.CHINESE)
                                    showLangMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechNavySurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Diagnostic Tool Tabs / Chips Row
            TabNavigationRow(
                selectedTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Content Area based on Selected Tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState.currentTab) {
                    DiagnosticTab.OVERVIEW -> OverviewTab(viewModel, uiState)
                    DiagnosticTab.PING -> PingTab(viewModel, uiState)
                    DiagnosticTab.TCP -> TcpTab(viewModel, uiState)
                    DiagnosticTab.DNS -> DnsTab(viewModel, uiState)
                    DiagnosticTab.TRACEROUTE -> TracerouteTab(viewModel, uiState)
                    DiagnosticTab.HISTORY -> HistoryTab(viewModel, reports)
                }
            }
        }
    }

    // Detail Report Dialog
    uiState.activeDetailReport?.let { report ->
        ReportDetailDialog(
            report = report,
            onDismiss = { viewModel.setActiveDetailReport(null) }
        )
    }

    // Deep Link Help Dialog
    if (uiState.isDeepLinkDialogVisible) {
        DeepLinkDialog(
            onDismiss = { viewModel.setDeepLinkDialogVisible(false) },
            onLaunchSample = { uriString ->
                viewModel.handleDeepLink(android.net.Uri.parse(uriString))
                viewModel.setDeepLinkDialogVisible(false)
            }
        )
    }
}

@Composable
fun TabNavigationRow(
    selectedTab: DiagnosticTab,
    onTabSelected: (DiagnosticTab) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TechNavySurface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DiagnosticTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = when (tab) {
                DiagnosticTab.OVERVIEW -> stringResource(R.string.tab_overview)
                DiagnosticTab.PING -> stringResource(R.string.tab_ping)
                DiagnosticTab.TCP -> stringResource(R.string.tab_tcp)
                DiagnosticTab.DNS -> stringResource(R.string.tab_dns)
                DiagnosticTab.TRACEROUTE -> stringResource(R.string.tab_trace)
                DiagnosticTab.HISTORY -> stringResource(R.string.tab_history)
            }
            val icon = when (tab) {
                DiagnosticTab.OVERVIEW -> Icons.Default.Speed
                DiagnosticTab.PING -> Icons.Default.SwapVert
                DiagnosticTab.TCP -> Icons.Default.NetworkCheck
                DiagnosticTab.DNS -> Icons.Default.Dns
                DiagnosticTab.TRACEROUTE -> Icons.Default.Route
                DiagnosticTab.HISTORY -> Icons.Default.History
            }

            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) CyanNeon else TextSecondary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    selectedLabelColor = CyanNeon,
                    containerColor = TechNavyCard,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = TechNavyBorder,
                    selectedBorderColor = CyanNeon
                ),
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}

// -------------------------------------------------------------
// OVERVIEW TAB
// -------------------------------------------------------------
@Composable
fun OverviewTab(viewModel: NetworkViewModel, uiState: DiagnosticUiState) {
    val scrollState = rememberScrollState()
    var fullTargetHost by remember { mutableStateOf("google.com") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero One-Key Diagnostic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.btn_run_full_diag),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullTargetHost,
                    onValueChange = { fullTargetHost = it },
                    label = { Text(stringResource(R.string.target_host)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_full_diag_host"),
                    colors = outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isFullDiagRunning) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.fullDiagStep,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(uiState.fullDiagProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { uiState.fullDiagProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyanNeon,
                            trackColor = TechNavyBorder
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.stopCurrentTest() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_stop))
                    }
                } else {
                    Button(
                        onClick = { viewModel.runFullDiagnostic(fullTargetHost) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_full_diag"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = Color(0xFF00373D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_run_full_diag),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Network Interface & Local DNS Card
        val net = uiState.networkInfo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.net_info_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (net?.isConnected == true) stringResource(R.string.net_status_connected) else stringResource(R.string.net_status_disconnected),
                        color = if (net?.isConnected == true) MintNeon else StatusError,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = stringResource(R.string.net_type), value = net?.networkType ?: "—")
                InfoRow(label = stringResource(R.string.net_local_ip), value = net?.localIpv4 ?: "—", isMonospace = true)
                InfoRow(label = stringResource(R.string.net_local_ipv6), value = net?.localIpv6 ?: "—", isMonospace = true)
                InfoRow(label = stringResource(R.string.net_gateway), value = net?.gatewayIp ?: "—", isMonospace = true)
                if (net?.wifiSsid != null) {
                    InfoRow(label = stringResource(R.string.net_wifi_ssid), value = "${net.wifiSsid} (${net.linkSpeedMbps} Mbps)")
                }
                InfoRow(label = "MTU", value = "${net?.mtu ?: 1500} bytes")

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.net_local_dns),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanNeon,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Render Local DNS servers as badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dnsList = net?.localDnsServers ?: emptyList()
                    if (dnsList.isEmpty()) {
                        Text(
                            text = "Detecting...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        dnsList.forEach { dnsIp ->
                            Surface(
                                color = TechNavySurface,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dnsIp,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Preset Tools Grid Cards
        Text(
            text = "Diagnostic Modules",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleCard(
                title = stringResource(R.string.tab_ping),
                desc = "ICMP & Latency",
                icon = Icons.Default.SwapVert,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(DiagnosticTab.PING) }
            )
            ModuleCard(
                title = stringResource(R.string.tab_tcp),
                desc = "Port Handshake",
                icon = Icons.Default.NetworkCheck,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(DiagnosticTab.TCP) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModuleCard(
                title = stringResource(R.string.tab_dns),
                desc = "Resolve & Benchmark",
                icon = Icons.Default.Dns,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(DiagnosticTab.DNS) }
            )
            ModuleCard(
                title = stringResource(R.string.tab_trace),
                desc = "Hop Route Trace",
                icon = Icons.Default.Route,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(DiagnosticTab.TRACEROUTE) }
            )
        }
    }
}

@Composable
fun ModuleCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TechNavyCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TechNavySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// -------------------------------------------------------------
// PING TAB
// -------------------------------------------------------------
@Composable
fun PingTab(viewModel: NetworkViewModel, uiState: DiagnosticUiState) {
    val context = LocalContext.current
    val packets = uiState.pingPackets
    val summary = uiState.pingSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controls Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = uiState.pingHost,
                    onValueChange = { viewModel.updatePingHost(it) },
                    label = { Text(stringResource(R.string.target_host)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_ping_host"),
                    colors = outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ping_packet_count) + ":",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        listOf(4, 10, 20).forEach { count ->
                            val selected = uiState.pingCount == count
                            Surface(
                                color = if (selected) CyanNeon else TechNavySurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { viewModel.updatePingCount(count) }
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.Black else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (uiState.isPingRunning) {
                        Button(
                            onClick = { viewModel.stopCurrentTest() },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_stop_ping")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_stop))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startPing() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00373D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_ping")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_start))
                        }
                    }
                }
            }
        }

        // Summary Stats Card (if available)
        if (summary != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TechNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ping_stats_summary),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyanNeon
                        )
                        Text(
                            text = "${stringResource(R.string.ping_loss)}: ${summary.lossPercentage}%",
                            color = if (summary.lossPercentage == 0f) MintNeon else StatusError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatPill("Min", "${summary.minRtt} ms")
                        StatPill("Avg", "${summary.avgRtt} ms", isHighlight = true)
                        StatPill("Max", "${summary.maxRtt} ms")
                        StatPill("Jitter", "${summary.mdevRtt} ms")
                    }
                }
            }
        }

        // Packets List
        Text(
            text = "Packets Output (${packets.size})",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(packets) { pkt ->
                Surface(
                    color = TechNavyCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, TechNavyBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${pkt.seq}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CyanNeon
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = pkt.ip,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ttl=${pkt.ttl}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        val rttColor = when {
                            !pkt.isSuccess || pkt.rttMs < 0 -> StatusError
                            pkt.rttMs < 50f -> MintNeon
                            pkt.rttMs < 150f -> StatusWarning
                            else -> StatusError
                        }

                        Text(
                            text = if (pkt.isSuccess && pkt.rttMs >= 0) "${pkt.rttMs} ms" else "Timeout",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = rttColor
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TCP CONNECT TAB
// -------------------------------------------------------------
@Composable
fun TcpTab(viewModel: NetworkViewModel, uiState: DiagnosticUiState) {
    val results = uiState.tcpResults
    val summary = uiState.tcpSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.tcpHost,
                        onValueChange = { viewModel.updateTcpHost(it) },
                        label = { Text(stringResource(R.string.target_host)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("input_tcp_host"),
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.tcpPort.toString(),
                        onValueChange = { viewModel.updateTcpPort(it.toIntOrNull() ?: 0) },
                        label = { Text(stringResource(R.string.target_port)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_tcp_port"),
                        colors = outlinedTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Port Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(443 to "HTTPS", 80 to "HTTP", 53 to "DNS", 22 to "SSH", 8080 to "Web")
                    presets.forEach { (port, name) ->
                        val isSelected = uiState.tcpPort == port
                        Surface(
                            color = if (isSelected) CyanNeon else TechNavySurface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { viewModel.updateTcpPort(port) }
                        ) {
                            Text(
                                text = "$name ($port)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isTcpRunning) {
                    Button(
                        onClick = { viewModel.stopCurrentTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_stop_tcp")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_stop))
                    }
                } else {
                    Button(
                        onClick = { viewModel.startTcpTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00373D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_tcp")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_start))
                    }
                }
            }
        }

        // Summary Card
        if (summary != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TechNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.host}:${summary.port} (${summary.resolvedIp})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = summary.status,
                            color = if (summary.successes > 0) MintNeon else StatusError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatPill("Avg Handshake", "${summary.avgHandshakeMs} ms", isHighlight = true)
                        StatPill("Min", "${summary.minHandshakeMs} ms")
                        StatPill("Max", "${summary.maxHandshakeMs} ms")
                        StatPill("Success", "${summary.successes}/${summary.attempts}")
                    }
                }
            }
        }

        // Attempts List
        Text(
            text = "Handshake Probes (${results.size})",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results) { probe ->
                Surface(
                    color = TechNavyCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, TechNavyBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (probe.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (probe.isConnected) MintNeon else StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Probe #${probe.attempt}: ${probe.statusText}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (probe.isConnected) "${probe.handshakeTimeMs} ms" else "Failed",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (probe.isConnected) CyanNeon else StatusError
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DNS BENCHMARK TAB
// -------------------------------------------------------------
@Composable
fun DnsTab(viewModel: NetworkViewModel, uiState: DiagnosticUiState) {
    val report = uiState.dnsReport
    val net = uiState.networkInfo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Domain input card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = uiState.dnsDomain,
                    onValueChange = { viewModel.updateDnsDomain(it) },
                    label = { Text(stringResource(R.string.dns_query_domain)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dns_domain"),
                    colors = outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Local DNS notice
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Device Local DNS: ${(net?.localDnsServers ?: listOf("Detecting...")).joinToString(", ")}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isDnsRunning) {
                    Button(
                        onClick = { viewModel.stopCurrentTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_stop_dns")
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_stop))
                    }
                } else {
                    Button(
                        onClick = { viewModel.startDnsBenchmark() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00373D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_dns")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_start))
                    }
                }
            }
        }

        // Benchmark Results
        Text(
            text = stringResource(R.string.dns_benchmark_header),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        if (report != null) {
            // Local DNS duration summary banner
            Surface(
                color = TechNavySurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Resolved ${report.domain} in ${report.localLookupDurationMs} ms (System)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyanNeon
                        )
                        Text(
                            text = "${report.resolvedIps.size} IPs",
                            fontSize = 12.sp,
                            color = MintNeon
                        )
                    }
                    if (report.resolvedIps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.resolvedIps.joinToString(" • "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(report.benchmarks) { bench ->
                    DnsBenchmarkItem(bench)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Press 'Start Test' to benchmark Local DNS vs Public DNS",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DnsBenchmarkItem(bench: DnsBenchmarkResult) {
    Surface(
        color = TechNavyCard,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (bench.isLocalDns) 1.5.dp else 0.5.dp,
            if (bench.isLocalDns) CyanNeon else TechNavyBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bench.isLocalDns) {
                        Surface(
                            color = CyanNeon,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LOCAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = bench.serverName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${bench.serverIp})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = if (bench.isSuccess) "${bench.durationMs} ms" else "Failed",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (bench.isSuccess) {
                        if (bench.durationMs < 50f) MintNeon else if (bench.durationMs < 120f) CyanNeon else StatusWarning
                    } else StatusError
                )
            }

            // Latency comparison progress bar
            Spacer(modifier = Modifier.height(6.dp))
            val progress = (bench.durationMs / 300f).coerceIn(0.05f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (bench.durationMs < 50f) MintNeon else if (bench.durationMs < 120f) CyanNeon else StatusWarning,
                trackColor = TechNavyBorder
            )

            if (bench.records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A Records: " + bench.records.take(3).joinToString(", "),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TRACEROUTE TAB
// -------------------------------------------------------------
@Composable
fun TracerouteTab(viewModel: NetworkViewModel, uiState: DiagnosticUiState) {
    val hops = uiState.traceHops
    val report = uiState.traceReport

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechNavyCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = uiState.traceHost,
                    onValueChange = { viewModel.updateTraceHost(it) },
                    label = { Text(stringResource(R.string.target_host)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_trace_host"),
                    colors = outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isTraceRunning) {
                    Button(
                        onClick = { viewModel.stopCurrentTest() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_stop_trace")
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_stop))
                    }
                } else {
                    Button(
                        onClick = { viewModel.startTraceroute() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00373D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_trace")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_start))
                    }
                }
            }
        }

        // Traceroute live hops list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hops Discovered (${hops.size})",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            if (report?.isTargetReached == true) {
                Surface(
                    color = MintNeon.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintNeon)
                ) {
                    Text(
                        text = stringResource(R.string.trace_destination_reached),
                        color = MintNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(hops) { hop ->
                Surface(
                    color = TechNavyCard,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (hop.isReached) 1.dp else 0.5.dp,
                        if (hop.isReached) MintNeon else TechNavyBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (hop.isReached) MintNeon else TechNavySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${hop.hopIndex}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hop.isReached) Color.Black else CyanNeon
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = hop.ip,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (hop.isTimeout) TextMuted else TextPrimary
                                )
                                if (!hop.hostname.isNullOrBlank()) {
                                    Text(
                                        text = hop.hostname,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (hop.isTimeout) "* * *" else "${hop.rttMs} ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (hop.isTimeout) TextMuted else if (hop.isReached) MintNeon else CyanNeon
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HISTORY TAB
// -------------------------------------------------------------
@Composable
fun HistoryTab(viewModel: NetworkViewModel, reports: List<ReportEntity>) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title) + " (${reports.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (reports.isNotEmpty()) {
                TextButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_clear), color = StatusError, fontSize = 12.sp)
                }
            }
        }

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports) { item ->
                    ReportCardItem(
                        report = item,
                        onClick = { viewModel.setActiveDetailReport(item) },
                        onDelete = { viewModel.deleteReport(item.id) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.confirm_clear_history)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("OK", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportCardItem(
    report: ReportEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(report.timestamp) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(report.timestamp))
    }

    val typeColor = when (report.testType) {
        "PING" -> CyanNeon
        "TCP" -> ElectricBlue
        "DNS" -> MintNeon
        "TRACEROUTE" -> Color(0xFFFFB300)
        "FULL_DIAG" -> Color(0xFFE040FB)
        else -> TextPrimary
    }

    Surface(
        color = TechNavyCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, typeColor)
                    ) {
                        Text(
                            text = report.testType,
                            color = typeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.targetHost,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.summary,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// REPORT DETAIL DIALOG (Export JSON & Text & Share)
// -------------------------------------------------------------
@Composable
fun ReportDetailDialog(
    report: ReportEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedViewMode by remember { mutableIntStateOf(0) } // 0: Formatted Text, 1: Raw JSON
    val formattedText = remember(report) { DiagnosticsReporter.formatReportToText(report.detailsJson) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.report_detail_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    color = CyanNeon.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = report.testType,
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedViewMode,
                    containerColor = TechNavySurface,
                    contentColor = CyanNeon
                ) {
                    Tab(
                        selected = selectedViewMode == 0,
                        onClick = { selectedViewMode = 0 },
                        text = { Text("Report Text", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedViewMode == 1,
                        onClick = { selectedViewMode = 1 },
                        text = { Text("JSON", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(TechNavySurface, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (selectedViewMode == 0) formattedText else report.detailsJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Copy Button
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NetDiag Report", if (selectedViewMode == 0) formattedText else report.detailsJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_copy), fontSize = 12.sp)
                }

                // Share Button
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "NetDiag Test Report - ${report.testType}")
                            putExtra(Intent.EXTRA_TEXT, if (selectedViewMode == 0) formattedText else report.detailsJson)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.btn_share)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00373D))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_share), fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// -------------------------------------------------------------
// DEEPLINK DIALOG
// -------------------------------------------------------------
@Composable
fun DeepLinkDialog(
    onDismiss: () -> Unit,
    onLaunchSample: (String) -> Unit
) {
    val context = LocalContext.current
    val samples = listOf(
        "netdiag://test?type=ping&host=1.1.1.1" to "Ping 1.1.1.1",
        "netdiag://test?type=tcp&host=google.com&port=443" to "TCP Handshake 443",
        "netdiag://test?type=dns&host=github.com" to "DNS Benchmark github.com",
        "netdiag://test?type=traceroute&host=8.8.8.8" to "Traceroute 8.8.8.8",
        "netdiag://test?type=fulldiag&host=cloudflare.com" to "Full Network Diagnostic"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = CyanNeon)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.deeplink_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.deeplink_desc),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                samples.forEach { (url, label) ->
                    Surface(
                        color = TechNavySurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TechNavyBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                TextButton(
                                    onClick = { onLaunchSample(url) },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Test Launch", fontSize = 11.sp, color = CyanNeon)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = url,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CyanNeon,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("DeepLink", url))
                                    Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// -------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------
@Composable
fun InfoRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatPill(label: String, value: String, isHighlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) CyanNeon else TextPrimary
        )
    }
}

@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = TechNavyBorder,
    focusedLabelColor = CyanNeon,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanNeon
)
