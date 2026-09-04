package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Reusable PureLock App Icon & Security Logo Emblem.
 * Faithfully mirrors the Launcher App Icon everywhere across the application:
 * - Splash Screen
 * - Onboarding Flows
 * - Lock Overlay Screen
 * - Screen Header Badges (Vault, App Shield, Intruder Vault, Security Analytics, Invisible Suite)
 * - Security Architecture Trust Card
 */
@Composable
fun PureLockLogoEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlowRing: Boolean = false,
    showRadarScan: Boolean = false,
    isSquircle: Boolean = true,
    badgeBackground: Color? = null,
    elevation: Dp = 0.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PureLockEmblemAnim")

    val pulseScale by if (showGlowRing) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowPulse"
        )
    } else {
        rememberUpdatedState(1f)
    }

    val glowAlpha by if (showGlowRing) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val scanAngle by if (showRadarScan) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ScanAngle"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val containerSize = if (showGlowRing || showRadarScan) size * 1.35f else size
    val iconShape: Shape = if (isSquircle) RoundedCornerShape(size * 0.22f) else CircleShape

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Radial Aura
        if (showGlowRing) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .alpha(glowAlpha)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00F0FF).copy(alpha = 0.40f),
                            Color(0xFF0284C7).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    radius = size.toPx() * 0.65f
                )

                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                    radius = size.toPx() * 0.55f,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Rotating radar scan arc
        if (showRadarScan) {
            Canvas(
                modifier = Modifier
                    .size(size * 1.15f)
                    .alpha(0.75f)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00F0FF).copy(alpha = 0.85f)
                        )
                    ),
                    startAngle = scanAngle,
                    sweepAngle = 85f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // App Icon Container (exact squircle/circle badge matching launcher icon)
        Surface(
            modifier = Modifier
                .size(size)
                .clip(iconShape)
                .border(
                    width = if (size > 40.dp) 1.2.dp else 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00F0FF).copy(alpha = 0.6f),
                            Color(0xFF0284C7).copy(alpha = 0.3f),
                            Color(0xFF0F172A).copy(alpha = 0.8f)
                        )
                    ),
                    shape = iconShape
                ),
            shape = iconShape,
            color = badgeBackground ?: Color(0xFF030712),
            shadowElevation = elevation
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_purelock_app_icon),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
