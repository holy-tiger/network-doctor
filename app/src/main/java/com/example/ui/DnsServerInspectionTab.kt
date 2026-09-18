package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.network.DiagnosticStatus
import com.example.network.DiagnosticSubResult
import com.example.network.TargetInspectionResult
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningBg
import com.example.ui.theme.AmberWarningBorder
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueText
import com.example.ui.theme.BrandIndigo
import com.example.ui.theme.BrandLightBlueBg
import com.example.ui.theme.BrandLightBlueBorder
import com.example.ui.theme.CodeBadgeBg
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrimsonRedBg
import com.example.ui.theme.CrimsonRedBorder
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenBg
import com.example.ui.theme.EmeraldGreenBorder
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.TextSlateMuted
import com.example.ui.theme.TextSlatePrimary
import com.example.ui.theme.TextSlateSecondary

@Composable
fun DnsServerInspectionTab(
    viewModel: NetworkViewModel,
    uiState: DiagnosticUiState
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val rawTargets = remember(uiState.targetInputText) {
        uiState.targetInputText.lines()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
    val readyCount = rawTargets.size

    val filteredResults by remember(uiState.inspectionResults, uiState.inspectionFilter, uiState.inspectionSearchQuery) {
        derivedStateOf {
            val q = uiState.inspectionSearchQuery.trim().lowercase()
            uiState.inspectionResults.filter { item ->
                val matchesFilter = when (uiState.inspectionFilter) {
                    InspectionFilter.ALL -> true
                    InspectionFilter.FAILED_ONLY -> !item.isConnected
                    InspectionFilter.HEALTHY_ONLY -> item.isConnected
                }
                val matchesSearch = q.isEmpty() ||
                        item.host.lowercase().contains(q) ||
                        (item.resolvedIp?.contains(q) == true)
                matchesFilter && matchesSearch
            }
        }
    }

    val totalCount = uiState.inspectionResults.size
    val healthyCount = uiState.inspectionResults.count { it.isConnected }
    val failedCount = totalCount - healthyCount
    val avgLatency = if (uiState.inspectionResults.isNotEmpty()) {
        uiState.inspectionResults.map { it.totalDurationMs }.average().toLong()
    } else {
        0L
    }

    var showGenerateConfigDialog by remember { mutableStateOf(false) }
    var generatedDeepLink by remember { mutableStateOf<String?>(null) }
    var tempReportUrlInput by remember { mutableStateOf(uiState.reportUrl) }

    val onOpenGenerateDialog = {
        tempReportUrlInput = uiState.reportUrl
        showGenerateConfigDialog = true
    }

    // Step 1 Dialog: Input report URL endpoint, then generate final test link
    if (showGenerateConfigDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.generate_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextSlatePrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generate_dialog_desc),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextSlateSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.label_report_url),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlatePrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.optional_tag),
                                    fontSize = 11.sp,
                                    color = TextSlateMuted
                                )
                            }

                            OutlinedTextField(
                                value = tempReportUrlInput,
                                onValueChange = { tempReportUrlInput = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.hint_report_url),
                                        fontSize = 11.5.sp,
                                        color = TextSlateMuted
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextSlatePrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReportUrl = tempReportUrlInput.trim()
                        if (cleanReportUrl.isNotBlank()) {
                            viewModel.updateReportUrl(cleanReportUrl)
                        }
                        val link = viewModel.generateTestLink(cleanReportUrl)
                        showGenerateConfigDialog = false
                        generatedDeepLink = link
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("test_link", link))
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(R.string.btn_create_link))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateConfigDialog = false }) {
                    Text(text = stringResource(R.string.btn_close), color = TextSlateSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    // Step 2 Dialog: Generated DeepLink display & Fast test trigger
    if (generatedDeepLink != null) {
        val linkToDisplay = generatedDeepLink!!
        AlertDialog(
            onDismissRequest = { generatedDeepLink = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.deeplink_generated_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextSlatePrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.deeplink_generated_hint),
                        fontSize = 13.sp,
                        color = TextSlateSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = linkToDisplay,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            color = BrandBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("deeplink", linkToDisplay))
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                        val uri = Uri.parse(linkToDisplay)
                        generatedDeepLink = null
                        viewModel.handleDeepLink(uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.btn_selftest))
                }
            },
            dismissButton = {
                TextButton(onClick = { generatedDeepLink = null }) {
                    Text(text = stringResource(R.string.btn_close), color = TextSlateSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = true
        ) {
        // 1. Target Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_target_input")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header title & description
                    Text(
                        text = stringResource(R.string.target_input_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextSlatePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.target_input_subtitle),
                        fontSize = 13.sp,
                        color = TextSlateSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons above text area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Generate link button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                            modifier = Modifier
                                .clickable { onOpenGenerateDialog() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = EmeraldGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_generate_test_link),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }

                        // Advanced settings button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.clickable {
                                viewModel.setAdvancedSettingsOpen(true)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = TextSlateSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_advanced_settings),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSlateSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Clear input button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            modifier = Modifier.clickable { viewModel.clearTargetInput() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = TextSlateMuted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_clear_input),
                                    fontSize = 12.sp,
                                    color = TextSlateSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Textarea box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column {
                            OutlinedTextField(
                                value = uiState.targetInputText,
                                onValueChange = { viewModel.updateTargetInputText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_multi_targets"),
                                minLines = 3,
                                maxLines = 5,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = TextSlatePrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                })
                            )

                            // Ready addresses badge at bottom right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = stringResource(R.string.ready_addresses_count, readyCount),
                                    fontSize = 12.sp,
                                    color = TextSlateMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick presets row with horizontal scroll to prevent text wrapping/clipping on narrow screens or longer languages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AmberWarning
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.quick_presets_title),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSlateSecondary,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { viewModel.loadPreset(isAnomalyGroup = false) }
                        ) {
                            Text(
                                text = stringResource(R.string.preset_healthy_domains),
                                fontSize = 12.sp,
                                color = TextSlatePrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable { viewModel.loadPreset(isAnomalyGroup = true) }
                        ) {
                            Text(
                                text = stringResource(R.string.preset_anomaly_domains),
                                fontSize = 12.sp,
                                color = TextSlatePrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Buttons Row: [生成测试链接] + [开始全面检测]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onOpenGenerateDialog() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF0FDF4),
                                contentColor = Color(0xFF047857)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(R.string.btn_generate_test_link),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (uiState.isInspectionRunning) {
                                    viewModel.stopServerInspection()
                                } else {
                                    viewModel.startServerInspection()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_start_inspection")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isInspectionRunning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.btn_stop_inspection),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.btn_start_full_inspection),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.5 Auto-Report Status Banner / Notification
        if (uiState.isReportUploading || uiState.reportUploadStatus != null || (uiState.reportUrl.isNotBlank() && uiState.isAutoReportEnabled)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (uiState.isReportSuccess) {
                            true -> Color(0xFFF0FDF4) // Light green
                            false -> Color(0xFFFEF2F2) // Light red
                            else -> Color(0xFFF0F9FF) // Light sky blue
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (uiState.isReportSuccess) {
                            true -> Color(0xFF86EFAC)
                            false -> Color(0xFFFCA5A5)
                            else -> Color(0xFFBAE6FD)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isReportUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandBlue
                                )
                            } else {
                                Icon(
                                    imageVector = when (uiState.isReportSuccess) {
                                        true -> Icons.Default.CloudDone
                                        false -> Icons.Default.Error
                                        else -> Icons.Default.CloudUpload
                                    },
                                    contentDescription = null,
                                    tint = when (uiState.isReportSuccess) {
                                        true -> Color(0xFF16A34A)
                                        false -> Color(0xFFDC2626)
                                        else -> BrandBlue
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                val bannerText = uiState.reportUploadStatus
                                    ?: if (uiState.reportUrl.isNotBlank()) {
                                        "${stringResource(R.string.report_upload_status_idle)}: ${uiState.reportUrl.take(36)}${if (uiState.reportUrl.length > 36) "..." else ""}"
                                    } else {
                                        stringResource(R.string.switch_auto_report)
                                    }
                                Text(
                                    text = bannerText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when (uiState.isReportSuccess) {
                                        true -> Color(0xFF15803D)
                                        false -> Color(0xFFB91C1C)
                                        else -> Color(0xFF0369A1)
                                    }
                                )
                            }
                        }

                        // Manual trigger or dismiss
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!uiState.isReportUploading && uiState.reportUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { viewModel.uploadInspectionReport() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = BrandBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.btn_manual_report),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBlue
                                    )
                                }
                            }
                            if (uiState.reportUploadStatus != null) {
                                IconButton(
                                    onClick = { viewModel.dismissReportStatus() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = TextSlateSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Summary Stats Cards (2x2 Grid matching reference design)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Total Targets & Healthy Connected
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Targets Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.stat_total_targets),
                                fontSize = 12.sp,
                                color = TextSlateSecondary
                            )
                            Text(
                                text = "$totalCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlatePrimary
                            )
                        }
                    }

                    // Healthy Connected Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreenBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = EmeraldGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.stat_healthy_connected),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF047857)
                                )
                            }
                            Text(
                                text = "$healthyCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                // Row 2: Abnormal/Failed & Avg Total Latency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Abnormal/Failed Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonRedBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDA4AF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = CrimsonRed
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.stat_abnormal_failed),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CrimsonRed
                                )
                            }
                            Text(
                                text = "$failedCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonRed
                            )
                        }
                    }

                    // Avg Total Latency Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandLightBlueBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.stat_avg_latency),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandBlueText
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$avgLatency",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ms",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Jump to Results Bar
                if (filteredResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    // Item 2 is Filter Bar, Item 3 is Results Header, Item 4+ are cards
                                    listState.animateScrollToItem(2)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BrandBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.jump_to_results, filteredResults.size),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                        }
                    }
                }
            }
        }

        // 3. Filter Bar & Action Buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Filter Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterSegmentPill(
                            label = stringResource(R.string.filter_all, totalCount),
                            isSelected = uiState.inspectionFilter == InspectionFilter.ALL,
                            selectedColor = Color.White,
                            textColor = TextSlatePrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.updateFilter(InspectionFilter.ALL) }
                        )

                        FilterSegmentPill(
                            label = stringResource(R.string.filter_failed, failedCount),
                            isSelected = uiState.inspectionFilter == InspectionFilter.FAILED_ONLY,
                            selectedColor = Color.White,
                            textColor = CrimsonRed,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.updateFilter(InspectionFilter.FAILED_ONLY) }
                        )

                        FilterSegmentPill(
                            label = stringResource(R.string.filter_healthy, healthyCount),
                            isSelected = uiState.inspectionFilter == InspectionFilter.HEALTHY_ONLY,
                            selectedColor = Color.White,
                            textColor = EmeraldGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.updateFilter(InspectionFilter.HEALTHY_ONLY) }
                        )
                    }

                    // Search field & export action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = uiState.inspectionSearchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 12.sp, color = TextSlateMuted) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSlateMuted)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBlue,
                                unfocusedBorderColor = SurfaceCardBorder,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        // Copy Brief
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.clickable {
                                val brief = viewModel.generateBriefSummary()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("brief", brief))
                                Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSlateSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_copy_brief), fontSize = 12.sp, color = TextSlatePrimary)
                            }
                        }

                        // Export JSON
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder),
                            modifier = Modifier.clickable {
                                val json = viewModel.generateExportJson()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("export_json", json))
                                Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSlateSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.btn_export_json), fontSize = 12.sp, color = TextSlatePrimary)
                            }
                        }
                    }

                    // Save snapshot row
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.saveCurrentInspectionToHistory()
                                Toast.makeText(context, "已归档至快照历史", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(15.dp), tint = BrandBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_save_report),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                        }
                    }
                }
            }
        }

        // 4. Results List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.results_list_title, filteredResults.size, totalCount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlatePrimary
                )

                Text(
                    text = stringResource(R.string.view_full_markdown_report),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue,
                    modifier = Modifier
                        .clickable { viewModel.setMarkdownReportOpen(true) }
                        .testTag("link_view_markdown_report")
                )
            }
        }

        // 5. Results List Items (Screenshot 2!)
        items(filteredResults, key = { it.id }) { item ->
            TargetResultCard(
                item = item,
                onRetest = { viewModel.retestSingleTarget(item.rawInput) },
                onToggleExpand = { viewModel.toggleExpandTarget(item.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Scroll-to-Top Floating Action Button
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }
    AnimatedVisibility(
        visible = showScrollToTop,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(20.dp)
    ) {
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            containerColor = BrandBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

    // Markdown Report Dialog
    if (uiState.isMarkdownReportOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setMarkdownReportOpen(false) },
            confirmButton = {
                Button(
                    onClick = {
                        val md = viewModel.generateMarkdownReport()
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("report", md))
                        Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(stringResource(R.string.btn_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setMarkdownReportOpen(false) }) {
                    Text(stringResource(R.string.btn_clear))
                }
            },
            title = {
                Text("Markdown 报告预览", fontWeight = FontWeight.Bold, color = TextSlatePrimary)
            },
            text = {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = viewModel.generateMarkdownReport(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSlatePrimary,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }
            }
        )
    }

    // Advanced Settings Dialog
    if (uiState.isAdvancedSettingsOpen) {
        var tempUrl by remember(uiState.reportUrl) { mutableStateOf(uiState.reportUrl) }

        AlertDialog(
            onDismissRequest = { viewModel.setAdvancedSettingsOpen(false) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateReportUrl(tempUrl.trim())
                        viewModel.setAdvancedSettingsOpen(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("确定")
                }
            },
            title = {
                Text(stringResource(R.string.btn_advanced_settings), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Auto-Report Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = BrandBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.switch_auto_report),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlatePrimary
                                )
                            }
                            Switch(
                                checked = uiState.isAutoReportEnabled,
                                onCheckedChange = { viewModel.setAutoReportEnabled(it) },
                                modifier = Modifier.size(width = 38.dp, height = 24.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.label_report_url),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlateSecondary
                        )

                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.hint_report_url),
                                    fontSize = 11.sp,
                                    color = TextSlateMuted
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("检测参数配置", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSlateSecondary)
                        Text("• 默认端口: 443 (HTTPS) / 80 (HTTP)", fontSize = 12.sp, color = TextSlatePrimary)
                        Text("• 超时阈值: 3500ms ~ 4000ms", fontSize = 12.sp, color = TextSlatePrimary)
                        Text("• TLS 校验: 深度解析 X.509 证书有效期及链条", fontSize = 12.sp, color = TextSlatePrimary)
                        Text("• HTTP 请求方式: 优先 HEAD 探测，自动回退 GET", fontSize = 12.sp, color = TextSlatePrimary)
                    }
                }
            }
        )
    }
}

