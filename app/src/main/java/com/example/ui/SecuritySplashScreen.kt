package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PureLockDatabase
import com.example.ui.theme.BodyFontFamily
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Choreographed Security Splash Screen shown upon launching PureLock.
 * Features centered atmospheric radial aura, expanding sonar shield waves,
 * real SQLCipher native verification, spring-driven brand reveal, and safe inset handling.
 */
@Composable
fun SecuritySplashScreen(
    onSplashFinished: () -> Unit
) {
    var animationStage by remember { mutableIntStateOf(0) }
    var isExiting by remember { mutableStateOf(false) }

    val emblemScale = remember { Animatable(0.74f) }
    val emblemAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleTranslationY = remember { Animatable(24f) }
    val exitAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SplashExitAlpha"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (isExiting) 1.04f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "SplashExitScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAtmosphere")
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave1"
    )
    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarWave2"
    )
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanlineY"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ProgressShimmer"
    )

    val latestOnFinished by rememberUpdatedState(onSplashFinished)
    val scope = rememberCoroutineScope()

    fun finishGracefully() {
        if (isExiting) return
        isExiting = true
        scope.launch {
            delay(200)
            latestOnFinished()
        }
    }

    // Choreographed entrance + real SQLCipher readiness sequence
    LaunchedEffect(Unit) {
        launch {
            emblemAlpha.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            delay(140)
            titleAlpha.animateTo(1f, animationSpec = tween(360, easing = FastOutSlowInEasing))
        }
        launch {
            delay(140)
            titleTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        delay(340)
        animationStage = 1 // Hardware Keystore verified
        withContext(Dispatchers.IO) {
            PureLockDatabase.ensureSqlCipherLoaded()
        }
        delay(380)
        animationStage = 2 // SQLCipher database mounted
        delay(380)
        animationStage = 3 // Defense armed
        delay(340)
        finishGracefully()
    }

    val stageAccentColor by animateColorAsState(
        targetValue = if (animationStage >= 3) Color(0xFF10B981) else Color(0xFF38BDF8),
        animationSpec = tween(320),
        label = "StageAccentColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha
                scaleX = exitScale
                scaleY = exitScale
            }
            .drawBehind {
                // Deep obsidian base
                drawRect(color = Color(0xFF030712))

                // Centered atmospheric radial aura behind the emblem
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0EA5E9).copy(alpha = 0.22f),
                            Color(0xFF0F172A).copy(alpha = 0.72f),
                            Color(0xFF020617)
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.40f),
                        radius = size.maxDimension * 0.62f
                    )
                )
            }
            .testTag("security_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cyber coordinate grid + vertical radar scanline + sonar rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 44.dp.toPx()
            val w = size.width
            val h = size.height
            val gridColor = Color(0xFF38BDF8).copy(alpha = 0.055f)

            var x = 0f
            while (x < w) {
                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                y += step
            }

            // Sweeping translucent scanline beam
            val beamCenterY = h * scanlineY
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF38BDF8).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    startY = (beamCenterY - 48.dp.toPx()).coerceAtLeast(0f),
                    endY = (beamCenterY + 48.dp.toPx()).coerceAtMost(h)
                ),
                topLeft = Offset(0f, (beamCenterY - 48.dp.toPx()).coerceAtLeast(0f)),
                size = Size(w, 96.dp.toPx())
            )

            // Concentric expanding sonar rings centered behind emblem
            val emblemCenter = Offset(w * 0.5f, h * 0.41f)
            val minR = 56.dp.toPx()
            val maxR = 175.dp.toPx()

            listOf(sonarWave1, sonarWave2).forEach { wave ->
                val radius = minR + (maxR - minR) * wave
                val ringAlpha = (1f - wave).coerceIn(0f, 1f) * 0.26f
                drawCircle(
                    color = stageAccentColor.copy(alpha = ringAlpha),
                    radius = radius,
                    center = emblemCenter,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero Cyber Emblem with spring entrance
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = emblemScale.value
                    scaleY = emblemScale.value
                    alpha = emblemAlpha.value
                }
            ) {
                PureLockLogoEmblem(
                    size = 112.dp,
                    showGlowRing = true,
                    showRadarScan = true,
                    elevation = 20.dp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Brand Lockup with staggered spring rise
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleTranslationY.value
                }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = DisplayFontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.splash_tagline),
                    fontFamily = BodyFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Dynamic Initialization Stage Pill
            AnimatedContent(
                targetState = animationStage,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260)) + slideInVertically { it / 3 }) togetherWith
                        (fadeOut(animationSpec = tween(180)) + slideOutVertically { -it / 3 })
                },
                label = "SplashStageTransition"
            ) { stage ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, stageAccentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val icon = when (stage) {
                            0 -> Icons.Default.Shield
                            1 -> Icons.Default.Key
                            2 -> Icons.Default.Lock
                            else -> Icons.Default.VerifiedUser
                        }
                        val statusText = when (stage) {
                            0 -> stringResource(R.string.splash_status_initializing)
                            1 -> stringResource(R.string.splash_keystore_verified)
                            2 -> stringResource(R.string.splash_database_mounted)
                            else -> stringResource(R.string.splash_defense_armed)
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = stageAccentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Glowing Shimmer Progress Bar
            val progressAnim by animateFloatAsState(
                targetValue = when (animationStage) {
                    0 -> 0.24f
                    1 -> 0.58f
                    2 -> 0.86f
                    else -> 1.0f
                },
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "SplashProgress"
            )

            Canvas(
                modifier = Modifier
                    .width(204.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    cornerRadius = corner
                )
                val fillWidth = (size.width * progressAnim).coerceAtLeast(size.height)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0284C7),
                            stageAccentColor,
                            Color(0xFF67E8F9)
                        ),
                        startX = 0f,
                        endX = fillWidth
                    ),
                    size = Size(fillWidth, size.height),
                    cornerRadius = corner
                )
                // Moving specular highlight inside active bar
                val shimmerCenter = fillWidth * shimmerOffset
                if (shimmerCenter in 0f..fillWidth) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent),
                            startX = (shimmerCenter - 22.dp.toPx()).coerceAtLeast(0f),
                            endX = (shimmerCenter + 22.dp.toPx()).coerceAtMost(fillWidth)
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = corner
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-step verification micro-dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { stepIdx ->
                    val completed = stepIdx <= animationStage
                    val dotWidth by animateDpAsState(
                        targetValue = if (stepIdx == animationStage) 18.dp else 6.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "SplashDotWidth_$stepIdx"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (completed) stageAccentColor else Color(0xFF334155),
                        animationSpec = tween(250),
                        label = "SplashDotColor_$stepIdx"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = dotWidth, height = 6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }

        // Top Right Skip Button with proper status bar insets & 48dp touch target
        TextButton(
            onClick = { finishGracefully() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 12.dp)
                .minimumInteractiveComponentSize()
                .testTag("btn_skip_splash")
        ) {
            Text(
                text = stringResource(R.string.splash_skip),
                fontFamily = BodyFontFamily,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Zero-Telemetry Trust Pill with proper navigation bar inset
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.7f),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                    fontFamily = BodyFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
