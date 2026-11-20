package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.ui.theme.PureLockTheme
import kotlinx.coroutines.launch

class LockOverlayActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
    }

    private lateinit var repository: PureLockRepository
    private var lockedPackageState = mutableStateOf("")

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
        if (!newPackage.isNullOrEmpty()) {
            lockedPackageState.value = newPackage
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateHomeSafely()
            }
        })

        val db = PureLockDatabase.getDatabase(this)
        val prefs = PureLockPreferences(this)
        repository = PureLockRepository(this, db.appLockDao(), db.intruderDao(), db.logDao(), db.scheduleRuleDao(), db.encryptedVaultDao(), db.userSettingDao(), prefs)

        lockedPackageState.value = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)?.takeIf { it.isNotBlank() } ?: packageName

        setContent {
            val lockedPackage by lockedPackageState
            val dsTheme by repository.userSettings.themeMode.collectAsState(initial = "SYSTEM")
            val roomTheme by repository.roomThemeMode.collectAsState(initial = null)
            val activeThemeMode = roomTheme ?: dsTheme

            PureLockTheme(themeMode = activeThemeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LockOverlayScreen(
                        packageName = lockedPackage,
                        repository = repository,
                        onUnlocked = {
                            com.example.service.PureLockAccessibilityService.onPackageUnlocked(lockedPackage)
                            lifecycleScope.launch {
                                try {
                                    repository.updateLastUnlocked(lockedPackage)
                                } finally {
                                    finish()
                                }
                            }
                        },
                        onCancelled = {
                            navigateHomeSafely()
                        }
                    )
                }
            }
        }
    }

    private fun navigateHomeSafely() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {
        } finally {
            finish()
        }
    }
}
