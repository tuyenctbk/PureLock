package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EncryptedVaultEntity
import com.example.service.PasswordGeneratorService
import com.example.service.PasswordStrength
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VaultInsightsDialog(
    vaultItems: List<EncryptedVaultEntity>,
    onDismiss: () -> Unit
) {
    val passwordService = remember { PasswordGeneratorService() }

    // Statistical calculations
    val totalCount = vaultItems.size
    val entropyList = remember(vaultItems) {
        vaultItems.map { passwordService.calculateEntropyBits(it.secretContent) }
    }
    val avgEntropy = remember(entropyList) {
        if (entropyList.isEmpty()) 0.0 else entropyList.average()
    }
    val avgStrengthLabel = remember(avgEntropy) {
        when {
            avgEntropy >= 75.0 -> "Very Strong"
            avgEntropy >= 55.0 -> "Strong"
            avgEntropy >= 35.0 -> "Fair"
            else -> "Weak"
        }
    }

    // Health Index (0 - 100%)
    val healthIndex = remember(avgEntropy, totalCount) {
        if (totalCount == 0) 100
        else (avgEntropy.coerceIn(0.0, 100.0)).toInt()
    }

    // Strength tier breakdown
    val weakCount = remember(entropyList) { entropyList.count { it < 35.0 } }
    val fairCount = remember(entropyList) { entropyList.count { it in 35.0..54.9 } }
    val goodCount = remember(entropyList) { entropyList.count { it in 55.0..74.9 } }
    val strongCount = remember(entropyList) { entropyList.count { it >= 75.0 } }

    // Category breakdown
    val categoryCounts = remember(vaultItems) {
        vaultItems.groupingBy {
            when (it.category.uppercase()) {
                "WORK" -> "Work"
                "PERSONAL" -> "Personal"
                "FINANCE" -> "Finance"
                "PASSWORD" -> "Passwords"
                "CREDENTIAL" -> "Credentials"
                "PIN", "BANK_PIN" -> "PINs"
                "API_KEY" -> "API Keys"
                "RECOVERY_KEY" -> "Recovery Keys"
                "CARD" -> "Cards"
                else -> "Notes"
            }
        }.eachCount()
    }

    // Timeline breakdown (Creation & update frequency)
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60 * 60 * 1000
    val sevenDaysMs = 7L * oneDayMs
    val thirtyDaysMs = 30L * oneDayMs

    val past24hCount = remember(vaultItems) { vaultItems.count { now - it.timestamp <= oneDayMs } }
    val past7dCount = remember(vaultItems) { vaultItems.count { now - it.timestamp in (oneDayMs + 1)..sevenDaysMs } }
    val past30dCount = remember(vaultItems) { vaultItems.count { now - it.timestamp in (sevenDaysMs + 1)..thirtyDaysMs } }
    val olderCount = remember(vaultItems) { vaultItems.count { now - it.timestamp > thirtyDaysMs } }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .testTag("dialog_vault_insights"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.vault_insights_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.vault_insights_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatCard(
                        title = "Total Notes",
                        value = totalCount.toString(),
                        subtitle = "Encrypted at Rest",
                        icon = Icons.Default.Lock,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Health Index",
                        value = "$healthIndex%",
                        subtitle = avgStrengthLabel,
                        icon = Icons.Default.Shield,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Section 1: Average Strength Gauge (D3 Circular Gauge Chart)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Average Password Strength & Entropy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        D3StrengthGaugeChart(
                            avgEntropy = avgEntropy,
                            healthScore = healthIndex,
                            strengthLabel = avgStrengthLabel,
                            modifier = Modifier
                                .size(160.dp)
                                .padding(vertical = 8.dp)
                        )

                        Text(
                            text = if (totalCount > 0) {
                                "Average entropy is %.1f bits per secret. %s".format(
                                    avgEntropy,
                                    if (avgEntropy >= 60.0) "Your credentials have high cryptographic resistance to brute-force attacks."
                                    else "Consider using longer passwords with mixed special characters."
                                )
                            } else "Add your encrypted notes and credentials to see real-time security entropy metrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Section 2: Password Strength Breakdown (D3 Multi-Bar Histogram)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.insights_strength_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        D3StrengthHistogram(
                            total = totalCount,
                            weak = weakCount,
                            fair = fairCount,
                            good = goodCount,
                            strong = strongCount
                        )
                    }
                }

                // Section 3: Category Distribution Donut Chart (D3 Donut Chart)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.insights_category_distribution),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (totalCount > 0) {
                            D3CategoryDonutChart(
                                categoryCounts = categoryCounts,
                                totalCount = totalCount,
                                modifier = Modifier
                                    .size(150.dp)
                                    .padding(vertical = 4.dp)
                            )

                            // Donut Chart Legend
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val palette = listOf(
                                    Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF9800),
                                    Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF9C27B0)
                                )
                                var colorIdx = 0
                                categoryCounts.forEach { (cat, count) ->
                                    val color = palette[colorIdx % palette.size]
                                    colorIdx++
                                    val pct = (count.toFloat() / totalCount * 100).toInt()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "$count ($pct%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No category data available yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                // Section 4: Usage & Storage Frequency Timeline Chart
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.insights_usage_frequency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        D3TimelineFrequencyChart(
                            total = totalCount,
                            past24h = past24hCount,
                            past7d = past7dCount,
                            past30d = past30dCount,
                            older = olderCount
                        )
                    }
                }

                // Security Audit Card
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Zero-Cloud Hardware Protection",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "All vault insights are computed on-device with zero network telemetry or tracking.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_vault_insights")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Custom D3-Style Circular Gauge Chart drawn with Canvas.
 */