@Composable
fun FilterSegmentPill(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) textColor else TextSlateSecondary
        )
    }
}

@Composable
fun TargetResultCard(
    item: TargetInspectionResult,
    onRetest: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_card_${item.host}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Status badge pill + Host bold + :port (protocol)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                if (item.isConnected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EmeraldGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.badge_status_healthy),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CrimsonRedBg,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFDA4AF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = CrimsonRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.badge_status_failed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CrimsonRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Host and Port / protocol
                Text(
                    text = item.host,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlatePrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = ":${item.port} (${if (item.isHttps) "https" else "http"})",
                    fontSize = 13.sp,
                    color = TextSlateMuted
                )
            }

            // Row 2: Latency pill + Icon Buttons (Retest, Copy, Open in browser)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Latency pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextSlateSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.totalDurationMs}ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlatePrimary
                        )
                    }
                }

                // Action buttons: Retest, Copy, Open
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onRetest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retest",
                            modifier = Modifier.size(18.dp),
                            tint = TextSlateSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            val details = "${item.host} (IP: ${item.resolvedIp ?: "N/A"}) - ${item.totalDurationMs}ms\nDNS: ${item.dnsResult.summary}\nTCP: ${item.tcpResult.summary}\nTLS: ${item.tlsResult.summary}\nHTTP: ${item.httpResult.summary}"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("item_detail", details))
                            Toast.makeText(context, R.string.toast_copied, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(17.dp),
                            tint = TextSlateSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            val scheme = if (item.isHttps) "https" else "http"
                            val uri = Uri.parse("$scheme://${item.host}:${item.port}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Open",
                            modifier = Modifier.size(18.dp),
                            tint = TextSlateSecondary
                        )
                    }
                }
            }

            // Row 3: 4-Quadrant Diagnostic Grid (Screenshot 2!)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top Row: DNS & TCP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticMetricTile(
                        icon = Icons.Default.Language,
                        iconTint = BrandBlue,
                        title = stringResource(R.string.metric_dns_resolution),
                        result = item.dnsResult,
                        subDetail = item.dnsResult.summary,
                        modifier = Modifier.weight(1f)
                    )

                    DiagnosticMetricTile(
                        icon = Icons.Default.Wifi,
                        iconTint = BrandBlue,
                        title = stringResource(R.string.metric_tcp_connect),
                        result = item.tcpResult,
                        subDetail = item.tcpResult.summary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom Row: TLS & HTTP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticMetricTile(
                        icon = Icons.Default.Security,
                        iconTint = EmeraldGreen,
                        title = stringResource(R.string.metric_tls_cert),
                        result = item.tlsResult,
                        subDetail = item.tlsResult.summary,
                        modifier = Modifier.weight(1f)
                    )

                    DiagnosticMetricTile(
                        icon = Icons.Default.Http,
                        iconTint = Color(0xFF7C3AED),
                        title = stringResource(R.string.metric_http_response),
                        result = item.httpResult,
                        subDetail = item.httpResult.summary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 4: Resolved IP chip + Expand technical details button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.resolved_ip_label),
                        fontSize = 12.sp,
                        color = TextSlateSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CodeBadgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = item.resolvedIp ?: "N/A",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSlatePrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = if (item.isExpanded) {
                        stringResource(R.string.collapse_tech_details)
                    } else {
                        stringResource(R.string.expand_tech_details)
                    },
                    fontSize = 12.sp,
                    color = TextSlateSecondary,
                    modifier = Modifier.clickable(onClick = onToggleExpand)
                )
            }

            // Accordion: Expanded Technical Details
            AnimatedVisibility(
                visible = item.isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.2.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "技术诊断详情 (Technical Specifications)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSlatePrimary
                    )

                    // Resolved DNS records
                    if (item.allResolvedIps.isNotEmpty()) {
                        Text(
                            text = "A / AAAA 记录: ${item.allResolvedIps.joinToString(", ")}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextSlateSecondary
                        )
                    }

                    // TLS Certificate details
                    if (item.certValidUntil != null) {
                        Text(
                            text = "TLS 颁发机构: ${item.certIssuer ?: "未知"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextSlateSecondary
                        )
                        Text(
                            text = "证书到期日: ${item.certValidUntil} (${item.certExpiryDays ?: 0} 天后过期)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextSlateSecondary
                        )
                    }

                    // HTTP Response details
                    if (item.httpStatusLine != null) {
                        Text(
                            text = "HTTP 响应头状态: ${item.httpStatusLine}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextSlateSecondary
                        )
                    }

                    if (item.httpHeaders.isNotEmpty()) {
                        Text(
                            text = "Headers: ${item.httpHeaders.entries.take(4).joinToString("; ") { "${it.key}: ${it.value}" }}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextSlateMuted,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = BrandBlue,
    title: String,
    result: DiagnosticSubResult,
    subDetail: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Icon + Metric Title + Status Check icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconTint
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSlatePrimary
                    )
                }

                // Status check circle
                when (result.status) {
                    DiagnosticStatus.SUCCESS -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = EmeraldGreen
                    )
                    DiagnosticStatus.WARNING -> Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = AmberWarning
                    )
                    DiagnosticStatus.ERROR -> Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = CrimsonRed
                    )
                    DiagnosticStatus.PENDING -> CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = BrandBlue
                    )
                }
            }

            // Status label & latency
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (result.status) {
                    DiagnosticStatus.SUCCESS -> stringResource(R.string.status_ok)
                    DiagnosticStatus.WARNING -> stringResource(R.string.status_warning)
                    DiagnosticStatus.ERROR -> stringResource(R.string.status_fail)
                    DiagnosticStatus.PENDING -> stringResource(R.string.status_not_tested)
                }
                val statusColor = when (result.status) {
                    DiagnosticStatus.SUCCESS -> EmeraldGreen
                    DiagnosticStatus.WARNING -> AmberWarning
                    DiagnosticStatus.ERROR -> CrimsonRed
                    DiagnosticStatus.PENDING -> TextSlateMuted
                }

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                if (result.latencyMs > 0) {
                    Text(
                        text = "${result.latencyMs}ms",
                        fontSize = 12.sp,
                        color = TextSlateMuted
                    )
                }
            }

            // Sub detail text (e.g. IP: xxx or 端口: 443 or 有效剩 129 天)
            Text(
                text = subDetail.ifEmpty { "-" },
                fontSize = 11.sp,
                color = TextSlateSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
