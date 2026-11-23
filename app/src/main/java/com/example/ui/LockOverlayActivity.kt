package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.service.PureLockAccessibilityService
import com.example.service.PureLockAutofillService
import com.example.ui.theme.PureLockTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockOverlayActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val EXTRA_AUTOFILL_AUTH_REQUEST = "extra_autofill_auth_request"
        const val EXTRA_AUTOFILL_USERNAME_IDS = "extra_autofill_username_ids"
        const val EXTRA_AUTOFILL_PASSWORD_IDS = "extra_autofill_password_ids"
        const val EXTRA_AUTOFILL_WEB_DOMAINS = "extra_autofill_web_domains"
        const val EXTRA_AUTOFILL_TARGET_PACKAGE = "extra_autofill_target_package"
    }

    private lateinit var repository: PureLockRepository
    private var lockedPackageState = mutableStateOf("")
    private var isAutofillAuthRequestState = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        isAutofillAuthRequestState.value = intent.getBooleanExtra(EXTRA_AUTOFILL_AUTH_REQUEST, false)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()

        isAutofillAuthRequestState.value = intent.getBooleanExtra(EXTRA_AUTOFILL_AUTH_REQUEST, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAutofillAuthRequestState.value) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                } else {
                    navigateHomeSafely()
                }
            }
        })

        val appCtx = applicationContext
        val db = PureLockDatabase.getDatabase(appCtx)
        val prefs = PureLockPreferences(appCtx)
        repository = PureLockRepository(
            appCtx,
            db.appLockDao(),
            db.intruderDao(),
            db.logDao(),
            db.scheduleRuleDao(),
            db.encryptedVaultDao(),
            db.userSettingDao(),
            prefs
        )

        lockedPackageState.value = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)?.takeIf { it.isNotBlank() } ?: packageName

        setContent {
            val lockedPackage by lockedPackageState
            val isAutofillAuth by isAutofillAuthRequestState
            val dsTheme by repository.userSettings.themeMode.collectAsState(initial = "SYSTEM")
            val roomTheme by repository.roomThemeMode.collectAsState(initial = null)
            val activeThemeMode = roomTheme ?: dsTheme

            PureLockTheme(themeMode = activeThemeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LockOverlayScreen(
                        packageName = lockedPackage,
                        repository = repository,
                        onUnlocked = {
                            if (isAutofillAuth && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                completeAutofillAuthentication()
                            } else {
                                PureLockAccessibilityService.onPackageUnlocked(lockedPackage)
                                lifecycleScope.launch {
                                    try {
                                        repository.updateLastUnlocked(lockedPackage)
                                    } finally {
                                        finish()
                                    }
                                }
                            }
                        },
                        onCancelled = {
                            if (isAutofillAuth) {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            } else {
                                navigateHomeSafely()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun completeAutofillAuthentication() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val usernameIds = IntentCompat.getParcelableArrayListExtra(
            intent,
            EXTRA_AUTOFILL_USERNAME_IDS,
            AutofillId::class.java
        ) ?: emptyList()
        val passwordIds = IntentCompat.getParcelableArrayListExtra(
            intent,
            EXTRA_AUTOFILL_PASSWORD_IDS,
            AutofillId::class.java
        ) ?: emptyList()
        val webDomains = intent.getStringArrayListExtra(EXTRA_AUTOFILL_WEB_DOMAINS)?.toSet() ?: emptySet()
        val targetPackage = intent.getStringExtra(EXTRA_AUTOFILL_TARGET_PACKAGE) ?: ""

        lifecycleScope.launch {
            val fillResponse = withContext(Dispatchers.IO) {
                try {
                    val items = repository.activeVaultItems.first()
                    val response = PureLockAutofillService.buildAuthenticatedFillResponse(
                        context = applicationContext,
                        vaultItems = items,
                        usernameIds = usernameIds,
                        passwordIds = passwordIds,
                        webDomains = webDomains,
                        targetPackage = targetPackage
                    )
                    if (response != null) {
                        repository.logSecurityEvent(
                            "AUTOFILL_REQUEST",
                            "Authenticated offline autofill datasets provided for ${targetPackage.ifBlank { "app" }}."
                        )
                    }
                    response
                } catch (_: Exception) {
                    null
                }
            }

            if (fillResponse != null) {
                val reply = Intent().apply {
                    putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
                }
                setResult(Activity.RESULT_OK, reply)
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
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
