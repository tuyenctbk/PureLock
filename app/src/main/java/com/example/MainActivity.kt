package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.theme.PureLockTheme
import kotlinx.coroutines.delay

class MainActivity : FragmentActivity() {

    private val viewModel: PureLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Privacy Overlay: Prevents screenshotting and automatically blurs/obscures app contents in Recent Apps overview
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        // Perform periodic background checks
        com.example.service.DatabaseChecksumManager(this).validateChecksumAndLog()
        com.example.service.BackupHealthService(this).checkBackupHealth()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val inactivityTimeoutSec by viewModel.inactivityTimeoutSec.collectAsState()
            val shakeToLockEnabled by viewModel.shakeToLockEnabled.collectAsState()

            PureLockTheme(themeMode = themeMode) {
                var appAuthenticated by remember { mutableStateOf(false) }
                var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                val shortcutAction = remember { intent?.getStringExtra("shortcut_action") }
                val startRoute = remember(shortcutAction) {
                    if (shortcutAction == "VIEW_VAULT" || shortcutAction == "GENERATE_PASSWORD") "vault" else "shield"
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

                // Screen-Off Receiver & Lifecycle Navigation Away Listener
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
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                            appAuthenticated = false
                            viewModel.clearSensitiveState()
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

                // Inactivity Auto-Lock Loop
                LaunchedEffect(appAuthenticated, inactivityTimeoutSec, lastInteractionTime) {
                    if (appAuthenticated && inactivityTimeoutSec > 0) {
                        val timeoutMs = inactivityTimeoutSec * 1000L
                        val elapsed = System.currentTimeMillis() - lastInteractionTime
                        val remaining = timeoutMs - elapsed
                        if (remaining > 0) {
                            delay(remaining)
                            if (System.currentTimeMillis() - lastInteractionTime >= timeoutMs) {
                                appAuthenticated = false
                                viewModel.clearSensitiveState()
                            }
                        } else {
                            appAuthenticated = false
                            viewModel.clearSensitiveState()
                        }
                    }
                }

                AnimatedContent(
                    targetState = appAuthenticated,
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                    },
                    label = "AppLockTransition"
                ) { isAuthenticated ->
                    if (!isAuthenticated) {
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
                        val currentRoute = navBackStackEntry?.destination?.route ?: "shield"

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                    )
                                }
                        ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("main_navigation_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = currentRoute == "shield",
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
                                        icon = { Icon(Icons.Default.Shield, contentDescription = "Shield") },
                                        label = { Text("App Shield") },
                                        modifier = Modifier.testTag("nav_item_shield")
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "suite",
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
                                        icon = { Icon(Icons.Default.VisibilityOff, contentDescription = "Invisible Suite") },
                                        label = { Text("Invisible Suite") },
                                        modifier = Modifier.testTag("nav_item_suite")
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "vault",
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
                                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Vault") },
                                        label = { Text("Intruder Vault") },
                                        modifier = Modifier.testTag("nav_item_vault")
                                    )

                                    NavigationBarItem(
                                        selected = currentRoute == "audit",
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
                                        icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Privacy Audit") },
                                        label = { Text("Privacy Audit") },
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
                                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                                        initialOffsetX = { it / 8 },
                                        animationSpec = tween(300)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                                        targetOffsetX = { -it / 8 },
                                        animationSpec = tween(250)
                                    )
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                                        initialOffsetX = { -it / 8 },
                                        animationSpec = tween(300)
                                    )
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                                        targetOffsetX = { it / 8 },
                                        animationSpec = tween(250)
                                    )
                                }
                            ) {
                                composable("shield") {
                                    AppShieldScreen(
                                        viewModel = viewModel,
                                        onNavigateToSettings = { navController.navigate("suite") }
                                    )
                                }
                                composable("suite") {
                                    InvisibleSuiteScreen(viewModel = viewModel)
                                }
                                composable("vault") {
                                    IntruderVaultScreen(viewModel = viewModel)
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
