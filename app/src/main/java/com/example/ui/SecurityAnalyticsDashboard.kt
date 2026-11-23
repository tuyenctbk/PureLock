package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SecurityLogEntity
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRangeFilter(@param:androidx.annotation.StringRes val labelRes: Int, val durationMs: Long) {
    HOURS_24(R.string.audit_range_24h, 24L * 60 * 60 * 1000),
    DAYS_7(R.string.audit_range_7d, 7L * 24 * 60 * 60 * 1000),
    DAYS_30(R.string.audit_range_30d, 30L * 24 * 60 * 60 * 1000),
    ALL_TIME(R.string.audit_range_all, Long.MAX_VALUE)
}

enum class EventCategoryFilter(@param:androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    ALL(R.string.audit_cat_all, Icons.Default.AllInclusive),
    INTRUDER(R.string.audit_cat_threats, Icons.Default.Warning),
    DURESS(R.string.audit_cat_panic, Icons.Default.GppBad),
    UNLOCK(R.string.audit_cat_unlocks, Icons.Default.LockOpen),
    TIMEOUT(R.string.audit_cat_timeouts, Icons.Default.Timer),
    SYSTEM(R.string.audit_cat_system, Icons.Default.Shield)
}

@Composable
fun SecurityAnalyticsDashboard(
    securityLogs: List<SecurityLogEntity>,
    protectionScore: Int,
    isSqlCipherActive: Boolean = true,
    backgroundTimeoutSec: Int = 30,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.DAYS_7) }
    var selectedCategoryFilter by remember { mutableStateOf(EventCategoryFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var inspectLog by remember { mutableStateOf<SecurityLogEntity?>(null) }
    var showChartInfoDialog by remember { mutableStateOf(false) }

    // Filter logs by time
    val timeFilteredLogs = remember(securityLogs, selectedTimeRange) {
        val now = System.currentTimeMillis()
        if (selectedTimeRange == TimeRangeFilter.ALL_TIME) {
            securityLogs
        } else {
            securityLogs.filter { (now - it.timestamp) <= selectedTimeRange.durationMs }
        }
    }

    // Filter logs by category and search query
    val displayedLogs = remember(timeFilteredLogs, selectedCategoryFilter, searchQuery) {
        timeFilteredLogs.filter { log ->
            val matchesCategory = when (selectedCategoryFilter) {
                EventCategoryFilter.ALL -> true
                EventCategoryFilter.INTRUDER -> log.action.contains("INTRUDER", ignoreCase = true) || log.action.contains("FAILED", ignoreCase = true)
                EventCategoryFilter.DURESS -> log.action.contains("PANIC", ignoreCase = true) || log.action.contains("DURESS", ignoreCase = true)
                EventCategoryFilter.UNLOCK -> log.action.contains("UNLOCK", ignoreCase = true) || log.action.contains("AUTH", ignoreCase = true)
                EventCategoryFilter.TIMEOUT -> log.action.contains("TIMEOUT", ignoreCase = true) || log.action.contains("AUTO_LOCK", ignoreCase = true)
                EventCategoryFilter.SYSTEM -> !log.action.contains("INTRUDER", ignoreCase = true) && !log.action.contains("UNLOCK", ignoreCase = true) && !log.action.contains("PANIC", ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.action.contains(searchQuery, ignoreCase = true) ||
                        log.details.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    val threatEventsCount = remember(timeFilteredLogs) {
        timeFilteredLogs.count {
            it.action.contains("INTRUDER", ignoreCase = true) ||
                    it.action.contains("FAILED", ignoreCase = true) ||
                    it.action.contains("PANIC", ignoreCase = true) ||
                    it.action.contains("DURESS", ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("security_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Minimal Threat Level Gauge Card
        ThreatLevelGaugeCard(
            protectionScore = protectionScore,
            threatCount = threatEventsCount,
            isSqlCipherActive = isSqlCipherActive,
            backgroundTimeoutSec = backgroundTimeoutSec,
            onInfoClick = { showChartInfoDialog = true }
        )

        // Time Range & Category Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeRangeFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedTimeRange == filter,
                    onClick = { selectedTimeRange = filter },
                    label = { Text(stringResource(filter.labelRes), fontSize = 12.sp, fontWeight = if (selectedTimeRange == filter) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("chip_timerange_${filter.name.lowercase()}")
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            EventCategoryFilter.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(stringResource(cat.labelRes), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(cat.icon, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.testTag("chip_cat_${cat.name.lowercase()}")
                )
            }
        }

        // Search Bar & Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.audit_search_placeholder), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_search_logs")
            )

            IconButton(
                onClick = onExportLogs,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("btn_export_analytics_logs")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = stringResource(R.string.export),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (displayedLogs.isNotEmpty()) {
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .testTag("btn_clear_analytics_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Log Stream
        if (displayedLogs.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.audit_no_events_recorded),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedLogs.take(40).forEach { log ->
                    SecurityLogItemCard(
                        log = log,
                        onClick = { inspectLog = log }
                    )
                }
            }
        }
    }

    // Chart / Score Info Dialog
    if (showChartInfoDialog) {
        AlertDialog(
            onDismissRequest = { showChartInfoDialog = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.audit_protection_rating_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.audit_protection_rating_bullet1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.audit_protection_rating_bullet2), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.audit_protection_rating_bullet3), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChartInfoDialog = false }) { Text(stringResource(R.string.got_it)) }
            }
        )
    }

    // Inspect Log Modal Dialog
    inspectLog?.let { log ->
        SecurityLogDetailDialog(
            log = log,
            onDismiss = { inspectLog = null }
        )
    }
}

@Composable
fun ThreatLevelGaugeCard(
    protectionScore: Int,
    threatCount: Int,
    isSqlCipherActive: Boolean,
    backgroundTimeoutSec: Int,
    onInfoClick: () -> Unit
) {
    val isThreatActive = threatCount > 0
    val statusColor = when {
        isThreatActive -> MaterialTheme.colorScheme.error
        protectionScore >= 70 -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(protectionScore) {
        animatedScore.animateTo(
            targetValue = protectionScore.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("card_threat_level_gauge")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isThreatActive) Icons.Default.Warning else Icons.Default.GppGood,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isThreatActive) stringResource(R.string.audit_alerts_detected_format, threatCount) else stringResource(R.string.audit_protection_status),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isThreatActive) stringResource(R.string.audit_threats_logged) else stringResource(R.string.audit_active_defense),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${animatedScore.value.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.details),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Quick Status Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isSqlCipherActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSqlCipherActive) Icons.Default.Lock else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (isSqlCipherActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(if (isSqlCipherActive) R.string.badge_aes_sqlcipher else R.string.badge_sqlcipher_pending),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (isSqlCipherActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.badge_autolock_format, backgroundTimeoutSec), style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityLogItemCard(
    log: SecurityLogEntity,
    onClick: () -> Unit
) {
    val isThreat = log.action.contains("INTRUDER", ignoreCase = true) ||
            log.action.contains("FAILED", ignoreCase = true) ||
            log.action.contains("PANIC", ignoreCase = true)
    val isUnlock = log.action.contains("UNLOCK", ignoreCase = true) || log.action.contains("AUTH", ignoreCase = true)
    val isTimeout = log.action.contains("TIMEOUT", ignoreCase = true) || log.action.contains("AUTO_LOCK", ignoreCase = true)

    val icon = when {
        isThreat -> Icons.Default.Warning
        isUnlock -> Icons.Default.LockOpen
        isTimeout -> Icons.Default.Timer
        else -> Icons.Default.Shield
    }

    val iconTint = when {
        isThreat -> MaterialTheme.colorScheme.error
        isUnlock -> Color(0xFF0284C7)
        isTimeout -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm • MMM dd", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isThreat) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("item_security_log_${log.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedSecurityActionLabel(log.action),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isThreat) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SecurityLogDetailDialog(
    log: SecurityLogEntity,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy • HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { timeFormat.format(Date(log.timestamp)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = localizedSecurityActionLabel(log.action),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.audit_timestamp_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.audit_details_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString("[${log.action}] $formattedTime: ${log.details}"))
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.btn_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@Composable
private fun localizedSecurityActionLabel(action: String): String {
    val resId = when (action.uppercase(Locale.ROOT)) {
        "APP_LOCKED" -> R.string.log_action_app_locked
        "APP_UNLOCKED" -> R.string.log_action_app_unlocked
        "VAULT_ITEM_ADDED" -> R.string.log_action_vault_item_added
        "VAULT_ITEM_UPDATED" -> R.string.log_action_vault_item_updated
        "VAULT_ITEM_TRASHED" -> R.string.log_action_vault_item_trashed
        "VAULT_ITEM_RESTORED" -> R.string.log_action_vault_item_restored
        "VAULT_ITEM_DELETED" -> R.string.log_action_vault_item_deleted
        "INTRUDER_DETECTED" -> R.string.log_action_intruder_detected
        "PANIC_MODE_ACTIVATED" -> R.string.log_action_panic_mode_activated
        "SHAKE_TO_LOCK" -> R.string.log_action_shake_to_lock
        "BACKGROUND_AUTO_LOCK" -> R.string.log_action_background_auto_lock
        "AUTO_LOCK_TRIGGERED" -> R.string.log_action_auto_lock_triggered
        "VAULT_INTEGRITY_CHECK" -> R.string.log_action_vault_integrity_check
        "VAULT_LOCKDOWN" -> R.string.log_action_vault_lockdown
        "QUICK_LOCKDOWN_TILE" -> R.string.log_action_quick_lockdown_tile
        "SETTINGS_CHANGED" -> R.string.log_action_settings_changed
        "SHIELD_INITIALIZED" -> R.string.log_action_shield_initialized
        "SENSITIVE_STATE_CLEARED" -> R.string.log_action_sensitive_state_cleared
        "AUTOFILL_REQUEST" -> R.string.log_action_autofill_request
        "NEW_PACKAGE_DETECTED" -> R.string.log_action_new_package_detected
        "PACKAGE_UNINSTALLED" -> R.string.log_action_package_uninstalled
        "APPS_SYNCED" -> R.string.log_action_apps_synced
        else -> 0
    }
    return if (resId != 0) stringResource(resId) else action.replace("_", " ")
}
