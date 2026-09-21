package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.SecurityLogEntity
import com.example.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldScreen(
    viewModel: PureLockViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val securityLogs by viewModel.securityLogs.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var showRestrictedSettingsHelp by remember { mutableStateOf(false) }

    var isAccessibilityGranted by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionUtils.isOverlayPermissionGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
                isOverlayGranted = PermissionUtils.isOverlayPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val lockedAppsCount = remember(allApps) { allApps.count { it.isLocked } }
    val protectionScore = if (allApps.isNotEmpty()) (lockedAppsCount * 100) / allApps.size else 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PureLockLogoEmblem(
                        size = 38.dp,
                        showGlowRing = false,
                        badgeBackground = MaterialTheme.colorScheme.primaryContainer
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.nav_audit),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.splash_badge_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_audit_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.shield_details_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Service Privileges Status Strip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Accessibility Service Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityGranted) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        stringResource(R.string.onboarding_perm_acc_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isAccessibilityGranted) "(${stringResource(R.string.perm_status_active)})" else "(${stringResource(R.string.perm_status_inactive)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAccessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    stringResource(R.string.onboarding_perm_acc_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (!isAccessibilityGranted) {
                            OutlinedButton(
                                onClick = { showAccessibilityDisclosure = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("btn_grant_accessibility")
                            ) {
                                Text(stringResource(R.string.perm_action_grant), fontSize = 12.sp)
                            }
                        }
                    }

                    if (!isAccessibilityGranted) {
                        TextButton(
                            onClick = { showRestrictedSettingsHelp = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).align(Alignment.Start)
                        ) {
                            Text(
                                text = stringResource(R.string.restricted_settings_help_btn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Overlay Permission Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isOverlayGranted) Icons.Default.CheckCircle else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isOverlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        stringResource(R.string.onboarding_perm_overlay_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isOverlayGranted) "(${stringResource(R.string.perm_status_granted)})" else "(${stringResource(R.string.perm_status_required)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOverlayGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    stringResource(R.string.onboarding_perm_overlay_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (!isOverlayGranted) {
                            OutlinedButton(
                                onClick = { PermissionUtils.openOverlaySettings(context) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("btn_grant_overlay")
                            ) {
                                Text(stringResource(R.string.perm_action_grant), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Security Analytics Dashboard (Threat level gauge, activity bar chart, donut chart, interactive audit stream)
            SecurityAnalyticsDashboard(
                securityLogs = securityLogs,
                protectionScore = protectionScore,
                isSqlCipherActive = true,
                backgroundTimeoutSec = 30,
                onClearLogs = { viewModel.clearAllSecurityLogs() },
                onExportLogs = { showExportDialog = true }
            )
        }
    }

    // Privacy Details Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.insights_zero_cloud_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.insights_zero_cloud_bullet1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.insights_zero_cloud_bullet2), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.insights_zero_cloud_bullet3), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    // Accessibility Service Disclosure Dialog
    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            icon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.onboarding_disclosure_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_why_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_why_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_privacy_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_privacy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        PermissionUtils.openAccessibilitySettings(context)
                    }
                ) {
                    Text(stringResource(R.string.onboarding_disclosure_agree))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Restricted Settings Help Dialog
    if (showRestrictedSettingsHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsHelp = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.restricted_settings_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.restricted_settings_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = stringResource(R.string.restricted_settings_step_1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = stringResource(R.string.restricted_settings_step_2), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = stringResource(R.string.restricted_settings_step_3), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(text = stringResource(R.string.restricted_settings_step_4), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedSettingsHelp = false
                        PermissionUtils.openAppInfoSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.restricted_settings_open_app_info))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsHelp = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Export Logs Dialog
    if (showExportDialog) {
        val exportText = remember(securityLogs) {
            val sb = StringBuilder()
            sb.append("Timestamp,Action,Details\n")
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            securityLogs.forEach { log ->
                sb.append("${fmt.format(Date(log.timestamp))},\"${log.action}\",\"${log.details.replace("\"", "\"\"")}\"\n")
            }
            sb.toString()
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.suite_export_backup_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.privacy_export_csv_header, securityLogs.size), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = exportText.take(300) + if (exportText.length > 300) "..." else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportText))
                        Toast.makeText(context, context.getString(R.string.privacy_export_copied_toast), Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text(stringResource(R.string.generator_btn_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
