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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Perform periodic background checks
        com.example.service.DatabaseChecksumManager(this).validateChecksumAndLog()
        com.example.service.BackupHealthService(this).checkBackupHealth()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
            val shakeToLockEnabled by viewModel.shakeToLockEnabled.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

            PureLockTheme(themeMode = themeMode) {
                var hasSplashFinished by remember { mutableStateOf(false) }
                var appAuthenticated by remember { mutableStateOf(false) }
                var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                val shortcutAction = remember { intent?.getStringExtra("shortcut_action") }
                val startRoute = remember(shortcutAction) {
                    when (shortcutAction) {
                        "VIEW_SHIELD" -> "shield"
                        "VIEW_INTRUDER" -> "intruder"
                        "VIEW_AUDIT" -> "audit"
                        else -> "vault"
                    }
                }

                // Shake to Lock Accelerometer Sensor Listener
                DisposableEffect(shakeToLockEnabled) {
                    if (!shakeToLockEnabled) {
                        onDispose { }
                    } else {
                        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
                        val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)

                        var lastShakeTime = 0L
                        val listener = object : android.hardware.SensorEventListener {
                            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                                if (event == null) return
                                val x = event.values[0]
                                val y = event.values[1]
                                val z = event.values[2]

                                val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / android.hardware.SensorManager.GRAVITY_EARTH
                                val currentTime = System.currentTimeMillis()
                                if (gForce > 2.5 && currentTime - lastShakeTime > 1500) {
                                    lastShakeTime = currentTime
                                    appAuthenticated = false
                                    viewModel.clearSensitiveState()
                                    viewModel.logSecurityEvent("SHAKE_TO_LOCK", "Emergency lockdown triggered via Shake to Lock gesture.")
                                }
                            }

                            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
                        }

                        if (sensorManager != null && accelerometer != null) {
                            sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
                        }

                        onDispose {
                            sensorManager?.unregisterListener(listener)
                        }
                    }
                }

                // Auto-Lock on App Background Movement & Screen-Off
                var appBackgroundTimestamp by remember { mutableLongStateOf(0L) }

                DisposableEffect(lifecycleOwner) {
                    val screenOffReceiver = object : android.content.BroadcastReceiver() {
                        override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                                appAuthenticated = false
                                viewModel.clearSensitiveState()
                            }
                        }
                    }
                    val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
                    context.registerReceiver(screenOffReceiver, filter)

                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        when (event) {
                            androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                                appBackgroundTimestamp = System.currentTimeMillis()
                                viewModel.clearSensitiveState()
                            }
                            androidx.lifecycle.Lifecycle.Event.ON_START -> {
                                if (appBackgroundTimestamp > 0L) {
                                    // Auto-lock whenever app returns from background
                                    appAuthenticated = false
                                    viewModel.clearSensitiveState()
                                    viewModel.logSecurityEvent(
                                        "BACKGROUND_AUTO_LOCK",
                                        "PureLock secured on background return. Biometric authentication required."
                                    )
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

                // Inactivity Auto-Lock Loop using ViewModel Timer
                val isAppAutoLocked by viewModel.isAppAutoLocked.collectAsState()

                LaunchedEffect(isAppAutoLocked) {
                    if (isAppAutoLocked) {
                        appAuthenticated = false
                    }
                }

                LaunchedEffect(appAuthenticated, inactivityTimeoutSec, lastInteractionTime) {
                    if (appAuthenticated && inactivityTimeoutSec > 0) {
                        viewModel.onUserActivity {
                            appAuthenticated = false
                        }
                    }
                }

                if (isOnboardingCompleted == null || !hasSplashFinished) {
                    SecuritySplashScreen(
                        onSplashFinished = { hasSplashFinished = true }
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
                                }
                            )
                        } else if (!isAuthenticated) {
                            LockOverlayScreen(
                                packageName = "com.example",
                                repository = viewModel.repository,
                                onUnlocked = {
                                    appAuthenticated = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onCancelled = { finish() }
                            )
                        } else {
                            val navController = rememberNavController()
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route ?: "vault"

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                lastInteractionTime = System.currentTimeMillis()
                                                viewModel.onUserActivity { appAuthenticated = false }
                                            }
                                        )
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
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    navController.navigate("vault") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Lock, contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_vault)) },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_vault), fontSize = 11.sp, fontWeight = if (currentRoute == "vault") FontWeight.Bold else FontWeight.Normal) },
                                                modifier = Modifier.testTag("nav_item_vault")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "shield",
                                                colors = navColors,
                                                onClick = {
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    navController.navigate("shield") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Shield, contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_shield)) },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_shield), fontSize = 11.sp, fontWeight = if (currentRoute == "shield") FontWeight.Bold else FontWeight.Normal) },
                                                modifier = Modifier.testTag("nav_item_shield")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "intruder",
                                                colors = navColors,
                                                onClick = {
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    navController.navigate("intruder") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_intruder)) },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_intruder), fontSize = 11.sp, fontWeight = if (currentRoute == "intruder") FontWeight.Bold else FontWeight.Normal) },
                                                modifier = Modifier.testTag("nav_item_intruder")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "suite",
                                                colors = navColors,
                                                onClick = {
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    navController.navigate("suite") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Tune, contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_settings)) },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_settings), fontSize = 11.sp, fontWeight = if (currentRoute == "suite") FontWeight.Bold else FontWeight.Normal) },
                                                modifier = Modifier.testTag("nav_item_suite")
                                            )

                                            NavigationBarItem(
                                                selected = currentRoute == "audit",
                                                colors = navColors,
                                                onClick = {
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    navController.navigate("audit") {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = androidx.compose.ui.res.stringResource(R.string.nav_audit)) },
                                                label = { Text(androidx.compose.ui.res.stringResource(R.string.nav_audit), fontSize = 11.sp, fontWeight = if (currentRoute == "audit") FontWeight.Bold else FontWeight.Normal) },
                                                modifier = Modifier.testTag("nav_item_audit")
                                            )
                                        }
                                    }
                                ) { innerPadding ->
                                    NavHost(
                                        navController = navController,
                                        startDestination = startRoute,
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
                                                onNavigateToSettings = { navController.navigate("suite") }
                                            )
                                        }
                                        composable("shield") {
                                            AppShieldScreen(
                                                viewModel = viewModel,
                                                onNavigateToSettings = { navController.navigate("suite") }
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
