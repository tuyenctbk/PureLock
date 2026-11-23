package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.PureLockTheme

class MainActivity : FragmentActivity() {

    private val viewModel: PureLockViewModel by viewModels()

    private val shortcutActionState = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutActionState.value = intent.getStringExtra("shortcut_action")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent plaintext screenshots, screen recording, and Recents thumbnails of the encrypted vault
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        shortcutActionState.value = intent?.getStringExtra("shortcut_action")

        viewModel.bindBillingSession(this)
        viewModel.recordAppLaunch()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
            val shakeToLockEnabled by viewModel.shakeToLockEnabled.collectAsState()
            val shakeSensitivity by viewModel.shakeSensitivity.collectAsState()
            val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
            val hasSplashFinished by viewModel.hasSplashFinished.collectAsState()
            val appAuthenticated by viewModel.isAuthenticated.collectAsState()

            PureLockTheme(themeMode = themeMode) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                val latestInactivityTimeoutSec by rememberUpdatedState(inactivityTimeoutSec)
                val latestAppAuthenticated by rememberUpdatedState(appAuthenticated)

                val shortcutAction by shortcutActionState

                val latestHapticEnabled by rememberUpdatedState(hapticFeedbackEnabled)
                val parsedSensitivity = remember(shakeSensitivity) {
                    com.example.service.ShakeSensitivity.fromKey(shakeSensitivity)
                }

                // Lifecycle-aware, gravity-filtered Shake-to-Lock Accelerometer Controller
                val shakeController = remember(context) {
                    com.example.service.ShakeToLockSensorController(
                        context = context,
                        onShakeDetected = {
                            if (latestAppAuthenticated) {
                                if (latestHapticEnabled) {
                                    com.example.service.ShakeToLockSensorController.triggerLockHapticFeedback(context)
                                }
                                viewModel.lockApp()
                                viewModel.triggerEmergencySessionLockdown()
                                viewModel.logSecurityEvent(
                                    "SHAKE_TO_LOCK",
                                    "Emergency lockdown triggered inside PureLock via accelerometer shake gesture."
                                )
                            }
                        }
                    )
                }

                LaunchedEffect(parsedSensitivity) {
                    shakeController.updateSensitivity(parsedSensitivity)
                }

