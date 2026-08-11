package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.ui.LockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PureLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: PureLockRepository
    private var lastCheckedPackage: String? = null

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
            prefs
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui") return

        if (packageName == lastCheckedPackage) return
        lastCheckedPackage = packageName

        serviceScope.launch {
            try {
                if (repository.isAppLockRequired(packageName)) {
                    Log.d("PureLockService", "Locking package: $packageName")
                    val intent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE, packageName)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("PureLockService", "Error evaluating app lock requirement", e)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
