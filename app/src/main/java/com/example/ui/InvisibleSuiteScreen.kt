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
    val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
    val gracePeriodMs by viewModel.gracePeriodMs.collectAsState()
    val duressPin by viewModel.duressPin.collectAsState()
    val randomKeyboard by viewModel.randomKeyboard.collectAsState()
    val hidePatternPath by viewModel.hidePatternPath.collectAsState()
    val intruderCapture by viewModel.intruderCapture.collectAsState()

    val context = LocalContext.current
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(masterPin) }

    var showChangePatternDialog by remember { mutableStateOf(false) }
    var newPatternInput by remember { mutableStateOf(masterPattern) }

    var showChangeKnockDialog by remember { mutableStateOf(false) }
    var newKnockList by remember { mutableStateOf(listOf<Int>()) }

    var showDuressPinDialog by remember { mutableStateOf(false) }
    var newDuressPinInput by remember { mutableStateOf(duressPin) }

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
                            onClick = { viewModel.setSecurityType("PATTERN") },
                            label = { Text(stringResource(R.string.lock_mode_pattern), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pattern, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_pattern")
                        )
                        FilterChip(
                            selected = securityType == "KNOCK",
                            onClick = { viewModel.setSecurityType("KNOCK") },
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
                                newPatternInput = masterPattern
                                showChangePatternDialog = true
                            }
                            "KNOCK" -> {
                                newKnockList = emptyList()
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
            }

            // Section 2: Camouflage & Stealth Decoys
            SettingsSectionCard(title = stringResource(R.string.suite_section_stealth)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_decoy_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.suite_decoy_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = decoyType == "NONE" && !stealthDecoy,
                            onClick = {
                                viewModel.setDecoyType("NONE")
                                viewModel.setStealthDecoy(false)
                            },
                            label = { Text(stringResource(R.string.suite_decoy_none), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_none")
                        )
                        FilterChip(
                            selected = decoyType == "CALCULATOR" || (stealthDecoy && decoyType == "NONE"),
                            onClick = {
                                viewModel.setDecoyType("CALCULATOR")
                                viewModel.setStealthDecoy(true)
                            },
                            label = { Text(stringResource(R.string.suite_decoy_calculator), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_calculator")
                        )
                        FilterChip(
                            selected = decoyType == "FAKE_CRASH",
                            onClick = {
                                viewModel.setDecoyType("FAKE_CRASH")
                                viewModel.setStealthDecoy(false)
                            },
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
                        "CYBER_MIDNIGHT" to "Midnight",
                        "DARK" to "OLED Black",
                        "EMERALD" to "Emerald",
                        "SAPPHIRE" to "Sapphire",
                        "MONET" to "Monet",
                        "LIGHT" to "Minimal"
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
                            text = if (inactivityTimeoutSec <= 0) "Immediate (Always)" else "${inactivityTimeoutSec}s Inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = inactivityTimeoutSec == 0,
                            onClick = { viewModel.setInactivityTimeout(0) },
                            label = { Text("0s", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = inactivityTimeoutSec == 30,
                            onClick = { viewModel.setInactivityTimeout(30) },
                            label = { Text("30s", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = inactivityTimeoutSec == 60,
                            onClick = { viewModel.setInactivityTimeout(60) },
                            label = { Text("60s", fontSize = 11.sp) }
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
                            label = { Text("0s", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 30_000L,
                            onClick = { viewModel.updateGracePeriodMs(30_000L) },
                            label = { Text("30s", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 60_000L,
                            onClick = { viewModel.updateGracePeriodMs(60_000L) },
                            label = { Text("1m", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gracePeriodMs == 300_000L,
                            onClick = { viewModel.updateGracePeriodMs(300_000L) },
                            label = { Text("5m", fontSize = 11.sp) },
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
                        text = "PureLock Security Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version ${com.example.BuildConfig.VERSION_NAME} • Hardware Keystore & SQLCipher AES-256",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "100% Offline • Zero Telemetry • Air-Gapped Privacy Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Info Popup Dialog (Details hidden behind hint icon)
    if (infoDialogTitle != null && infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogMessage = null
            },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(infoDialogTitle!!, fontWeight = FontWeight.Bold) },
            text = { Text(infoDialogMessage!!, style = MaterialTheme.typography.bodySmall) },
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_pin")
                    )
                }
            },
            confirmButton = {
                Button(
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

    // Change Pattern Dialog
    if (showChangePatternDialog) {
        AlertDialog(
            onDismissRequest = { showChangePatternDialog = false },
            title = { Text(stringResource(R.string.suite_set_master_pattern_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.suite_set_master_pattern_desc), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPatternInput,
                        onValueChange = { newPatternInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPatternInput.isNotBlank()) {
                            viewModel.setMasterPattern(newPatternInput)
                            showChangePatternDialog = false
                            Toast.makeText(context, context.getString(R.string.suite_master_pattern_updated_toast), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.suite_save_pattern_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePatternDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                TextButton(onClick = { showChangeKnockDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                        onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) newDuressPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDuressPin(newDuressPinInput)
                        showDuressPinDialog = false
                        Toast.makeText(context, context.getString(R.string.suite_duress_pin_configured_toast), Toast.LENGTH_SHORT).show()
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
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
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
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(15.dp))
                }
            }
        }

        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}
