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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                            text = "Security Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Stealth, Decoys & Authentication",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Section 1: Authentication Type & Credentials
            SettingsSectionCard(title = "Credentials & Lock Type") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lock Method", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = securityType == "PIN",
                            onClick = { viewModel.setSecurityType("PIN") },
                            label = { Text("PIN", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_pin")
                        )
                        FilterChip(
                            selected = securityType == "PATTERN",
                            onClick = { viewModel.setSecurityType("PATTERN") },
                            label = { Text("Pattern", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Pattern, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_pattern")
                        )
                        FilterChip(
                            selected = securityType == "KNOCK",
                            onClick = { viewModel.setSecurityType("KNOCK") },
                            label = { Text("Knock", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_knock")
                        )
                        FilterChip(
                            selected = securityType == "BIOMETRIC",
                            onClick = { viewModel.setSecurityType("BIOMETRIC") },
                            label = { Text("Bio", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_security_biometric")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Change Master Credential action button
                SettingsActionRow(
                    icon = when (securityType) {
                        "PATTERN" -> Icons.Default.Pattern
                        "KNOCK" -> Icons.Default.TouchApp
                        else -> Icons.Default.Pin
                    },
                    title = when (securityType) {
                        "PATTERN" -> "Change Master Pattern"
                        "KNOCK" -> "Configure Knock Code (4-Quadrant)"
                        else -> "Change Master PIN"
                    },
                    subtitle = when (securityType) {
                        "PATTERN" -> "Pattern set (${masterPattern.split(',').filter { it.isNotEmpty() }.size} dots)"
                        "KNOCK" -> "Knock rhythm (${masterKnock.split(',').filter { it.isNotEmpty() }.size} taps)"
                        else -> "Current: ••••"
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
                    SettingsSwitchRow(
                        icon = Icons.Default.Shuffle,
                        title = "Randomize Keypad",
                        checked = randomKeyboard,
                        onCheckedChange = { viewModel.setRandomKeyboard(it) },
                        onInfoClick = {
                            infoDialogTitle = "Randomize Keypad"
                            infoDialogMessage = "Shuffles number positions on every unlock attempt to prevent shoulder surfing and smudged screen footprint analysis."
                        },
                        testTag = "switch_random_keyboard"
                    )
                } else if (securityType == "PATTERN") {
                    SettingsSwitchRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Invisible Pattern Trail",
                        checked = hidePatternPath,
                        onCheckedChange = { viewModel.setHidePatternPath(it) },
                        onInfoClick = {
                            infoDialogTitle = "Invisible Pattern Trail"
                            infoDialogMessage = "Hides the connecting line while drawing your unlock pattern, preventing onlookers from seeing your unlock gesture."
                        },
                        testTag = "switch_hide_pattern"
                    )
                }
            }

            // Section 2: Camouflage & Stealth Decoys
            SettingsSectionCard(title = "Camouflage & Stealth Decoys") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lock Screen Decoy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Deceives snoopers by displaying a fake screen before the lock prompt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

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
                            label = { Text("None", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_none")
                        )
                        FilterChip(
                            selected = decoyType == "CALCULATOR" || (stealthDecoy && decoyType == "NONE"),
                            onClick = {
                                viewModel.setDecoyType("CALCULATOR")
                                viewModel.setStealthDecoy(true)
                            },
                            label = { Text("Calculator", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_calculator")
                        )
                        FilterChip(
                            selected = decoyType == "FAKE_CRASH",
                            onClick = {
                                viewModel.setDecoyType("FAKE_CRASH")
                                viewModel.setStealthDecoy(false)
                            },
                            label = { Text("Fake Crash", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier.weight(1f).testTag("chip_decoy_fake_crash")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Duress PIN
                SettingsActionRow(
                    icon = Icons.Default.GppBad,
                    title = "Duress PIN",
                    subtitle = if (duressPin.isNotBlank()) "Configured (••••)" else "Not configured",
                    onClick = {
                        newDuressPinInput = duressPin
                        showDuressPinDialog = true
                    },
                    onInfoClick = {
                        infoDialogTitle = "Duress PIN"
                        infoDialogMessage = "If forced to unlock under pressure, entering your Duress PIN will appear to unlock normally while secretly locking down sensitive items and alerting security logs."
                    },
                    testTag = "btn_duress_pin"
                )

                // Intruder Capture
                SettingsSwitchRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Intruder Selfie",
                    checked = intruderCapture,
                    onCheckedChange = { viewModel.setIntruderCapture(it) },
                    onInfoClick = {
                        infoDialogTitle = "Intruder Selfie"
                        infoDialogMessage = "Silently takes a front-facing camera photo when someone enters an incorrect PIN or pattern, stored in your encrypted Vault."
                    },
                    testTag = "switch_intruder_capture"
                )
            }

            // Section 3: Visual Theme & Security Palette
            SettingsSectionCard(title = "Visual Theme & Security Palette") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Color Palette", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

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
            SettingsSectionCard(title = "Triggers & Timeout") {
                // Shake to Lock
                SettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    title = "Shake to Lock",
                    checked = shakeToLockEnabled,
                    onCheckedChange = { viewModel.setShakeToLock(it) },
                    onInfoClick = {
                        infoDialogTitle = "Shake to Lock"
                        infoDialogMessage = "Instantly locks PureLock and clears active sessions when a physical shake gesture is detected."
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
                        Text("Auto-Lock Timeout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                        text = "Version 1.7 • Hardware Keystore & SQLCipher AES-256",
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
                    Text("Got It")
                }
            }
        )
    }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Set Master PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter 4-8 digit master security PIN:", style = MaterialTheme.typography.bodySmall)
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
                            Toast.makeText(context, "Master PIN updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Change Pattern Dialog
    if (showChangePatternDialog) {
        AlertDialog(
            onDismissRequest = { showChangePatternDialog = false },
            title = { Text("Set Master Pattern", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect dots (comma-separated 0-8, e.g. 0,1,2,5,8):", style = MaterialTheme.typography.bodySmall)
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
                            Toast.makeText(context, "Master pattern updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Pattern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePatternDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Knock Code Configuration Dialog
    if (showChangeKnockDialog) {
        AlertDialog(
            onDismissRequest = { showChangeKnockDialog = false },
            title = { Text("Configure Knock Code", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tap 4-quadrant area to record a rhythm (minimum 3 taps). Recorded: ${newKnockList.size} knocks",
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
                            Text("Clear (${newKnockList.joinToString("→")})", fontSize = 12.sp)
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
                            Toast.makeText(context, "Knock Code configured", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Tap at least 3 quadrants to form a pattern", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Knock Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeKnockDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Duress PIN Dialog
    if (showDuressPinDialog) {
        AlertDialog(
            onDismissRequest = { showDuressPinDialog = false },
            title = { Text("Set Duress PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a decoy PIN different from your master PIN:", style = MaterialTheme.typography.bodySmall)
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
                        Toast.makeText(context, "Duress PIN configured", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuressPinDialog = false }) { Text("Cancel") }
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
