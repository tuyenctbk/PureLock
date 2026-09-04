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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityLogEntity
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRangeFilter(val label: String, val durationMs: Long) {
    HOURS_24("24h", 24L * 60 * 60 * 1000),
    DAYS_7("7d", 7L * 24 * 60 * 60 * 1000),
    DAYS_30("30d", 30L * 24 * 60 * 60 * 1000),
    ALL_TIME("All", Long.MAX_VALUE)
}

enum class EventCategoryFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.AllInclusive),
    INTRUDER("Threats", Icons.Default.Warning),
    DURESS("Panic", Icons.Default.GppBad),
    UNLOCK("Unlocks", Icons.Default.LockOpen),
    TIMEOUT("Timeouts", Icons.Default.Timer),
    SYSTEM("System", Icons.Default.Shield)
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

    val now = System.currentTimeMillis()

    // Filter logs by time
    val timeFilteredLogs = remember(securityLogs, selectedTimeRange) {
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
                    label = { Text(filter.label, fontSize = 12.sp, fontWeight = if (selectedTimeRange == filter) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("chip_timerange_${filter.name.lowercase()}")
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            EventCategoryFilter.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat.label, fontSize = 12.sp) },
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
                placeholder = { Text("Filter audit logs...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
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
                    contentDescription = "Export CSV",
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
                        contentDescription = "Clear",
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
                        text = "No Events Recorded",
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
            title = { Text("Protection Rating", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Score is based on protected app coverage and active defense layers.", style = MaterialTheme.typography.bodySmall)
                    Text("• 0s-30s auto-lock threshold minimizes vulnerability exposure.", style = MaterialTheme.typography.bodySmall)
                    Text("• SQLCipher AES-256 secures all databases locally with zero telemetry.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showChartInfoDialog = false }) { Text("Got It") }
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
                            text = if (isThreatActive) "$threatCount Alerts Detected" else "Protection Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isThreatActive) "Threats Logged" else "Active Defense",
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
                            contentDescription = "Details",
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AES-256 SQLCipher", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${backgroundTimeoutSec}s Auto-Lock", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        text = log.action.replace("_", " "),
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
                    text = log.action.replace("_", " "),
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
                            text = "Timestamp",
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
                            text = "Details",
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
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
