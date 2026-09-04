package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.FirebaseManager

@Composable
fun OnboardingScreen(
    onOnboardingComplete: (pin: String, securityType: String, pattern: String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    var selectedSecurityType by remember { mutableStateOf("PIN") }
    var inputPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var patternNodes by remember { mutableStateOf(listOf<Int>()) }
    var pinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStep) {
        FirebaseManager.logScreenView("onboarding_step_$currentStep")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == currentStep) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentStep) Color(0xFF2563EB) else Color(0xFF475569)
                            )
                    )
                }
            }

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "OnboardingTransition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { step ->
                when (step) {
                    0 -> OnboardingStepWelcome()
                    1 -> OnboardingStepSecuritySetup(
                        selectedSecurityType = selectedSecurityType,
                        onSecurityTypeSelected = { selectedSecurityType = it },
                        pin = inputPin,
                        onPinChanged = { inputPin = it },
                        confirmPin = confirmPin,
                        onConfirmPinChanged = { confirmPin = it },
                        patternNodes = patternNodes,
                        onPatternNodesChanged = { patternNodes = it },
                        error = pinError
                    )
                    2 -> OnboardingStepPermissions(context = context)
                    3 -> OnboardingStepComplete()
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.onboarding_btn_back))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                val minPinError = stringResource(R.string.onboarding_pin_min_length_error)
                val mismatchError = stringResource(R.string.onboarding_pin_mismatch_error)
                val patternError = stringResource(R.string.onboarding_pattern_min_nodes_error)

                Button(
                    onClick = {
                        if (currentStep == 1) {
                            if (inputPin.length < 4) {
                                pinError = minPinError
                                return@Button
                            }
                            if (inputPin != confirmPin) {
                                pinError = mismatchError
                                return@Button
                            }
                            if (selectedSecurityType == "PATTERN" && patternNodes.size < 4) {
                                pinError = patternError
                                return@Button
                            }
                            pinError = null
                        }

                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            val finalPin = if (inputPin.isNotBlank()) inputPin else "1234"
                            val finalPattern = if (patternNodes.isNotEmpty()) patternNodes.joinToString(",") else "1,2,5,8,9"
                            onOnboardingComplete(finalPin, selectedSecurityType, finalPattern)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (currentStep == 3) stringResource(R.string.onboarding_btn_enter) else stringResource(R.string.onboarding_btn_continue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PureLockLogoEmblem(
            size = 108.dp,
            showGlowRing = true,
            showRadarScan = true,
            elevation = 16.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            fontSize = 15.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun OnboardingStepSecuritySetup(
    selectedSecurityType: String,
    onSecurityTypeSelected: (String) -> Unit,
    pin: String,
    onPinChanged: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChanged: (String) -> Unit,
    patternNodes: List<Int>,
    onPatternNodesChanged: (List<Int>) -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_setup_key_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when (selectedSecurityType) {
                "PATTERN" -> stringResource(R.string.onboarding_setup_pattern_desc)
                "BIOMETRIC" -> stringResource(R.string.onboarding_biometrics_desc)
                else -> stringResource(R.string.onboarding_setup_pin_desc)
            },
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("PIN", "PATTERN", "BIOMETRIC").forEach { mode ->
                FilterChip(
                    selected = selectedSecurityType == mode,
                    onClick = { onSecurityTypeSelected(mode) },
                    label = { Text(mode, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSecurityType) {
            "PATTERN" -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (patternNodes.isEmpty()) stringResource(R.string.onboarding_draw_pattern_hint) else stringResource(R.string.onboarding_selected_nodes_format, patternNodes.joinToString("-")),
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    PatternGridDrawer(
                        selectedNodes = patternNodes,
                        hidePath = false,
                        onNodeSelected = { node ->
                            if (!patternNodes.contains(node)) {
                                onPatternNodesChanged(patternNodes + node)
                            }
                        },
                        onPatternCompleted = {}
                    )

                    if (patternNodes.isNotEmpty()) {
                        TextButton(onClick = { onPatternNodesChanged(emptyList()) }) {
                            Text(stringResource(R.string.onboarding_reset_pattern), color = Color(0xFFF87171), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            "BIOMETRIC" -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.onboarding_biometrics_title),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(R.string.onboarding_biometrics_desc),
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Master PIN (Always required as primary or essential fallback)
        OutlinedTextField(
            value = pin,
            onValueChange = onPinChanged,
            label = { Text(if (selectedSecurityType == "PIN") stringResource(R.string.onboarding_master_pin_label) else stringResource(R.string.onboarding_fallback_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = Color(0xFF2563EB),
                unfocusedLabelColor = Color(0xFF94A3B8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = onConfirmPinChanged,
            label = { Text(stringResource(R.string.onboarding_confirm_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFF475569),
                focusedLabelColor = Color(0xFF2563EB),
                unfocusedLabelColor = Color(0xFF94A3B8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingStepPermissions(context: android.content.Context) {
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            icon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = Color(0xFF38BDF8)) },
            title = { Text(stringResource(R.string.onboarding_disclosure_title), fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_why_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_why_desc),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_privacy_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.onboarding_disclosure_privacy_desc),
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        try {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(stringResource(R.string.onboarding_disclosure_agree))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAccessibilityDisclosure = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.onboarding_disclosure_decline))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_perm_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_perm_desc),
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.onboarding_perm_acc_title), fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.onboarding_perm_acc_desc), fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAccessibilityDisclosure = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text(stringResource(R.string.onboarding_perm_acc_btn))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.onboarding_perm_usage_title), fontWeight = FontWeight.Bold, color = Color.White)
                        Text(stringResource(R.string.onboarding_perm_usage_desc), fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text(stringResource(R.string.onboarding_perm_usage_btn))
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepComplete() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = Color(0xFF166534)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_complete_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_complete_desc),
            fontSize = 15.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