@Composable
private fun D3StrengthGaugeChart(
    avgEntropy: Double,
    healthScore: Int,
    strengthLabel: String,
    modifier: Modifier = Modifier
) {
    val progress = (avgEntropy / 100.0).coerceIn(0.0, 1.0).toFloat()
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            // Background Track Arc (240 degrees sweep)
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground Gradient Arc
            if (progress > 0.01f) {
                val sweep = 240f * progress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFE53935), // Weak (Red)
                            Color(0xFFFFB300), // Fair (Amber)
                            Color(0xFF43A047), // Strong (Green)
                            primaryColor
                        )
                    ),
                    startAngle = 150f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%.1f".format(avgEntropy),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "bits avg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strengthLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when (strengthLabel) {
                    "Very Strong" -> Color(0xFF2E7D32)
                    "Strong" -> Color(0xFF43A047)
                    "Fair" -> Color(0xFFFB8C00)
                    else -> Color(0xFFE53935)
                }
            )
        }
    }
}

/**
 * Custom D3-Style Strength Breakdown Histogram.
 */
@Composable
private fun D3StrengthHistogram(
    total: Int,
    weak: Int,
    fair: Int,
    good: Int,
    strong: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StrengthTierRow(
            label = "Strong (75+ bits)",
            count = strong,
            total = total,
            barColor = Color(0xFF43A047)
        )
        StrengthTierRow(
            label = "Good (55-74 bits)",
            count = good,
            total = total,
            barColor = Color(0xFF00ACC1)
        )
        StrengthTierRow(
            label = "Fair (35-54 bits)",
            count = fair,
            total = total,
            barColor = Color(0xFFFB8C00)
        )
        StrengthTierRow(
            label = "Weak (<35 bits)",
            count = weak,
            total = total,
            barColor = Color(0xFFE53935)
        )
    }
}

@Composable
private fun StrengthTierRow(
    label: String,
    count: Int,
    total: Int,
    barColor: Color
) {
    val fraction = if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
    val pct = (fraction * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text = "$count ($pct%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceAtLeast(if (count > 0) 0.04f else 0f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Custom D3-Style Category Donut Chart drawn with Canvas.
 */
@Composable
private fun D3CategoryDonutChart(
    categoryCounts: Map<String, Int>,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF9C27B0)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

            var startAngle = -90f
            var colorIdx = 0
            categoryCounts.forEach { (_, count) ->
                val sweep = (count.toFloat() / totalCount) * 360f
                val color = palette[colorIdx % palette.size]
                colorIdx++

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep - 2f, // 2 degree gap for clean D3 look
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweep
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$totalCount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Custom D3-Style Timeline Frequency Chart.
 */
@Composable
private fun D3TimelineFrequencyChart(
    total: Int,
    past24h: Int,
    past7d: Int,
    past30d: Int,
    older: Int
) {
    val periods = listOf(
        "Past 24h" to past24h,
        "Past 7d" to past7d,
        "Past 30d" to past30d,
        "Older (>30d)" to older
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        periods.forEach { (label, count) ->
            val fraction = if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
            val pct = (fraction * 100).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodySmall)
                    Text(text = "$count ($pct%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction.coerceAtLeast(if (count > 0) 0.04f else 0f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}
