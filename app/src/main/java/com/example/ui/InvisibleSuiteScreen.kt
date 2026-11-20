package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvisibleSuiteScreen(
    viewModel: PureLockViewModel
) {
    val masterPin by viewModel.masterPin.collectAsState()
    val masterPattern by viewModel.masterPattern.collectAsState()
    val masterKnock by viewModel.masterKnock.collectAsState()
    val securityType by viewModel.securityType.collectAsState()
    val stealthDecoy by viewModel.stealthDecoy.collectAsState()
    val decoyType by viewModel.decoyType.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val shakeToLockEnabled by viewModel.shakeToLockEnabled.collectAsState()
    val shakeSensitivity by viewModel.shakeSensitivity.collectAsState()
    val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
    val gracePeriodMs by viewModel.gracePeriodMs.collectAsState()
    val duressPin by viewModel.duressPin.collectAsState()
    val randomKeyboard by viewModel.randomKeyboard.collectAsState()
    val hidePatternPath by viewModel.hidePatternPath.collectAsState()
    val intruderCapture by viewModel.intruderCapture.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val clipboardAutoClearSec by viewModel.clipboardAutoClearSec.collectAsState()
    val donationEngagementState by viewModel.donationEngagementState.collectAsState()
    val donationBillingState by viewModel.donationBillingState.collectAsState()

    val context = LocalContext.current
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSupportDialog by remember { mutableStateOf(false) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(masterPin) }

    var showChangePatternDialog by remember { mutableStateOf(false) }
    var newPatternNodes by remember { mutableStateOf(listOf<Int>()) }
    var activatePatternAfterSave by remember { mutableStateOf(false) }

    var showChangeKnockDialog by remember { mutableStateOf(false) }
    var newKnockList by remember { mutableStateOf(listOf<Int>()) }
    var activateKnockAfterSave by remember { mutableStateOf(false) }

    var showDuressPinDialog by remember { mutableStateOf(false) }
    var newDuressPinInput by remember { mutableStateOf(duressPin) }
    var duressPinError by remember { mutableStateOf<String?>(null) }

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
                            text = stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.suite_header_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Section 1: Authentication Type & Credentials
            SettingsSectionCard(title = stringResource(R.string.suite_section_credentials)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_lock_method), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = securityType == "PIN",
                            onClick = { viewModel.setSecurityType("PIN") },
                            label = { Text(stringResource(R.string.lock_mode_pin), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_pin")
                        )
                        FilterChip(
                            selected = securityType == "PATTERN",
                            onClick = {
                                val parsed = masterPattern.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..9 }
                                if (parsed.size >= 4) {
                                    viewModel.setSecurityType("PATTERN")
                                } else {
                                    newPatternNodes = emptyList()
                                    activatePatternAfterSave = true
                                    showChangePatternDialog = true
                                }
                            },
                            label = { Text(stringResource(R.string.lock_mode_pattern), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pattern, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_pattern")
                        )
                        FilterChip(
                            selected = securityType == "KNOCK",
                            onClick = {
                                val parsed = masterKnock.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..4 }
                                if (parsed.size >= 3) {
                                    viewModel.setSecurityType("KNOCK")
                                } else {
                                    newKnockList = emptyList()
                                    activateKnockAfterSave = true
                                    showChangeKnockDialog = true
                                }
                            },
                            label = { Text(stringResource(R.string.lock_mode_knock), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_knock")
                        )
                        FilterChip(
                            selected = securityType == "BIOMETRIC",
                            onClick = { viewModel.setSecurityType("BIOMETRIC") },
                            label = { Text(stringResource(R.string.lock_mode_biometric), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_biometric")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Change Master Credential action button
                val dotsCount = masterPattern.split(',').filter { it.isNotEmpty() }.size
                val tapsCount = masterKnock.split(',').filter { it.isNotEmpty() }.size
                SettingsActionRow(
                    icon = when (securityType) {
                        "PATTERN" -> Icons.Default.Pattern
                        "KNOCK" -> Icons.Default.TouchApp
                        else -> Icons.Default.Pin
                    },
                    title = when (securityType) {
                        "PATTERN" -> stringResource(R.string.suite_change_pattern_title)
                        "KNOCK" -> stringResource(R.string.suite_change_knock_title)
                        else -> stringResource(R.string.suite_change_pin_title)
                    },
                    subtitle = when (securityType) {
                        "PATTERN" -> stringResource(R.string.suite_pattern_dots_subtitle, dotsCount)
                        "KNOCK" -> stringResource(R.string.suite_knock_taps_subtitle, tapsCount)
                        else -> stringResource(R.string.suite_pin_current_subtitle)
                    },
                    onClick = {
                        when (securityType) {
                            "PATTERN" -> {
                                newPatternNodes = masterPattern.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..9 }
                                activatePatternAfterSave = false
                                showChangePatternDialog = true
                            }
                            "KNOCK" -> {
                                newKnockList = emptyList()
                                activateKnockAfterSave = false
                                showChangeKnockDialog = true
                            }
                            else -> {
                                newPinInput = masterPin
                                showChangePinDialog = true
                            }
                        }
                    },
                    testTag = "btn_change_credential"
                )

                // Scramble Keypad / Hide Pattern Path
                if (securityType == "PIN") {
                    val rndTitle = stringResource(R.string.suite_random_keypad_title)
                    val rndDesc = stringResource(R.string.suite_random_keypad_dialog_desc)
                    SettingsSwitchRow(
                        icon = Icons.Default.Shuffle,
                        title = rndTitle,
                        checked = randomKeyboard,
                        onCheckedChange = { viewModel.setRandomKeyboard(it) },
                        onInfoClick = {
                            infoDialogTitle = rndTitle
                            infoDialogMessage = rndDesc
                        },
                        testTag = "switch_random_keyboard"
                    )
                } else if (securityType == "PATTERN") {
                    val invTitle = stringResource(R.string.suite_invisible_pattern_title)
                    val invDesc = stringResource(R.string.suite_invisible_pattern_dialog_desc)
                    SettingsSwitchRow(
                        icon = Icons.Default.VisibilityOff,
                        title = invTitle,
                        checked = hidePatternPath,
                        onCheckedChange = { viewModel.setHidePatternPath(it) },
                        onInfoClick = {
                            infoDialogTitle = invTitle
                            infoDialogMessage = invDesc
                        },
                        testTag = "switch_hide_pattern"
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Biometric Quick Unlock toggle
                val bioTitle = stringResource(R.string.onboarding_biometrics_title)
                val bioDesc = stringResource(R.string.onboarding_biometrics_desc)
                SettingsSwitchRow(
                    icon = Icons.Default.Fingerprint,
                    title = bioTitle,
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                    onInfoClick = {
                        infoDialogTitle = bioTitle
                        infoDialogMessage = bioDesc
                    },
                    testTag = "switch_biometric_enabled"
                )
            }

            // Section 2: Camouflage & Stealth Decoys
            SettingsSectionCard(title = stringResource(R.string.suite_section_stealth)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_decoy_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.suite_decoy_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    val activeDecoy = when {
                        decoyType == "FAKE_CRASH" -> "FAKE_CRASH"
                        decoyType == "CALCULATOR" || (stealthDecoy && decoyType == "NONE") -> "CALCULATOR"
                        else -> "NONE"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = activeDecoy == "NONE",
                            onClick = { viewModel.setDecoyType("NONE") },
                            label = { Text(stringResource(R.string.suite_decoy_none), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_none")
                        )
                        FilterChip(
                            selected = activeDecoy == "CALCULATOR",
                            onClick = { viewModel.setDecoyType("CALCULATOR") },
                            label = { Text(stringResource(R.string.suite_decoy_calculator), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_calculator")
                        )
                        FilterChip(
                            selected = activeDecoy == "FAKE_CRASH",
                            onClick = { viewModel.setDecoyType("FAKE_CRASH") },
                            label = { Text(stringResource(R.string.suite_decoy_fake_crash), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_fake_crash")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Duress PIN
                val duressTitle = stringResource(R.string.suite_duress_title)
                val duressDesc = stringResource(R.string.suite_duress_dialog_desc)
                SettingsActionRow(
                    icon = Icons.Default.GppBad,
                    title = duressTitle,
                    subtitle = if (duressPin.isNotBlank()) stringResource(R.string.suite_pin_current_subtitle) else stringResource(R.string.perm_status_inactive),
                    onClick = {
                        newDuressPinInput = duressPin
                        duressPinError = null
                        showDuressPinDialog = true
                    },
                    onInfoClick = {
                        infoDialogTitle = duressTitle
                        infoDialogMessage = duressDesc
                    },
                    testTag = "btn_duress_pin"
                )

                // Intruder Capture
                val intruderTitle = stringResource(R.string.suite_intruder_selfie_title)
                val intruderDesc = stringResource(R.string.suite_intruder_selfie_dialog_desc)
                SettingsSwitchRow(
                    icon = Icons.Default.CameraAlt,
                    title = intruderTitle,
                    checked = intruderCapture,
                    onCheckedChange = { viewModel.setIntruderCapture(it) },
                    onInfoClick = {
                        infoDialogTitle = intruderTitle
                        infoDialogMessage = intruderDesc
                    },
                    testTag = "switch_intruder_capture"
                )
            }

            // Section 3: Visual Theme & Security Palette
            SettingsSectionCard(title = stringResource(R.string.suite_section_theme)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_palette_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                    val themes = listOf(
                        "CYBER_MIDNIGHT" to stringResource(R.string.theme_midnight),
                        "DARK" to stringResource(R.string.theme_oled_black),
                        "EMERALD" to stringResource(R.string.theme_emerald),
                        "SAPPHIRE" to stringResource(R.string.theme_sapphire),
                        "MONET" to stringResource(R.string.theme_monet),
                        "LIGHT" to stringResource(R.string.theme_minimal)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        themes.take(3).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        themes.drop(3).forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 4: Relock & Emergency Triggers
            SettingsSectionCard(title = stringResource(R.string.suite_section_triggers)) {
                // Shake to Lock
                val shakeTitle = stringResource(R.string.suite_shake_title)
                val shakeDesc = stringResource(R.string.suite_shake_dialog_desc)
                val hasAccelerometer = remember(context) {
                    com.example.service.ShakeToLockSensorController.isAccelerometerAvailable(context)
                }
                SettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    title = shakeTitle,
                    checked = shakeToLockEnabled,
                    onCheckedChange = { viewModel.setShakeToLock(it) },
                    onInfoClick = {
                        infoDialogTitle = shakeTitle
                        infoDialogMessage = shakeDesc
                    },
                    testTag = "switch_shake_lock"
                )

                AnimatedVisibility(
                    visible = shakeToLockEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.shake_to_lock_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (hasAccelerometer) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("shake_sensor_status_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasAccelerometer) Icons.Default.Sensors else Icons.Default.SensorsOff,
                                        contentDescription = null,
                                        tint = if (hasAccelerometer) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (hasAccelerometer) {
                                            stringResource(R.string.perm_status_active)
                                        } else {
                                            stringResource(R.string.perm_status_inactive)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasAccelerometer) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                            }
                        }

                        val activeSensitivity = com.example.service.ShakeSensitivity.fromKey(shakeSensitivity)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(com.example.service.ShakeSensitivity.HIGH, Icons.Default.Bolt, "chip_shake_sensitivity_high"),
                                Triple(com.example.service.ShakeSensitivity.MEDIUM, Icons.Default.Tune, "chip_shake_sensitivity_medium"),
                                Triple(com.example.service.ShakeSensitivity.LOW, Icons.Default.Shield, "chip_shake_sensitivity_low")
                            ).forEach { (preset, presetIcon, tag) ->
                                FilterChip(
                                    selected = activeSensitivity == preset,
                                    onClick = { viewModel.setShakeSensitivity(preset.key) },
                                    label = {
                                        Text(
                                            text = preset.badgeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (activeSensitivity == preset) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = presetIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tag)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Inactivity Timeout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.suite_autolock_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (inactivityTimeoutSec <= 0) stringResource(R.string.suite_timeout_immediate) else stringResource(R.string.suite_timeout_seconds_format, inactivityTimeoutSec),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = inactivityTimeoutSec == 0,
                            onClick = { viewModel.setInactivityTimeout(0) },
                            label = { Text(stringResource(R.string.duration_sec_short, 0), fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = inactivityTimeoutSec == 30,
                            onClick = { viewModel.setInactivityTimeout(30) },
                            label = { Text(stringResource(R.string.duration_sec_short, 30), fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = inactivityTimeoutSec == 60,
                            onClick = { viewModel.setInactivityTimeout(60) },
                            label = { Text(stringResource(R.string.duration_sec_short, 60), fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // App Relock Grace Period
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.suite_grace_period_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.suite_grace_period_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        val graceTitle = stringResource(R.string.suite_grace_period_title)
                        val graceDesc = stringResource(R.string.suite_grace_period_dialog_desc)
                        IconButton(
                            onClick = {
                                infoDialogTitle = graceTitle
                                infoDialogMessage = graceDesc
                            }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = gracePeriodMs <= 0L && gracePeriodMs != -1L,
                            onClick = { viewModel.updateGracePeriodMs(0L) },
                            label = { Text(stringResource(R.string.duration_sec_short, 0), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 30_000L,
                            onClick = { viewModel.updateGracePeriodMs(30_000L) },
                            label = { Text(stringResource(R.string.duration_sec_short, 30), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 60_000L,
                            onClick = { viewModel.updateGracePeriodMs(60_000L) },
                            label = { Text(stringResource(R.string.duration_min_short, 1), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 300_000L,
                            onClick = { viewModel.updateGracePeriodMs(300_000L) },
                            label = { Text(stringResource(R.string.duration_min_short, 5), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == -1L,
                            onClick = { viewModel.updateGracePeriodMs(-1L) },
                            label = { Text(stringResource(R.string.grace_period_screen_off), fontSize = 10.sp) },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Clipboard Auto-Clear Delay
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.clipboard_clear_delay_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (clipboardAutoClearSec <= 0) {
                                    stringResource(R.string.clipboard_delay_disabled)
                                } else {
                                    stringResource(R.string.suite_clipboard_autoclear_format, clipboardAutoClearSec)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        val clipTitle = stringResource(R.string.clipboard_clear_delay_title)
                        val clipDesc = stringResource(R.string.clipboard_clear_delay_desc)
                        IconButton(
                            onClick = {
                                infoDialogTitle = clipTitle
                                infoDialogMessage = clipDesc
                            }
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.details),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            10 to stringResource(R.string.duration_sec_short, 10),
                            30 to stringResource(R.string.duration_sec_short, 30),
                            60 to stringResource(R.string.duration_sec_short, 60),
                            0 to stringResource(R.string.suite_decoy_none)
                        ).forEach { (sec, label) ->
                            FilterChip(
                                selected = clipboardAutoClearSec == sec,
                                onClick = { viewModel.setClipboardAutoClearSec(sec) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chip_clipboard_clear_$sec")
                            )
                        }
                    }
                }
            }

            // Static Permanent Support Development Section (Settings / About)
            SettingsSectionCard(title = stringResource(R.string.settings_support_section_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (donationEngagementState.isActiveSupporter) Icons.Default.Verified else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (donationEngagementState.isActiveSupporter) {
                                        stringResource(R.string.donation_supporter_active_title)
                                    } else {
                                        stringResource(R.string.settings_support_card_title)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (donationEngagementState.isActiveSupporter) {
                                        stringResource(R.string.donation_supporter_active_desc)
                                    } else {
                                        stringResource(R.string.settings_support_card_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (donationEngagementState.isActiveSupporter) {
                        OutlinedButton(
                            onClick = { showSupportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_settings_support")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_support_cta))
                        }
                    } else {
                        Button(
                            onClick = { showSupportDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_settings_support")
                        ) {
                            Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_support_cta))
                        }
                    }
                }
            }

            // PureLock Security Architecture & Brand Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PureLockLogoEmblem(
                        size = 56.dp,
                        showGlowRing = true,
                        elevation = 8.dp
                    )

                    Text(
                        text = stringResource(R.string.suite_architecture_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.suite_architecture_version_format, com.example.BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(R.string.suite_architecture_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Info Popup Dialog (Details hidden behind hint icon)
    val currentInfoTitle = infoDialogTitle
    val currentInfoMessage = infoDialogMessage
    if (currentInfoTitle != null && currentInfoMessage != null) {
        AlertDialog(
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogMessage = null
            },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(currentInfoTitle, fontWeight = FontWeight.Bold) },
            text = { Text(currentInfoMessage, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = {
                    infoDialogTitle = null
                    infoDialogMessage = null
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text(stringResource(R.string.suite_set_master_pin_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_set_master_pin_desc), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) newPinInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPinInput.length >= 4,
                    onClick = {
                        if (newPinInput.length >= 4) {
                            viewModel.setMasterPin(newPinInput)
                            showChangePinDialog = false
                            Toast.makeText(context, context.getString(R.string.suite_master_pin_updated_toast), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.suite_save_pin_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Change Pattern Dialog (Interactive 3x3 Grid to prevent invalid pattern lockouts)
    if (showChangePatternDialog) {
        AlertDialog(
            onDismissRequest = { showChangePatternDialog = false },
            title = { Text(stringResource(R.string.suite_set_master_pattern_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (newPatternNodes.isEmpty()) {
                            stringResource(R.string.onboarding_draw_pattern_hint)
                        } else {
                            stringResource(R.string.onboarding_selected_nodes_format, newPatternNodes.joinToString("-"))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    PatternGridDrawer(
                        selectedNodes = newPatternNodes,
                        hidePath = false,
                        onNodeSelected = { node ->
                            if (!newPatternNodes.contains(node)) {
                                newPatternNodes = newPatternNodes + node
                            }
                        },
                        onPatternCompleted = {}
                    )
                    if (newPatternNodes.isNotEmpty()) {
                        TextButton(onClick = { newPatternNodes = emptyList() }) {
                            Text(stringResource(R.string.onboarding_reset_pattern))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = newPatternNodes.size >= 4,
                    onClick = {
                        if (newPatternNodes.size >= 4) {
                            viewModel.setMasterPattern(newPatternNodes.joinToString(","))
                            if (activatePatternAfterSave) {
                                viewModel.setSecurityType("PATTERN")
                                activatePatternAfterSave = false
                            }
                            showChangePatternDialog = false
                            Toast.makeText(context, context.getString(R.string.suite_master_pattern_updated_toast), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.onboarding_pattern_min_nodes_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.suite_save_pattern_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activatePatternAfterSave = false
                    showChangePatternDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Knock Code Configuration Dialog
    if (showChangeKnockDialog) {
        AlertDialog(
            onDismissRequest = { showChangeKnockDialog = false },
            title = { Text(stringResource(R.string.suite_configure_knock_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.suite_configure_knock_desc, newKnockList.size),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(180.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { newKnockList = newKnockList + 1 }
                                        .testTag("setup_knock_1"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("I", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { newKnockList = newKnockList + 2 }
                                        .testTag("setup_knock_2"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("II", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Row(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { newKnockList = newKnockList + 3 }
                                        .testTag("setup_knock_3"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("III", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { newKnockList = newKnockList + 4 }
                                        .testTag("setup_knock_4"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("IV", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    if (newKnockList.isNotEmpty()) {
                        TextButton(onClick = { newKnockList = emptyList() }) {
                            Text("${stringResource(R.string.clear)} (${newKnockList.joinToString("→")})", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKnockList.size >= 3) {
                            val codeStr = newKnockList.joinToString(",")
                            viewModel.setMasterKnock(codeStr)
                            if (activateKnockAfterSave) {
                                viewModel.setSecurityType("KNOCK")
                                activateKnockAfterSave = false
                            }
                            showChangeKnockDialog = false
                            Toast.makeText(context, context.getString(R.string.suite_knock_configured_toast), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.suite_knock_min_error_toast), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.suite_save_knock_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activateKnockAfterSave = false
                    showChangeKnockDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Duress PIN Dialog
    if (showDuressPinDialog) {
        AlertDialog(
            onDismissRequest = { showDuressPinDialog = false },
            title = { Text(stringResource(R.string.suite_set_duress_pin_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_set_duress_pin_desc), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newDuressPinInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                                newDuressPinInput = it
                                duressPinError = null
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        isError = duressPinError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_duress_pin")
                    )
                    if (duressPinError != null) {
                        Text(
                            text = duressPinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newDuressPinInput.isNotEmpty() && newDuressPinInput.length < 4 -> {
                                duressPinError = context.getString(R.string.onboarding_pin_min_length_error)
                            }
                            newDuressPinInput.isNotEmpty() && newDuressPinInput == masterPin -> {
                                duressPinError = context.getString(R.string.suite_set_duress_pin_desc)
                            }
                            else -> {
                                viewModel.setDuressPin(newDuressPinInput)
                                showDuressPinDialog = false
                                Toast.makeText(context, context.getString(R.string.suite_duress_pin_configured_toast), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuressPinDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showSupportDialog) {
        SupportDevelopmentDialog(
            engagementState = donationEngagementState,
            billingState = donationBillingState,
            onLaunchGooglePlay = { launchContext, tier ->
                viewModel.launchGooglePlayDonation(launchContext, tier)
            },
            onClearBillingStatus = { viewModel.clearDonationBillingStatus() },
            onDismiss = { showSupportDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null,
    testTag: String = ""
) {
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
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.details), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Info, contentDescription = stringResource(R.string.details), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                }
            }
        }

        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}
