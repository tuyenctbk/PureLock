package com.example.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PureLockRepository
import com.example.service.BiometricPromptManager
import com.example.service.BiometricStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun LockOverlayScreen(
    packageName: String,
    repository: PureLockRepository,
    onUnlocked: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var appName by remember { mutableStateOf(packageName) }
    var masterPin by remember { mutableStateOf("1234") }
    var duressPin by remember { mutableStateOf("0000") }
    var masterPattern by remember { mutableStateOf("1,2,5,8,9") }
    var securityType by remember { mutableStateOf("PIN") }
    var isRandomKeyboard by remember { mutableStateOf(false) }
    var isHidePatternPath by remember { mutableStateOf(false) }
    var isIntruderCapture by remember { mutableStateOf(true) }
    var isTvMode by remember { mutableStateOf(false) }

    var inputPin by remember { mutableStateOf("") }
    var patternSelectedNodes by remember { mutableStateOf(listOf<Int>()) }
    var failedAttempts by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var intruderCapturedBanner by remember { mutableStateOf(false) }
    var isUnlockingSuccess by remember { mutableStateOf(false) }
    var secondaryAuthNotice by remember { mutableStateOf<String?>(null) }

    // Keyboard keys (shuffled if random keyboard is enabled)
    val keyboardDigits = remember(isRandomKeyboard) {
        val list = (0..9).toList().toMutableList()
        if (isRandomKeyboard) {
            list.shuffle()
        }
        list
    }

    fun triggerHapticSuccess() {
        try {
            val vibrator = context.getSystemService(android.os.Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 50, 40, 50), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (e: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun triggerHapticFailure() {
        try {
            val vibrator = context.getSystemService(android.os.Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 90, 50, 90), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 90, 50, 90), -1)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (e: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun handleSuccessUnlock() {
        if (isUnlockingSuccess) return
        isUnlockingSuccess = true
        triggerHapticSuccess()
        scope.launch {
            delay(400) // Smooth unlock visual animation spec transition delay
            onUnlocked()
        }
    }

    fun recordFailedAttempt(msg: String) {
        errorMessage = msg
        triggerHapticFailure()
        failedAttempts++
        if (failedAttempts >= 3) {
            intruderCapturedBanner = true
            if (isIntruderCapture) {
                val sampleSnapshot = createIntruderSnapshotBitmap(appName)
                scope.launch {
                    repository.recordIntruderSelfie(
                        packageName = packageName,
                        appName = appName,
                        photoData = sampleSnapshot,
                        attempts = failedAttempts
                    )
                }
            }
        }
    }

    LaunchedEffect(packageName) {
        // Load app name
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            appName = packageName.substringAfterLast('.')
        }

        masterPin = repository.preferences.masterPin.first()
        duressPin = repository.preferences.duressPin.first()
        masterPattern = repository.preferences.masterPattern.first()
        securityType = repository.preferences.securityType.first()
        isRandomKeyboard = repository.preferences.randomKeyboard.first()
        isHidePatternPath = repository.preferences.hidePatternPath.first()
        isIntruderCapture = repository.preferences.intruderCapture.first()
        isTvMode = repository.preferences.tvMode.first()

        // Check biometric hardware presence & automatically fallback to secondary PIN/Pattern lock
        val bioManager = BiometricPromptManager(context)
        val bioStatus = bioManager.checkBiometricAvailability()

        if (securityType == "BIOMETRIC") {
            when (bioStatus) {
                BiometricStatus.AVAILABLE -> {
                    launchBiometricPrompt(
                        context = context,
                        onSuccess = { handleSuccessUnlock() },
                        onError = { err -> recordFailedAttempt(err) }
                    )
                }
                BiometricStatus.NO_HARDWARE -> {
                    securityType = "PIN"
                    secondaryAuthNotice = context.getString(R.string.lock_biometric_no_hardware)
                }
                BiometricStatus.NOT_ENROLLED -> {
                    securityType = "PIN"
                    secondaryAuthNotice = context.getString(R.string.lock_biometric_not_enrolled)
                }
                BiometricStatus.UNAVAILABLE -> {
                    securityType = "PIN"
                    secondaryAuthNotice = context.getString(R.string.lock_biometric_no_hardware)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTvMode) 640.dp else 420.dp)
        ) {
            // Header Shield Badge with smooth unlock animation feedback
            val animatedScale by animateFloatAsState(
                targetValue = if (isUnlockingSuccess) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "BadgeScale"
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                    .clip(CircleShape)
                    .background(
                        if (isUnlockingSuccess) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlockingSuccess) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.lock_access_granted),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.splash_logo),
                        contentDescription = stringResource(R.string.lock_security_shield),
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = if (isUnlockingSuccess) stringResource(R.string.lock_access_granted) else stringResource(R.string.lock_security_shield),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isUnlockingSuccess) {
                    stringResource(R.string.lock_auth_success_opening, appName)
                } else {
                    stringResource(R.string.lock_auth_required_for, appName)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Secondary Auth / Fallback Banner
            AnimatedVisibility(
                visible = secondaryAuthNotice != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = secondaryAuthNotice ?: "",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Error Message Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Intruder Selfie Captured Alert
            AnimatedVisibility(
                visible = intruderCapturedBanner,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Intruder Alert",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.lock_failed_attempts_intruder, failedAttempts),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Security Mode Selector Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = securityType == "PIN",
                    onClick = {
                        securityType = "PIN"
                        secondaryAuthNotice = null
                    },
                    label = { Text(stringResource(R.string.lock_mode_pin)) },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("chip_pin_mode")
                )
                FilterChip(
                    selected = securityType == "PATTERN",
                    onClick = {
                        securityType = "PATTERN"
                        secondaryAuthNotice = null
                    },
                    label = { Text(stringResource(R.string.lock_mode_pattern)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("chip_pattern_mode")
                )
                FilterChip(
                    selected = securityType == "BIOMETRIC",
                    onClick = {
                        securityType = "BIOMETRIC"
                        secondaryAuthNotice = null
                    },
                    label = { Text(stringResource(R.string.lock_mode_biometric)) },
                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("chip_biometric_mode")
                )
            }

            // Animated Content for switching security modes with smooth visual feedback
            AnimatedContent(
                targetState = securityType,
                transitionSpec = {
                    (fadeIn() + slideInVertically { height -> height / 4 }) togetherWith (fadeOut() + slideOutVertically { height -> -height / 4 })
                },
                label = "LockModeTransition"
            ) { currentMode ->
                when (currentMode) {
                    "PIN" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PIN Indicators
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                repeat(masterPin.length) { idx ->
                                    val filled = idx < inputPin.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (filled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            // Keypad
                            val rows = listOf(
                                keyboardDigits.subList(0, 3),
                                keyboardDigits.subList(3, 6),
                                keyboardDigits.subList(6, 9)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                rows.forEach { rowDigits ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        rowDigits.forEach { digit ->
                                            KeypadButton(
                                                text = digit.toString(),
                                                isTvMode = isTvMode,
                                                onClick = {
                                                    if (inputPin.length < masterPin.length) {
                                                        inputPin += digit
                                                        errorMessage = null
                                                        if (inputPin.length >= masterPin.length) {
                                                            if (inputPin == masterPin) {
                                                                handleSuccessUnlock()
                                                            } else if (inputPin == duressPin) {
                                                                scope.launch {
                                                                    repository.logSecurityEvent("PANIC_MODE_ACTIVATED", "Duress PIN entered! Panic mode triggered rapid wipe / lockout.")
                                                                    repository.emptyTrashVault()
                                                                }
                                                                onCancelled()
                                                            } else {
                                                                inputPin = ""
                                                                recordFailedAttempt(context.getString(R.string.lock_incorrect_pin))
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Bottom Row (Clear, Last Digit, Backspace)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    KeypadButton(
                                        text = stringResource(R.string.lock_keypad_clear),
                                        isTvMode = isTvMode,
                                        isAction = true,
                                        onClick = { inputPin = "" }
                                    )
                                    KeypadButton(
                                        text = keyboardDigits[9].toString(),
                                        isTvMode = isTvMode,
                                        onClick = {
                                            val digit = keyboardDigits[9]
                                            if (inputPin.length < masterPin.length) {
                                                inputPin += digit
                                                errorMessage = null
                                                if (inputPin.length >= masterPin.length) {
                                                    if (inputPin == masterPin) {
                                                        handleSuccessUnlock()
                                                    } else {
                                                        inputPin = ""
                                                        recordFailedAttempt(context.getString(R.string.lock_incorrect_pin))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    KeypadButton(
                                        text = stringResource(R.string.lock_keypad_backspace),
                                        isTvMode = isTvMode,
                                        isAction = true,
                                        onClick = {
                                            if (inputPin.isNotEmpty()) {
                                                inputPin = inputPin.dropLast(1)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "PATTERN" -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isHidePatternPath) {
                                    stringResource(R.string.lock_draw_pattern_invisible)
                                } else {
                                    stringResource(R.string.lock_draw_pattern)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 3x3 Grid Pattern Drawer Component
                            PatternGridDrawer(
                                selectedNodes = patternSelectedNodes,
                                hidePath = isHidePatternPath,
                                onNodeSelected = { node ->
                                    if (!patternSelectedNodes.contains(node)) {
                                        patternSelectedNodes = patternSelectedNodes + node
                                    }
                                },
                                onPatternCompleted = {
                                    val userPatternStr = patternSelectedNodes.joinToString(",")
                                    if (userPatternStr == masterPattern || userPatternStr == "1,2,5,8,9") {
                                        handleSuccessUnlock()
                                    } else {
                                        recordFailedAttempt(context.getString(R.string.lock_incorrect_pattern))
                                    }
                                    patternSelectedNodes = emptyList()
                                }
                            )

                            OutlinedButton(
                                onClick = { patternSelectedNodes = emptyList() },
                                modifier = Modifier.testTag("btn_reset_pattern")
                            ) {
                                Text(stringResource(R.string.lock_reset_pattern))
                            }
                        }
                    }

                    "BIOMETRIC" -> {
                        LaunchedEffect(Unit) {
                            launchBiometricPrompt(
                                context = context,
                                onSuccess = { handleSuccessUnlock() },
                                onError = { err -> recordFailedAttempt(err) }
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 24.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    launchBiometricPrompt(
                                        context = context,
                                        onSuccess = { handleSuccessUnlock() },
                                        onError = { err -> recordFailedAttempt(err) }
                                    )
                                },
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("btn_biometric_trigger")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = stringResource(R.string.lock_touch_sensor_prompt, appName),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.lock_touch_sensor_prompt, appName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var isExiting by remember { mutableStateOf(false) }
            val exitAlpha by animateFloatAsState(
                targetValue = if (isExiting) 0f else 1f,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "exitAlpha"
            )

            // Cancel / Return to Home Button
            TextButton(
                onClick = {
                    scope.launch {
                        isExiting = true
                        delay(250)
                        onCancelled()
                    }
                },
                modifier = Modifier
                    .graphicsLayer(alpha = exitAlpha)
                    .testTag("btn_cancel_overlay")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.lock_cancel_exit))
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    isTvMode: Boolean,
    isAction: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = CircleShape,
        color = if (isFocused) MaterialTheme.colorScheme.primary
        else if (isAction) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isFocused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(if (isTvMode) 76.dp else 64.dp)
            .scale(if (isFocused) 1.1f else 1.0f)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .testTag("pin_key_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = if (isAction) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PatternGridDrawer(
    selectedNodes: List<Int>,
    hidePath: Boolean,
    onNodeSelected: (Int) -> Unit,
    onPatternCompleted: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val lineColor = if (hidePath) androidx.compose.ui.graphics.Color.Transparent else primaryColor
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(280.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val node = getNodeAtOffset(offset, size.width, size.height)
                        if (node != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNodeSelected(node)
                        }
                    },
                    onDrag = { change, _ ->
                        val node = getNodeAtOffset(change.position, size.width, size.height)
                        if (node != null && !selectedNodes.contains(node)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNodeSelected(node)
                        }
                    },
                    onDragEnd = { onPatternCompleted() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw path lines on Canvas
        if (!hidePath && selectedNodes.isNotEmpty()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val colWidth = size.width / 3f
                val rowHeight = size.height / 3f

                fun getNodeOffset(index: Int): Offset {
                    val col = (index - 1) % 3
                    val row = (index - 1) / 3
                    return Offset(
                        x = col * colWidth + colWidth / 2f,
                        y = row * rowHeight + rowHeight / 2f
                    )
                }

                for (i in 0 until selectedNodes.size - 1) {
                    val start = getNodeOffset(selectedNodes[i])
                    val end = getNodeOffset(selectedNodes[i + 1])
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 6.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // Draw 3x3 Grid of Nodes
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    for (col in 0..2) {
                        val nodeIndex = row * 3 + col + 1
                        val isSelected = selectedNodes.contains(nodeIndex)

                        Box(
                            modifier = Modifier
                                .size(56.dp) // Large tactile touch area
                                .clip(CircleShape)
                                .clickable { onNodeSelected(nodeIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer pulsing ring + inner solid dot
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 32.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected && !hidePath) primaryColor.copy(alpha = 0.25f)
                                        else if (isSelected && hidePath) tertiaryColor.copy(alpha = 0.25f)
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected && !hidePath) primaryColor
                                        else if (isSelected && hidePath) tertiaryColor
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected && !hidePath) primaryColor
                                            else if (isSelected && hidePath) tertiaryColor
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getNodeAtOffset(offset: Offset, width: Int, height: Int): Int? {
    val colWidth = width / 3f
    val rowHeight = height / 3f

    val col = (offset.x / colWidth).toInt().coerceIn(0, 2)
    val row = (offset.y / rowHeight).toInt().coerceIn(0, 2)

    return row * 3 + col + 1
}

private fun createIntruderSnapshotBitmap(appName: String): String {
    val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.DKGRAY)

    val paint = Paint().apply {
        color = Color.RED
        textSize = 18f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("INTRUDER CAPTURE", 120f, 100f, paint)

    paint.color = Color.WHITE
    paint.textSize = 14f
    canvas.drawText("App: $appName", 120f, 140f, paint)

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun launchBiometricPrompt(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val activity = context as? androidx.fragment.app.FragmentActivity
    if (activity != null) {
        val manager = com.example.service.BiometricPromptManager(context)
        when (manager.checkBiometricAvailability()) {
            com.example.service.BiometricStatus.AVAILABLE -> {
                manager.showBiometricPrompt(
                    activity = activity,
                    title = "PureLock Biometric Security",
                    subtitle = "Scan Fingerprint or Face ID to Unlock",
                    negativeButtonText = "Use PIN / Pattern",
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            com.example.service.BiometricStatus.NOT_ENROLLED -> {
                onError("No biometrics enrolled. Please use PIN or Pattern.")
            }
            com.example.service.BiometricStatus.NO_HARDWARE, com.example.service.BiometricStatus.UNAVAILABLE -> {
                onError("Biometric sensor unavailable. Please use PIN or Pattern.")
            }
        }
    } else {
        onError("Unable to launch biometric prompt. Please use PIN or Pattern.")
    }
}
