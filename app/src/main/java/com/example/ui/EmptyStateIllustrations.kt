package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Custom Empty State Illustration for Main Encrypted Notes & Secrets List.
 */
@Composable
fun EmptyVaultIllustration(
    onAddFirstSecret: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmptyVaultPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Fidelity Layered Canvas Illustration
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = center
                val radius = size.minDimension / 2f

                // Ambient Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = pulseAlpha * 0.4f),
                            tertiaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = canvasCenter,
                        radius = radius
                    )
                )

                // Concentric Radar Rings
                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = radius * 0.85f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = radius * 0.65f,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Shield Icon Badge Core
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .size(72.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.empty_notes_main_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.empty_notes_main_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Button(
            onClick = onAddFirstSecret,
            modifier = Modifier
                .dpadFocusable(shape = RoundedCornerShape(20.dp))
                .testTag("btn_empty_add_secret")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.store_first_secret))
        }
    }
}

/**
 * Custom Empty State Illustration tailored specifically for selected Categories
 * (e.g. WORK, PERSONAL, FINANCE, PASSWORD, PIN, NOTE).
 */
@Composable
fun EmptyCategoryIllustration(
    categoryKey: String,
    categoryLabel: String,
    onAddSecret: () -> Unit,
    onClearCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, accentColor) = remember(categoryKey) {
        when (categoryKey.uppercase()) {
            "WORK" -> Icons.Default.BusinessCenter to Color(0xFF3B82F6)
            "PERSONAL" -> Icons.Default.Person to Color(0xFF10B981)
            "FINANCE" -> Icons.Default.AccountBalance to Color(0xFFF59E0B)
            "PASSWORD" -> Icons.Default.Key to Color(0xFF8B5CF6)
            "PIN" -> Icons.Default.Pin to Color(0xFFEC4899)
            else -> Icons.Default.Description to Color(0xFF06B6D4)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = center
                val radius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = canvasCenter,
                        radius = radius
                    )
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.3f),
                    radius = radius * 0.8f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .size(68.dp)
                    .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.empty_category_title, categoryLabel),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.empty_category_desc, categoryLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onClearCategory,
                modifier = Modifier.dpadFocusable(shape = RoundedCornerShape(20.dp))
            ) {
                Text(stringResource(R.string.clear_search))
            }

            Button(
                onClick = onAddSecret,
                modifier = Modifier
                    .dpadFocusable(shape = RoundedCornerShape(20.dp))
                    .testTag("btn_empty_category_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.store_first_category_secret, categoryLabel))
            }
        }
    }
}

/**
 * Custom Empty State Illustration for Intruder Snapshots list.
 */
@Composable
fun EmptyIntruderIllustration(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val emeraldColor = Color(0xFF10B981)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = center
                val radius = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            emeraldColor.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = canvasCenter,
                        radius = radius
                    )
                )
                drawCircle(
                    color = emeraldColor.copy(alpha = 0.3f),
                    radius = radius * 0.82f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .size(68.dp)
                    .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.7f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.empty_intruder_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.empty_intruder_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

/**
 * Custom Empty State Illustration for Soft-Deleted Trash Bin.
 */
@Composable
fun EmptyTrashIllustration(
    trashPurgeDays: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val strokeColor = MaterialTheme.colorScheme.outline
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = center
                val radius = size.minDimension / 2f

                drawCircle(
                    color = strokeColor.copy(alpha = 0.12f),
                    radius = radius,
                    center = canvasCenter
                )
                drawCircle(
                    color = strokeColor.copy(alpha = 0.25f),
                    radius = radius * 0.85f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                tonalElevation = 4.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.empty_trash_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.empty_trash_desc, trashPurgeDays),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
