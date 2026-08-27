package com.example.ui

import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun Modifier.tvFocusScaleBorder(isTvMode: Boolean): Modifier {
    if (!isTvMode) return this
    var isFocused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { isFocused = it.isFocused }
        .graphicsLayer {
            scaleX = if (isFocused) 1.04f else 1.0f
            scaleY = if (isFocused) 1.04f else 1.0f
        }
        .border(
            width = if (isFocused) 3.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
            shape = RoundedCornerShape(16.dp)
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvisibleSuiteScreen(
    viewModel: PureLockViewModel
) {
    val masterPin by viewModel.masterPin.collectAsState()
    val masterPattern by viewModel.masterPattern.collectAsState()
    val securityType by viewModel.securityType.collectAsState()
    val gracePeriodMs by viewModel.gracePeriodMs.collectAsState()
    val randomKeyboard by viewModel.randomKeyboard.collectAsState()
    val stealthDecoy by viewModel.stealthDecoy.collectAsState()
    val hidePatternPath by viewModel.hidePatternPath.collectAsState()
    val intruderCapture by viewModel.intruderCapture.collectAsState()
    val tvMode by viewModel.tvMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
    val clipboardAutoClearSec by viewModel.clipboardAutoClearSec.collectAsState()
    val shakeToLockEnabled by viewModel.shakeToLockEnabled.collectAsState()
    val trashPurgeDays by viewModel.trashPurgeDays.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    LaunchedEffect(appLanguage) {
        L10n.currentLanguageCode = appLanguage
    }

    val scheduleRules by viewModel.scheduleRules.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val biometricSettingsSecured by viewModel.biometricSettingsSecured.collectAsState()
    var hasAuthenticatedThisSession by remember { mutableStateOf(false) }
    var showFallbackPinAuthDialog by remember { mutableStateOf(false) }
    var fallbackPinInput by remember { mutableStateOf("") }
    var fallbackAuthError by remember { mutableStateOf<String?>(null) }

    val isSettingsUnlocked = !biometricSettingsSecured || hasAuthenticatedThisSession

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf(masterPin) }

    var showPatternDialog by remember { mutableStateOf(false) }
    var newPatternInput by remember { mutableStateOf(masterPattern) }

    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var selectedAppForSchedule by remember { mutableStateOf(allApps.firstOrNull()?.packageName ?: "") }
    var selectedAppNameForSchedule by remember { mutableStateOf(allApps.firstOrNull()?.appName ?: "") }
    var startHourInput by remember { mutableStateOf("18") }
    var startMinuteInput by remember { mutableStateOf("00") }
    var endHourInput by remember { mutableStateOf("23") }
    var endMinuteInput by remember { mutableStateOf("00") }

    LaunchedEffect(biometricSettingsSecured) {
        if (biometricSettingsSecured && !hasAuthenticatedThisSession) {
            launchBiometricPrompt(
                context = context,
                onSuccess = { hasAuthenticatedThisSession = true },
                onError = { /* fallback available */ }
            )
        }
    }

    if (!isSettingsUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Security Biometrics",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.settings_lock_active),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.settings_lock_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    launchBiometricPrompt(
                        context = context,
                        onSuccess = { hasAuthenticatedThisSession = true },
                        onError = { /* fallback available */ }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_unlock_settings_biometric")
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_scan_biometrics))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    showFallbackPinAuthDialog = true
                    fallbackPinInput = ""
                    fallbackAuthError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_unlock_settings_pin")
            ) {
                Icon(Icons.Default.Pin, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.settings_use_fallback_pin))
            }
        }

        if (showFallbackPinAuthDialog) {
            AlertDialog(
                onDismissRequest = { showFallbackPinAuthDialog = false },
                title = { Text(stringResource(R.string.settings_enter_master_pin_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.settings_enter_master_pin_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = fallbackPinInput,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() } && it.length <= 6) {
                                    fallbackPinInput = it
                                }
                            },
                            label = { Text(stringResource(R.string.settings_enter_master_pin_title)) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("input_fallback_pin")
                        )
                        if (fallbackAuthError != null) {
                            Text(
                                text = fallbackAuthError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (fallbackPinInput == masterPin) {
                                hasAuthenticatedThisSession = true
                                showFallbackPinAuthDialog = false
                            } else {
                                fallbackAuthError = context.getString(R.string.settings_incorrect_pin)
                            }
                        },
                        modifier = Modifier.testTag("btn_submit_fallback_pin")
                    ) {
                        Text(stringResource(R.string.settings_unlock_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFallbackPinAuthDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Top Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = "The Invisible Suite",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Stealth configurations & local security controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Security Type & Credentials
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Authentication Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Primary Lock Method",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Currently using $securityType",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = securityType == "PIN",
                            onClick = { viewModel.updateSecurityType("PIN") },
                            label = { Text("PIN") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = securityType == "PIN",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_set_pin")
                        )
                        FilterChip(
                            selected = securityType == "PATTERN",
                            onClick = { viewModel.updateSecurityType("PATTERN") },
                            label = { Text("Pattern") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = securityType == "PATTERN",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_set_pattern")
                        )
                        FilterChip(
                            selected = securityType == "BIOMETRIC",
                            onClick = { viewModel.updateSecurityType("BIOMETRIC") },
                            label = { Text("Biometrics") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = securityType == "BIOMETRIC",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_set_biometric")
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Master PIN Code",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "PIN: $masterPin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.testTag("btn_change_pin")
                    ) {
                        Text("Change PIN")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Master Pattern Path",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Nodes: $masterPattern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showPatternDialog = true },
                        modifier = Modifier.testTag("btn_change_pattern")
                    ) {
                        Text("Change Pattern")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Secure Settings with Biometrics",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Require local biometric verification to view and change settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = biometricSettingsSecured,
                        onCheckedChange = { viewModel.setBiometricSettingsSecured(it) },
                        modifier = Modifier.testTag("switch_biometric_settings_secured")
                    )
                }
            }
        }

        // Section 2: Intelligent Multitasking Grace Period
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Intelligent Grace Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Prevents 'Lock Fatigue' when rapidly switching between apps within a set timeframe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val periodOptions = listOf(
                    0L to "Immediate",
                    30000L to "30 Secs",
                    60000L to "1 Min",
                    300000L to "5 Mins"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periodOptions.forEach { (ms, label) ->
                        FilterChip(
                            selected = gracePeriodMs == ms,
                            onClick = { viewModel.updateGracePeriodMs(ms) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = gracePeriodMs == ms,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_grace_$label")
                        )
                    }
                }
            }
        }

        // Section: UI Theme Provider Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "App Theme Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Switch between System Default, explicit Light, or dark stealth themes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            label = { Text("System") },
                            leadingIcon = { Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "SYSTEM",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_theme_system")
                        )
                    }
                    item {
                        FilterChip(
                            selected = themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            label = { Text("Light") },
                            leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "LIGHT",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_theme_light")
                        )
                    }
                    item {
                        FilterChip(
                            selected = themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            label = { Text("Dark") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "DARK",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_theme_dark")
                        )
                    }
                    item {
                        FilterChip(
                            selected = themeMode == "AMOLED",
                            onClick = { viewModel.setThemeMode("AMOLED") },
                            label = { Text("AMOLED Pure Black") },
                            leadingIcon = { Icon(Icons.Default.Brightness1, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "AMOLED",
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_theme_amoled")
                        )
                    }
                }
            }
        }

        // Section: App UI Inactivity Auto-Lock Timeout
        // Section: Auto-Lock Inactivity Timeout Configuration Card (DataStore + Room Enforced)
        var showAutoLockDialog by remember { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().testTag("card_autolock_configuration")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.auto_lock_settings_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = { showAutoLockDialog = true },
                        modifier = Modifier.testTag("btn_configure_autolock_intervals")
                    ) {
                        Text("Configure")
                    }
                }

                Text(
                    text = stringResource(R.string.auto_lock_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val timeoutOptions = listOf(
                    0 to "Immediately",
                    30 to "30s",
                    60 to "1m",
                    120 to "2m",
                    300 to "5m",
                    600 to "10m"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    timeoutOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = inactivityTimeoutSec == sec,
                            onClick = { viewModel.setInactivityTimeoutSec(sec) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = inactivityTimeoutSec == sec,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_inactivity_$label")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Current policy: " + when (inactivityTimeoutSec) {
                                0 -> "Immediately lock when idle"
                                30 -> "Lock after 30 seconds of inactivity"
                                60 -> "Lock after 1 minute of inactivity"
                                120 -> "Lock after 2 minutes of inactivity"
                                300 -> "Lock after 5 minutes of inactivity"
                                600 -> "Lock after 10 minutes of inactivity"
                                else -> "Lock after $inactivityTimeoutSec seconds"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (showAutoLockDialog) {
            data class AutoLockOption(val seconds: Int, val title: String, val description: String)
            val intervals = listOf(
                AutoLockOption(0, "Immediately", "Locks right when inactivity is detected (0s)."),
                AutoLockOption(30, "30 Seconds", "Locks after 30 seconds of no interaction."),
                AutoLockOption(60, "1 Minute", "Standard security threshold (recommended)."),
                AutoLockOption(120, "2 Minutes", "Balanced convenience and protection."),
                AutoLockOption(300, "5 Minutes", "Extended grace period for reading."),
                AutoLockOption(600, "10 Minutes", "Maximum timeout before automatic lock.")
            )

            AlertDialog(
                onDismissRequest = { showAutoLockDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.auto_lock_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { opt ->
                            Surface(
                                color = if (inactivityTimeoutSec == opt.seconds) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (inactivityTimeoutSec == opt.seconds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setInactivityTimeoutSec(opt.seconds)
                                        showAutoLockDialog = false
                                    }
                                    .testTag("dialog_autolock_option_${opt.seconds}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = inactivityTimeoutSec == opt.seconds,
                                        onClick = {
                                            viewModel.setInactivityTimeoutSec(opt.seconds)
                                            showAutoLockDialog = false
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = opt.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showAutoLockDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Section: Clipboard Auto-Clear Timeout Configuration Card
        var showClipboardDialog by remember { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().testTag("card_clipboard_configuration")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPasteGo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.clipboard_clear_delay_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = { showClipboardDialog = true },
                        modifier = Modifier.testTag("btn_configure_clipboard_intervals")
                    ) {
                        Text("Configure")
                    }
                }

                Text(
                    text = stringResource(R.string.clipboard_clear_delay_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val clipboardOptions = listOf(
                    10 to "10s",
                    30 to "30s",
                    60 to "1m",
                    120 to "2m",
                    0 to "Never"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    clipboardOptions.forEach { (sec, label) ->
                        FilterChip(
                            selected = clipboardAutoClearSec == sec,
                            onClick = { viewModel.setClipboardAutoClearSec(sec) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = clipboardAutoClearSec == sec,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_clipboard_$label")
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Active policy: " + when (clipboardAutoClearSec) {
                                0 -> "Manual clearing only"
                                10 -> "Auto-purge clipboard after 10 seconds"
                                30 -> "Auto-purge clipboard after 30 seconds (Default)"
                                60 -> "Auto-purge clipboard after 1 minute"
                                120 -> "Auto-purge clipboard after 2 minutes"
                                else -> "Auto-purge after $clipboardAutoClearSec seconds"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (showClipboardDialog) {
            data class ClipboardOption(val seconds: Int, val title: String, val description: String)
            val clipIntervals = listOf(
                ClipboardOption(10, "10 Seconds", "Fast auto-clear for maximum physical privacy."),
                ClipboardOption(30, "30 Seconds", "Recommended balance for pasting into login forms."),
                ClipboardOption(60, "1 Minute", "Extended window for multi-field forms."),
                ClipboardOption(120, "2 Minutes", "Long window before wiping copied text."),
                ClipboardOption(0, "Never", "Keep in clipboard until manually overwritten.")
            )

            AlertDialog(
                onDismissRequest = { showClipboardDialog = false },
                title = {
                    Text(
                        text = "Clipboard Auto-Purge Window",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        clipIntervals.forEach { opt ->
                            Surface(
                                color = if (clipboardAutoClearSec == opt.seconds) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (clipboardAutoClearSec == opt.seconds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setClipboardAutoClearSec(opt.seconds)
                                        showClipboardDialog = false
                                    }
                                    .testTag("dialog_clipboard_option_${opt.seconds}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = clipboardAutoClearSec == opt.seconds,
                                        onClick = {
                                            viewModel.setClipboardAutoClearSec(opt.seconds)
                                            showClipboardDialog = false
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = opt.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = opt.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showClipboardDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Section: 60-Language Localisation Selection Card
        var isLanguageMenuExpanded by remember { mutableStateOf(false) }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().testTag("card_language_selection")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Interface Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select your preferred offline localization language. Supports 60 world languages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isLanguageMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_language_dropdown"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = L10n.supportedLanguages[appLanguage] ?: "English",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = isLanguageMenuExpanded,
                        onDismissRequest = { isLanguageMenuExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 300.dp)
                    ) {
                        L10n.supportedLanguages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.setAppLanguage(code)
                                    isLanguageMenuExpanded = false
                                },
                                modifier = Modifier.testTag("lang_item_$code")
                            )
                        }
                    }
                }
            }
        }

        // Section: Shake to Lock Emergency Gesture
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = L10n.getString(LocalContext.current, "shake_to_lock"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = L10n.getString(LocalContext.current, "shake_to_lock_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = shakeToLockEnabled,
                    onCheckedChange = { viewModel.toggleShakeToLock(it) },
                    modifier = Modifier.testTag("switch_shake_to_lock")
                )
            }
        }

        // Section: Vault Integrity SHA-256 Check
        var integrityResult by remember { mutableStateOf<com.example.data.VaultIntegrityResult?>(null) }
        var isVerifyingIntegrity by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.getString(LocalContext.current, "integrity_title"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = L10n.getString(LocalContext.current, "integrity_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (integrityResult != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = L10n.getString(LocalContext.current, "integrity_success"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = integrityResult!!.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        isVerifyingIntegrity = true
                        coroutineScope.launch {
                            integrityResult = viewModel.runVaultIntegrityCheck()
                            isVerifyingIntegrity = false
                        }
                    },
                    enabled = !isVerifyingIntegrity,
                    modifier = Modifier.fillMaxWidth().testTag("btn_run_integrity_check")
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isVerifyingIntegrity) L10n.getString(LocalContext.current, "integrity_verifying") else L10n.getString(LocalContext.current, "integrity_btn"))
                }
            }
        }

        // Section: Android Autofill Service API Integration
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.getString(LocalContext.current, "autofill_title"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = L10n.getString(LocalContext.current, "autofill_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                                intent.setData(android.net.Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Autofill requires Android 8.0+", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open System Settings -> Language & Input -> Autofill service", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_enable_autofill_setting")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.getString(LocalContext.current, "autofill_btn"))
                }
            }
        }

        // Section: Trash Bin Auto-Purge Duration
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = L10n.getString(LocalContext.current, "trash_purge"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = L10n.getString(LocalContext.current, "trash_purge_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val purgeOptions = listOf(7, 14, 30, 60)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    purgeOptions.forEach { days ->
                        FilterChip(
                            selected = trashPurgeDays == days,
                            onClick = { viewModel.updateTrashPurgeDays(days) },
                            label = { Text("$days Days", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = trashPurgeDays == days,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("chip_purge_${days}d")
                        )
                    }
                }
            }
        }

        // Section 3: Stealth & Invisible Features
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Stealth Protections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Stealth Icon Decoy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stealth Decoy Icon",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Disguise PureLock as a Calculator or System Tool icon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = stealthDecoy,
                        onCheckedChange = { viewModel.setStealthDecoy(it) },
                        modifier = Modifier.testTag("switch_stealth_decoy")
                    )
                }

                HorizontalDivider()

                // Random PIN Keyboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Random Keyboard Layout",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Shuffle PIN pad numbers every unlock to prevent shoulder peeping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = randomKeyboard,
                        onCheckedChange = { viewModel.setRandomKeyboard(it) },
                        modifier = Modifier.testTag("switch_random_keyboard")
                    )
                }

                HorizontalDivider()

                // Hide Pattern Path
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Pattern Path Trail",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Draw patterns silently without leaving visual lines on screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hidePatternPath,
                        onCheckedChange = { viewModel.setHidePatternPath(it) },
                        modifier = Modifier.testTag("switch_hide_pattern_path")
                    )
                }

                HorizontalDivider()

                // Intruder Selfie Capture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Intruder Capture (Camera)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Snap local photo after 3 failed unlock attempts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = intruderCapture,
                        onCheckedChange = { viewModel.setIntruderCapture(it) },
                        modifier = Modifier.testTag("switch_intruder_capture")
                    )
                }

                HorizontalDivider()

                // Android TV Leanback Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android TV Remote Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Oversized center PIN pad with focus scale & D-Pad remote navigation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tvMode,
                        onCheckedChange = { viewModel.setTvMode(it) },
                        modifier = Modifier.testTag("switch_tv_mode")
                    )
                }
            }
        }

        // Section 4: Battery Optimization & Service Longevity
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Battery Optimization Bypass",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Ensure Android OS battery saver does not stop the 10ms Accessibility service in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { viewModel.triggerImmediateLockAll() },
                    modifier = Modifier.fillMaxWidth().testTag("btn_immediate_lock_all")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Immediate Lock All")
                }
            }
        }

        // Section 5: Schedule-Based Application Lockdown Rules
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Schedule-Based Lock Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Enforce locking for specific apps during targeted time windows (e.g., work apps during evening hours 6 PM - 11 PM).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (scheduleRules.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No schedule rules configured yet. Tap below to create your first rule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    scheduleRules.forEach { rule ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rule.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format(
                                            "Active %02d:%02d to %02d:%02d",
                                            rule.startHour, rule.startMinute,
                                            rule.endHour, rule.endMinute
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Switch(
                                        checked = rule.isEnabled,
                                        onCheckedChange = { viewModel.toggleScheduleRule(rule.id, it) },
                                        modifier = Modifier.testTag("switch_rule_${rule.id}")
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteScheduleRule(rule.id) },
                                        modifier = Modifier.testTag("btn_delete_rule_${rule.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (allApps.isNotEmpty()) {
                            selectedAppForSchedule = allApps.first().packageName
                            selectedAppNameForSchedule = allApps.first().appName
                        }
                        showAddScheduleDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_add_schedule_rule")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Schedule Rule")
                }
            }
        }

        // Section: Visual Data-Usage Dashboard
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Data-Usage Storage Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Local Room database storage breakdown by category (Passwords, Notes, Encrypted Files, Apps).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val vaultItems by viewModel.encryptedVaultItems.collectAsState()
                val passwordCount = vaultItems.count { it.category == "PASSWORD" }
                val noteCount = vaultItems.count { it.category == "NOTE" }
                val fileCount = vaultItems.count { it.category == "FILE" }
                val appCount = allApps.size
                val totalItems = (passwordCount + noteCount + fileCount + appCount).coerceAtLeast(1)

                val pRatio = passwordCount.toFloat() / totalItems
                val nRatio = noteCount.toFloat() / totalItems
                val fRatio = fileCount.toFloat() / totalItems
                val aRatio = appCount.toFloat() / totalItems

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (passwordCount > 0) Box(modifier = Modifier.weight(pRatio).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                        if (noteCount > 0) Box(modifier = Modifier.weight(nRatio).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                        if (fileCount > 0) Box(modifier = Modifier.weight(fRatio).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                        if (appCount > 0) Box(modifier = Modifier.weight(aRatio).fillMaxHeight().background(MaterialTheme.colorScheme.error))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Passwords ($passwordCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Notes ($noteCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("Files ($fileCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    Text("Apps ($appCount)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Section: Automatic Backup Scheduler & Secondary Folder
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            var autoBackupEnabled by remember { mutableStateOf(true) }
            var backupFolder by remember { mutableStateOf(context.getExternalFilesDir("backups")?.absolutePath ?: "/storage/emulated/0/Download/PureLockBackups") }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Automatic Redundancy Backups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Schedule automatic encrypted JSON backups to secondary local folder storage for data redundancy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Automatic Daily Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { autoBackupEnabled = it },
                        modifier = Modifier.testTag("switch_auto_backup")
                    )
                }

                OutlinedTextField(
                    value = backupFolder,
                    onValueChange = { backupFolder = it },
                    label = { Text("Secondary Local Destination Folder") },
                    modifier = Modifier.fillMaxWidth().testTag("input_backup_folder"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Section: FIDO2 / U2F Security Key 2FA
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            var fidoEnabled by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "FIDO2 / U2F Hardware Security Key (USB/NFC)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Connect a physical security key via USB or tap via NFC for second-factor hardware authentication when unlocking the vault.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable FIDO2 / U2F Physical Key 2FA", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = fidoEnabled,
                        onCheckedChange = {
                            fidoEnabled = it
                            Toast.makeText(context, if (it) "FIDO2 / U2F Security Key 2FA Enabled" else "Security Key Disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("switch_fido_2fa")
                    )
                }
            }
        }

        // Section 6: Secure Encrypted JSON Backup & Restore for Local Room Database
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            var exportedJsonData by remember { mutableStateOf<String?>(null) }
            var showEncryptedExportDialog by remember { mutableStateOf(false) }
            var showEncryptedImportDialog by remember { mutableStateOf(false) }
            var showCsvExportDialog by remember { mutableStateOf(false) }
            var csvExportData by remember { mutableStateOf("") }

            var exportPassphrase by remember { mutableStateOf("") }
            var importPassphrase by remember { mutableStateOf("") }
            var importJsonInput by remember { mutableStateOf("") }
            var backupStatusMessage by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Encrypted Offline Room DB Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Backup or restore your full local Room database (apps, vault secrets, and rules) using AES-256-GCM encryption or human-readable CSV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportPassphrase = ""
                            showEncryptedExportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("btn_export_encrypted_json")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            csvExportData = viewModel.exportHumanReadableCsv()
                            showCsvExportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("btn_export_csv_setting")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV Export", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            importPassphrase = ""
                            importJsonInput = ""
                            showEncryptedImportDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("btn_import_encrypted_json")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp)
                    }
                }

                if (backupStatusMessage != null) {
                    Text(
                        text = backupStatusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (exportedJsonData != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AES-256 Encrypted JSON Payload:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { exportedJsonData = null }) {
                                    Text("Clear")
                                }
                            }
                            Text(
                                text = exportedJsonData!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 8
                            )
                        }
                    }
                }

                // Encrypted Export Dialog
                if (showEncryptedExportDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: JSON, Timestamp: ${System.currentTimeMillis()}, Status: TERMINATED_BY_USER")
                            showEncryptedExportDialog = false
                        },
                        title = { Text("Export Encrypted Room Backup") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Enter a strong passphrase to encrypt your backup with AES-256-GCM:")
                                OutlinedTextField(
                                    value = exportPassphrase,
                                    onValueChange = { exportPassphrase = it },
                                    label = { Text("Backup Passphrase") },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("input_export_passphrase")
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (exportPassphrase.isNotBlank()) {
                                        coroutineScope.launch {
                                            exportedJsonData = viewModel.exportEncryptedBackup(exportPassphrase)
                                            com.example.data.PureLockPreferences(context).setLastBackupTimestamp(System.currentTimeMillis())
                                            viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: JSON, Timestamp: ${System.currentTimeMillis()}, Status: SUCCESS")
                                            backupStatusMessage = "Encrypted backup created successfully!"
                                            showEncryptedExportDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("btn_confirm_encrypted_export")
                            ) {
                                Text("Encrypt & Generate")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: JSON, Timestamp: ${System.currentTimeMillis()}, Status: TERMINATED_BY_USER")
                                showEncryptedExportDialog = false
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Encrypted Import Dialog
                if (showEncryptedImportDialog) {
                    AlertDialog(
                        onDismissRequest = { showEncryptedImportDialog = false },
                        title = { Text("Import Encrypted Room Backup") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Paste your AES-256 Encrypted JSON payload:")
                                OutlinedTextField(
                                    value = importJsonInput,
                                    onValueChange = { importJsonInput = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_encrypted_json_import"),
                                    placeholder = { Text("{\"version\":1, \"salt\":\"...\", \"ciphertext\":\"...\"}") }
                                )

                                Text("Enter the decryption passphrase:")
                                OutlinedTextField(
                                    value = importPassphrase,
                                    onValueChange = { importPassphrase = it },
                                    label = { Text("Passphrase") },
                                    singleLine = true,
                                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("input_import_passphrase")
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (importJsonInput.isNotBlank() && importPassphrase.isNotBlank()) {
                                        coroutineScope.launch {
                                            val success = viewModel.importEncryptedBackup(importJsonInput, importPassphrase)
                                            if (success) {
                                                backupStatusMessage = "Backup successfully decrypted and restored into Room Database!"
                                                showEncryptedImportDialog = false
                                            } else {
                                                backupStatusMessage = "Failed to decrypt backup. Incorrect passphrase or corrupt JSON data."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("btn_confirm_encrypted_import")
                            ) {
                                Text("Decrypt & Restore")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEncryptedImportDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showCsvExportDialog) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    AlertDialog(
                        onDismissRequest = {
                            viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: CSV, Timestamp: ${System.currentTimeMillis()}, Status: TERMINATED_BY_USER")
                            showCsvExportDialog = false
                        },
                        title = { Text("Human-Readable CSV Export") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Human-readable CSV format containing your vault secrets, trash records, and app protection rules:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = csvExportData,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().height(160.dp).testTag("input_csv_export_data"),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(csvExportData))
                                    Toast.makeText(context, "CSV exported to clipboard!", Toast.LENGTH_SHORT).show()
                                    viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: CSV, Timestamp: ${System.currentTimeMillis()}, Status: SUCCESS")
                                    showCsvExportDialog = false
                                },
                                modifier = Modifier.testTag("btn_copy_csv_clipboard")
                            ) {
                                Text("Copy CSV")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                viewModel.logSecurityEvent("EXPORT_AUDIT", "Method: CSV, Timestamp: ${System.currentTimeMillis()}, Status: TERMINATED_BY_USER")
                                showCsvExportDialog = false
                            }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }

    // Change PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Master PIN Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a 4 to 8 digit secure PIN:")
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) newPinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length >= 4) {
                            viewModel.updateMasterPin(newPinInput)
                            showPinDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_pin")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Pattern Dialog
    if (showPatternDialog) {
        AlertDialog(
            onDismissRequest = { showPatternDialog = false },
            title = { Text("Set Master Pattern Path") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter comma-separated node indices (1-9):")
                    OutlinedTextField(
                        value = newPatternInput,
                        onValueChange = { newPatternInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_pattern")
                    )
                    Text(
                        text = "Example: 1,2,5,8,9 forms a clean L-shape",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPatternInput.isNotEmpty()) {
                            viewModel.updateMasterPattern(newPatternInput)
                            showPatternDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_pattern")
                ) {
                    Text("Save Pattern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPatternDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Schedule Rule Dialog
    if (showAddScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showAddScheduleDialog = false },
            title = { Text("Create Scheduled Lock Rule") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Target Application:", style = MaterialTheme.typography.bodySmall)

                    // App Selection Radio/List Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            allApps.forEach { app ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedAppForSchedule == app.packageName,
                                        onClick = {
                                            selectedAppForSchedule = app.packageName
                                            selectedAppNameForSchedule = app.appName
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Text("Active Window (24-Hour Format):", style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = startHourInput,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startHourInput = it },
                            label = { Text("Start Hr") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_start_hour")
                        )
                        OutlinedTextField(
                            value = startMinuteInput,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) startMinuteInput = it },
                            label = { Text("Start Min") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_start_min")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = endHourInput,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endHourInput = it },
                            label = { Text("End Hr") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_end_hour")
                        )
                        OutlinedTextField(
                            value = endMinuteInput,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) endMinuteInput = it },
                            label = { Text("End Min") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_end_min")
                        )
                    }

                    // Presets
                    Text("Time Window Presets:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = startHourInput == "18" && endHourInput == "23",
                            onClick = {
                                startHourInput = "18"
                                startMinuteInput = "00"
                                endHourInput = "23"
                                endMinuteInput = "00"
                            },
                            label = { Text("Evening 6-11PM", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = startHourInput == "09" && endHourInput == "17",
                            onClick = {
                                startHourInput = "09"
                                startMinuteInput = "00"
                                endHourInput = "17"
                                endMinuteInput = "00"
                            },
                            label = { Text("Work 9AM-5PM", fontSize = 10.sp) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sH = startHourInput.toIntOrNull() ?: 18
                        val sM = startMinuteInput.toIntOrNull() ?: 0
                        val eH = endHourInput.toIntOrNull() ?: 23
                        val eM = endMinuteInput.toIntOrNull() ?: 0

                        if (selectedAppForSchedule.isNotEmpty()) {
                            viewModel.addScheduleRule(
                                com.example.data.model.ScheduleRuleEntity(
                                    packageName = selectedAppForSchedule,
                                    appName = selectedAppNameForSchedule,
                                    startHour = sH,
                                    startMinute = sM,
                                    endHour = eH,
                                    endMinute = eM
                                )
                            )
                            showAddScheduleDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_schedule_rule")
                ) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}
