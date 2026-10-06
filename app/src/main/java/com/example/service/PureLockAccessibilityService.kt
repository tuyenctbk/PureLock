package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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

        fun clearAllSessions() {
            unlockedPackages.clear()
            packageLeaveTimestamps.clear()
            currentForegroundPackage = null
            Log.d(TAG, "All active sessions cleared.")
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: PureLockRepository
    private var lastCheckedPackage: String? = null
    private var lastCheckTimestamp = 0L
    @Volatile
    private var cachedGracePeriodMs: Long = 30000L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "Screen off detected: Clearing all active unlock sessions")
                clearAllSessions()
                serviceScope.launch {
                    try {
                        repository.resetAllUnlockedSessions()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reset unlocked timestamps on screen off", e)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = PureLockDatabase.getDatabase(this)
        val prefs = PureLockPreferences(this)
        repository = PureLockRepository(
            this,
            db.appLockDao(),
            db.intruderDao(),
            db.logDao(),
            db.scheduleRuleDao(),
            db.encryptedVaultDao(),
            db.userSettingDao(),
            prefs
        )

        serviceScope.launch {
            try {
                repository.preferences.gracePeriodMs.collect {
                    cachedGracePeriodMs = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting grace period preference", e)
            }
        }

        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(this, screenOffReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen-off receiver", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return

        val currentTime = System.currentTimeMillis()

        // Track foreground transitions and leave timestamps
        val prevForeground = currentForegroundPackage
        if (prevForeground != null && prevForeground != packageName) {
            if (unlockedPackages.containsKey(prevForeground)) {
                packageLeaveTimestamps[prevForeground] = currentTime
            }
        }
        currentForegroundPackage = packageName

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
                if (repository.isAppLockRequired(packageName)) {
                    Log.d(TAG, "Locking package: $packageName")
                    val intent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE, packageName)
                    }
                    startActivity(intent)
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
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Defensive catch if already unregistered
        }
        serviceScope.cancel()
    }
}
