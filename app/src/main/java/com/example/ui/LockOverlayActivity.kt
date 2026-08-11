package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.ui.theme.PureLockTheme
import kotlinx.coroutines.launch

import androidx.fragment.app.FragmentActivity

class LockOverlayActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
    }

    private lateinit var repository: PureLockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        val db = PureLockDatabase.getDatabase(this)
        val prefs = PureLockPreferences(this)
        repository = PureLockRepository(this, db.appLockDao(), db.intruderDao(), db.logDao(), db.scheduleRuleDao(), db.encryptedVaultDao(), prefs)

        val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: "com.android.settings"

        setContent {
            PureLockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LockOverlayScreen(
                        packageName = lockedPackage,
                        repository = repository,
                        onUnlocked = {
                            lifecycleScope.launch {
                                repository.updateLastUnlocked(lockedPackage)
                                finish()
                            }
                        },
                        onCancelled = {
                            // Return to home screen
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
