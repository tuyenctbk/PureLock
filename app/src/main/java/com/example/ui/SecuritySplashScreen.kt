package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * Animated Security Splash Screen shown upon launching PureLock.
 * Features a glowing cyber-shield pulse, hardware keystore validation animation,
 * and smooth transition to either Onboarding or Authenticated Vault.
 */
@Composable
fun SecuritySplashScreen(
    onSplashFinished: () -> Unit
) {
    var animationStage by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashPulse")
    
    // Stage progression timer
    LaunchedEffect(Unit) {
        delay(400)
        animationStage = 1
        delay(500)
        animationStage = 2
        delay(500)
        animationStage = 3
        delay(350)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF020617)
                    ),
                    center = Offset(0.5f, 0.45f)
                )
            )
            .testTag("security_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Cyber Grid / Scanline Canvas
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.08f)) {
            val step = 40.dp.toPx()
            val w = size.width
            val h = size.height

            var x = 0f
            while (x < w) {
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += step
            }

            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-Tech Cyber Emblem with ambient glow and radar sweep
            PureLockLogoEmblem(
                size = 110.dp,
                showGlowRing = true,
                showRadarScan = true,
                elevation = 16.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App Name with high contrast & tracked typography
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = stringResource(R.string.splash_tagline),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Dynamic Initialization Stage Status
            AnimatedContent(
                targetState = animationStage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + slideInVertically { it / 2 } togetherWith
                            fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 2 }
                },
                label = "SplashStageTransition"
            ) { stage ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    val icon = when (stage) {
                        0 -> Icons.Default.Shield
                        1 -> Icons.Default.Lock
                        2 -> Icons.Default.VerifiedUser
                        else -> Icons.Default.VerifiedUser
                    }
                    val statusText = when (stage) {
                        0 -> stringResource(R.string.splash_status_initializing)
                        1 -> stringResource(R.string.splash_status_keystore)
                        2 -> stringResource(R.string.splash_status_ready)
                        else -> stringResource(R.string.splash_status_ready)
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Linear Progress Indicator
            val progressAnim by animateFloatAsState(
                targetValue = when (animationStage) {
                    0 -> 0.25f
                    1 -> 0.65f
                    2 -> 0.95f
                    else -> 1.0f
                },
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "SplashProgress"
            )

            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00F0FF),
                trackColor = Color(0xFF1E293B)
            )
        }

        // Bottom Zero-Telemetry Trust Badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.splash_badge_offline),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }
    }
}
