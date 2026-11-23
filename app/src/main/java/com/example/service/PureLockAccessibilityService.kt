package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.ui.LockOverlayActivity
import com.example.util.ClipboardSecurityManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PureLockAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PureLockService"

        // Tracks package -> unlock timestamp
        private val unlockedPackages = java.util.concurrent.ConcurrentHashMap<String, Long>()
        // Tracks package -> leave timestamp
        private val packageLeaveTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

        @Volatile
        private var currentForegroundPackage: String? = null

        fun getCurrentForegroundPackage(): String? = currentForegroundPackage

        fun onPackageUnlocked(packageName: String) {
            val now = System.currentTimeMillis()
            unlockedPackages[packageName] = now
            packageLeaveTimestamps.remove(packageName)
            currentForegroundPackage = packageName
            Log.d(TAG, "Active session initiated for: $packageName at $now")
        }

        fun isPackageSessionValid(packageName: String, gracePeriodMs: Long): Boolean {
            if (!unlockedPackages.containsKey(packageName)) return false
            val leaveTime = packageLeaveTimestamps[packageName] ?: return true
            val now = System.currentTimeMillis()
            return when {
                gracePeriodMs == -1L -> true
                gracePeriodMs == 0L -> false
                gracePeriodMs > 0L -> (now - leaveTime) <= gracePeriodMs
                else -> false
            }
        }

        fun hasActiveUnlockedSessions(): Boolean = unlockedPackages.isNotEmpty()

        fun clearAllSessions() {
            unlockedPackages.clear()
            packageLeaveTimestamps.clear()
            currentForegroundPackage = null
            Log.d(TAG, "All active sessions cleared.")
        }

        fun launchOverlayForPackage(context: Context, packageName: String) {
            try {
                val appContext = context.applicationContext
                val intent = Intent(appContext, LockOverlayActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE, packageName)
                }
                appContext.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to launch LockOverlayActivity for $packageName", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repositoryDeferred = CompletableDeferred<PureLockRepository>()
    private var shakeController: ShakeToLockSensorController? = null
    private var lastCheckedPackage: String? = null
    private var lastCheckTimestamp = 0L

    @Volatile
    private var cachedLockedPackages: Set<String> = emptySet()
    @Volatile
    private var lockedPackagesCacheReady: Boolean = false
    @Volatile
    private var cachedGracePeriodMs: Long = 30000L
    @Volatile
    private var cachedShakeEnabled: Boolean = true
    @Volatile
    private var cachedShakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM
    @Volatile
    private var cachedHapticEnabled: Boolean = true
    @Volatile
    private var isScreenInteractive: Boolean = true
    @Volatile
    private var cachedImePackages: Set<String> = emptySet()
    @Volatile
    private var imeCacheLastRefreshedMs: Long = 0L

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                    updateShakeSensorState()
                    Log.d(TAG, "Screen off detected: Pausing shake sensor and clearing active unlock sessions")
                    clearAllSessions()
                    LockdownSignalBus.emitLockdown()
                    serviceScope.launch {
                        try {
                            repositoryDeferred.await().resetAllUnlockedSessions()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reset unlocked timestamps on screen off", e)
                        }
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenInteractive = true
                    updateShakeSensorState()
                }
            }
        }
    }

    private fun updateShakeSensorState() {
        val controller = shakeController ?: return
        if (cachedShakeEnabled && isScreenInteractive) {
            controller.start(cachedShakeSensitivity)
        } else {
            controller.stop()
        }
    }

    private fun handleGlobalShakeDetected() {
        val activePkg = currentForegroundPackage
        // When PureLock itself is in the foreground, MainActivity handles the lock screen directly
        if (activePkg == applicationContext.packageName) return

        val hadSessions = hasActiveUnlockedSessions()
        serviceScope.launch {
            try {
                val repo = repositoryDeferred.await()
                val isForegroundProtected = !activePkg.isNullOrBlank() && repo.isPackageProtected(activePkg)
                if (!isForegroundProtected && !hadSessions) {
                    // Avoid false vibrations or log spam while walking/jogging outside protected apps
                    return@launch
                }

                if (cachedHapticEnabled) {
                    ShakeToLockSensorController.triggerLockHapticFeedback(applicationContext)
                }

                ClipboardSecurityManager.clearClipboard(applicationContext, showNotification = false)
                repo.resetAllUnlockedSessions()
                repo.logSecurityEvent(
                    "SHAKE_TO_LOCK",
                    if (isForegroundProtected) {
                        "Instant Shake-to-Lock triggered while using protected app: $activePkg."
                    } else {
                        "Global Shake-to-Lock cleared all active protected app sessions."
                    }
                )

                if (isForegroundProtected) {
                    val lockedPkg = activePkg ?: return@launch
                    Log.d(TAG, "Shake-to-Lock engaging overlay immediately for: $lockedPkg")
                    launchOverlayActivity(lockedPkg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling global Shake-to-Lock gesture", e)
            }
        }
    }

    private fun launchOverlayActivity(packageName: String) {
        launchOverlayForPackage(applicationContext, packageName)
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenInteractive = powerManager?.isInteractive ?: true

        shakeController = ShakeToLockSensorController(
            context = this,
            onShakeDetected = ::handleGlobalShakeDetected
        )

        serviceScope.launch {
            try {
                val db = PureLockDatabase.getDatabase(this@PureLockAccessibilityService)
                val prefs = PureLockPreferences(this@PureLockAccessibilityService)
                val repo = PureLockRepository(
                    this@PureLockAccessibilityService,
                    db.appLockDao(),
                    db.intruderDao(),
                    db.logDao(),
                    db.scheduleRuleDao(),
                    db.encryptedVaultDao(),
                    db.userSettingDao(),
                    prefs
                )
                repositoryDeferred.complete(repo)
                refreshImePackageCache()

                launch {
                    try {
                        repo.activeLockedPackageNames.collect { lockedList ->
                            cachedLockedPackages = lockedList.toHashSet()
                            lockedPackagesCacheReady = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting active locked packages", e)
                    }
                }

                launch {
                    try {
                        repo.preferences.gracePeriodMs.collect {
                            cachedGracePeriodMs = it
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting grace period preference", e)
                    }
                }

                launch {
                    try {
                        combine(
                            repo.preferences.shakeToLock,
                            repo.preferences.shakeSensitivity,
                            repo.preferences.hapticFeedbackEnabled
                        ) { enabled, sensitivityKey, haptic ->
                            Triple(enabled, ShakeSensitivity.fromKey(sensitivityKey), haptic)
                        }.collect { (enabled, sensitivity, haptic) ->
                            cachedShakeEnabled = enabled
                            cachedShakeSensitivity = sensitivity
                            cachedHapticEnabled = haptic
                            shakeController?.updateSensitivity(sensitivity)
                            updateShakeSensorState()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting Shake-to-Lock preferences", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize PureLockAccessibilityService repository", e)
                repositoryDeferred.completeExceptionally(e)
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            ContextCompat.registerReceiver(this, screenStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen state receiver", e)
        }
    }

    private fun refreshImePackageCache() {
        try {
            val pkgs = mutableSetOf<String>()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.enabledInputMethodList?.forEach { info ->
                if (!info.packageName.isNullOrBlank()) pkgs.add(info.packageName)
            }
            val defaultIme = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.DEFAULT_INPUT_METHOD
            )
            val defaultPkg = defaultIme?.substringBefore('/')
            if (!defaultPkg.isNullOrBlank()) pkgs.add(defaultPkg)
            cachedImePackages = pkgs
            imeCacheLastRefreshedMs = System.currentTimeMillis()
        } catch (_: Exception) {
        }
    }

    private fun isInputMethodPackage(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - imeCacheLastRefreshedMs > 60_000L) {
            imeCacheLastRefreshedMs = now
            serviceScope.launch { refreshImePackageCache() }
        }
        return packageName in cachedImePackages
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == "com.android.systemui" || packageName == "android") return
        if (packageName == "com.android.permissioncontroller" || packageName == "com.google.android.permissioncontroller") return

        // Ignore transient IME keyboard windows and toast popups so typing never triggers a false re-lock
        val className = event.className?.toString() ?: ""
        if (className.contains("Toast", ignoreCase = true) ||
            className.contains("InputMethod", ignoreCase = true) ||
            isInputMethodPackage(packageName)
        ) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // Track foreground transitions and leave timestamps
        val prevForeground = currentForegroundPackage
        if (prevForeground != null && prevForeground != packageName) {
            if (unlockedPackages.containsKey(prevForeground)) {
                packageLeaveTimestamps[prevForeground] = currentTime
            }
        }
        currentForegroundPackage = packageName

        // PureLock manages its own internal lock state via MainActivity
        if (packageName == applicationContext.packageName) return

        // Fast-path O(1) in-memory filter: avoid hitting SQLCipher Room DB for unlocked apps & launchers
        if (lockedPackagesCacheReady && packageName !in cachedLockedPackages) {
            return
        }

        // Check if package has an active, unexpired unlock session
        if (isPackageSessionValid(packageName, cachedGracePeriodMs)) {
            packageLeaveTimestamps.remove(packageName)
            return
        } else {
            // Expired or never unlocked
            unlockedPackages.remove(packageName)
            packageLeaveTimestamps.remove(packageName)
        }

        // Prevent rapid duplicate evaluations for the same package within 500ms
        if (packageName == lastCheckedPackage && (currentTime - lastCheckTimestamp) < 500L) {
            return
        }
        lastCheckedPackage = packageName
        lastCheckTimestamp = currentTime

        serviceScope.launch {
            try {
                val repo = repositoryDeferred.await()
                if (repo.isAppLockRequired(packageName, cachedGracePeriodMs)) {
                    Log.d(TAG, "Locking package: $packageName")
                    launchOverlayActivity(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating app lock requirement", e)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeController?.stop()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Defensive catch if already unregistered
        }
        serviceScope.cancel()
    }
}