                DisposableEffect(lifecycleOwner, shakeToLockEnabled, appAuthenticated, parsedSensitivity) {
                    val shouldListen = shakeToLockEnabled && appAuthenticated
                    if (!shouldListen) {
                        shakeController.stop()
                        onDispose { shakeController.stop() }
                    } else {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                            shakeController.start(parsedSensitivity)
                        }
                        val sensorLifecycleObserver = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            when (event) {
                                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                                    if (latestAppAuthenticated) {
                                        shakeController.start(parsedSensitivity)
                                    }
                                }
                                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                                    shakeController.stop()
                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(sensorLifecycleObserver)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(sensorLifecycleObserver)
                            shakeController.stop()
                        }
                    }
                }

                // Auto-Lock on App Background Movement & Screen-Off (preserving session across configuration changes)
                var appBackgroundTimestamp by remember { mutableLongStateOf(0L) }

                DisposableEffect(lifecycleOwner) {
                    val screenOffReceiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                                if (latestAppAuthenticated) {
                                    viewModel.lockApp()
                                }
                            }
                        }
                    }
                    val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
                    androidx.core.content.ContextCompat.registerReceiver(
                        context,
                        screenOffReceiver,
                        filter,
                        androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                    )

                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                                if (!isChangingConfigurations && latestAppAuthenticated) {
                                    appBackgroundTimestamp = System.currentTimeMillis()
                                    viewModel.cancelAutoLockTimer()
                                    if (!com.example.util.PermissionManager.isReturningFromSettings()) {
                                        viewModel.clearSensitiveState()
                                    }
                                }
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_START -> {
                                if (appBackgroundTimestamp > 0L) {
                                    val elapsed = System.currentTimeMillis() - appBackgroundTimestamp
                                    val timeoutSec = latestInactivityTimeoutSec
                                    val timeoutMillis = if (timeoutSec > 0) timeoutSec * 1000L else 0L
                                    val returningFromSettings = com.example.util.PermissionManager.isReturningFromSettings()
                                    if (returningFromSettings) {
                                        com.example.util.PermissionManager.clearReturningFromSettings()
                                    }

                                    if (latestAppAuthenticated && !returningFromSettings && elapsed >= timeoutMillis) {
                                        viewModel.lockApp()
                                        viewModel.logSecurityEvent(
                                            "BACKGROUND_AUTO_LOCK",
                                            "PureLock secured on background return. Master authentication required."
                                        )
                                    } else if (latestAppAuthenticated) {
                                        viewModel.resetAutoLockTimer()
                                    }
                                    appBackgroundTimestamp = 0L
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        try {
                            context.unregisterReceiver(screenOffReceiver)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Inactivity Auto-Lock Loop driven directly by ViewModel without triggering Compose recomposition per tap
                LaunchedEffect(appAuthenticated, inactivityTimeoutSec) {
                    if (appAuthenticated && inactivityTimeoutSec > 0) {
                        viewModel.resetAutoLockTimer()
                    } else if (!appAuthenticated) {
                        viewModel.cancelAutoLockTimer()
                    }
                }

                if (isOnboardingCompleted == null || !hasSplashFinished) {
                    SecuritySplashScreen(
                        onSplashFinished = { viewModel.markSplashFinished() }
                    )
                } else {
                    AnimatedContent(
                        targetState = Pair(isOnboardingCompleted == true, appAuthenticated),
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(450)) + scaleIn(initialScale = 0.94f, animationSpec = tween(450))) togetherWith
                            (fadeOut(animationSpec = tween(350)) + scaleOut(targetScale = 1.05f, animationSpec = tween(350)))
                        },
                        label = "AppLockTransition"
                    ) { (onboarded, isAuthenticated) ->
                        if (!onboarded) {
                            com.example.ui.OnboardingScreen(
                                onOnboardingComplete = { pin, securityType, pattern ->
                                    viewModel.completeOnboarding(pin, securityType, pattern)
                                    viewModel.unlockApp()
                                }
                            )
                        } else if (!isAuthenticated) {
                            LockOverlayScreen(
                                packageName = context.packageName,
                                repository = viewModel.repository,
                                onUnlocked = {
                                    viewModel.unlockApp()
                                    viewModel.recordVaultUnlock()
                                },
                                onCancelled = { finish() }
                            )
                        } else {
                            val navController = rememberNavController()
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route ?: "vault"

                            // Handle warm app shortcut intents while already authenticated
                            LaunchedEffect(shortcutAction) {
                                val targetRoute = when (shortcutAction) {
                                    "VIEW_SHIELD" -> "shield"
                                    "VIEW_INTRUDER" -> "intruder"
                                    "VIEW_AUDIT" -> "audit"
                                    "VIEW_SETTINGS" -> "suite"
                                    "VIEW_VAULT", "GENERATE_PASSWORD" -> "vault"
                                    else -> null
                                }
                                if (targetRoute != null && targetRoute != currentRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                if (shortcutAction != null && shortcutAction != "GENERATE_PASSWORD") {
                                    shortcutActionState.value = null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                                viewModel.onUserActivity()
                                            }
                                        }
                                    }
                            ) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    bottomBar = {
                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .testTag("main_navigation_bar")
                                        ) {
                                            val navColors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "vault",
                                                colors = navColors,
                                                onClick = {
                                                    viewModel.onUserActivity()
                                                    navController.navigate("vault") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == "vault") Icons.Filled.Lock else Icons.Outlined.Lock,
                                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_vault)
                                                    )
                                                },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_vault), fontSize = 11.sp, fontWeight = if (currentRoute == "vault") FontWeight.Bold else FontWeight.Medium) },
                                                modifier = Modifier.testTag("nav_item_vault")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "shield",
                                                colors = navColors,
                                                onClick = {
                                                    viewModel.onUserActivity()
                                                    navController.navigate("shield") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == "shield") Icons.Filled.Shield else Icons.Outlined.Shield,
                                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_shield)
                                                    )
                                                },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_shield), fontSize = 11.sp, fontWeight = if (currentRoute == "shield") FontWeight.Bold else FontWeight.Medium) },
                                                modifier = Modifier.testTag("nav_item_shield")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "intruder",
                                                colors = navColors,
                                                onClick = {
                                                    viewModel.onUserActivity()
                                                    navController.navigate("intruder") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == "intruder") Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt,
                                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_intruder)
                                                    )
                                                },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_intruder), fontSize = 11.sp, fontWeight = if (currentRoute == "intruder") FontWeight.Bold else FontWeight.Medium) },
                                                modifier = Modifier.testTag("nav_item_intruder")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "suite",
                                                colors = navColors,
                                                onClick = {
                                                    viewModel.onUserActivity()
                                                    navController.navigate("suite") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == "suite") Icons.Filled.Tune else Icons.Outlined.Tune,
                                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_settings)
                                                    )
                                                },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_settings), fontSize = 11.sp, fontWeight = if (currentRoute == "suite") FontWeight.Bold else FontWeight.Medium) },
                                                modifier = Modifier.testTag("nav_item_suite")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "audit",
                                                colors = navColors,
                                                onClick = {
                                                    viewModel.onUserActivity()
                                                    navController.navigate("audit") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (currentRoute == "audit") Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser,
                                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_audit)
                                                    )
                                                },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_audit), fontSize = 11.sp, fontWeight = if (currentRoute == "audit") FontWeight.Bold else FontWeight.Medium) },
                                                modifier = Modifier.testTag("nav_item_audit")
                                            )
                                        }
                                    }
                                ) { innerPadding ->
                                    NavHost(
                                        navController = navController,
                                        startDestination = "vault",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding),
                                        enterTransition = {
                                            fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                                                initialOffsetX = { it / 10 },
                                                animationSpec = tween(250)
                                            )
                                        },
                                        exitTransition = {
                                            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                                targetOffsetX = { -it / 10 },
                                                animationSpec = tween(200)
                                            )
                                        },
                                        popEnterTransition = {
                                            fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                                                initialOffsetX = { -it / 10 },
                                                animationSpec = tween(250)
                                            )
                                        },
                                        popExitTransition = {
                                            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                                targetOffsetX = { it / 10 },
                                                animationSpec = tween(200)
                                            )
                                        }
                                    ) {
                                        composable("vault") {
                                            EncryptedVaultDashboardScreen(
                                                viewModel = viewModel,
                                                openGeneratorOnLaunch = shortcutAction == "GENERATE_PASSWORD",
                                                onGeneratorLaunchConsumed = {
                                                    if (shortcutActionState.value == "GENERATE_PASSWORD") {
                                                        shortcutActionState.value = null
                                                    }
                                                },
                                                onNavigateToSettings = {
                                                    navController.navigate("suite") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                        composable("shield") {
                                            AppShieldScreen(
                                                viewModel = viewModel,
                                                onNavigateToSettings = {
                                                    navController.navigate("suite") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            )
                                        }
                                        composable("intruder") {
                                            IntruderVaultScreen(viewModel = viewModel)
                                        }
                                        composable("suite") {
                                            InvisibleSuiteScreen(viewModel = viewModel)
                                        }
                                        composable("audit") {
                                            PrivacyShieldScreen(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
