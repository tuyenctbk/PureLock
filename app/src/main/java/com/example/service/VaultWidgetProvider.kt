package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VaultWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LOCK_VAULT = "com.example.ACTION_LOCK_VAULT"
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun requestUpdate(context: Context) {
            try {
                val appContext = context.applicationContext
                val manager = AppWidgetManager.getInstance(appContext) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(appContext, VaultWidgetProvider::class.java))
                if (ids.isEmpty()) return
                val intent = Intent(appContext, VaultWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appContext.sendBroadcast(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                val db = PureLockDatabase.getDatabase(appContext)
                val lockedCount = db.appLockDao().getActiveLockedAppsCountSync()
                val vaultCount = db.encryptedVaultDao().getActiveVaultItemsCountSync()
                for (appWidgetId in appWidgetIds) {
                    applyWidgetViews(appContext, appWidgetManager, appWidgetId, lockedCount, vaultCount, securedNow = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOCK_VAULT) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            scope.launch {
                try {
                    val db = PureLockDatabase.getDatabase(appContext)
                    val prefs = PureLockPreferences(appContext)
                    val repository = PureLockRepository(
                        appContext,
                        db.appLockDao(),
                        db.intruderDao(),
                        db.logDao(),
                        db.scheduleRuleDao(),
                        db.encryptedVaultDao(),
                        db.userSettingDao(),
                        prefs
                    )
                    // Immediately revoke all active sessions, lock in-app vault, and clear clipboard
                    repository.lockAllAndClearSessions()
                    com.example.util.ClipboardSecurityManager.clearClipboard(appContext, showNotification = false)

                    val lockedCount = db.appLockDao().getActiveLockedAppsCountSync()
                    val vaultCount = db.encryptedVaultDao().getActiveVaultItemsCountSync()

                    val appWidgetManager = AppWidgetManager.getInstance(appContext)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(appContext, VaultWidgetProvider::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        applyWidgetViews(appContext, appWidgetManager, appWidgetId, lockedCount, vaultCount, securedNow = true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun applyWidgetViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        lockedCount: Int,
        vaultCount: Int,
        securedNow: Boolean
    ) {
        val views = RemoteViews(context.packageName, R.layout.vault_widget)
        val totalProtected = lockedCount + vaultCount

        views.setTextViewText(
            R.id.tv_protected_count,
            if (securedNow) {
                context.getString(R.string.widget_vault_secured_format, totalProtected)
            } else {
                context.getString(R.string.widget_protected_items_format, totalProtected, lockedCount, vaultCount)
            }
        )

        val lockIntent = Intent(context, VaultWidgetProvider::class.java).apply {
            action = ACTION_LOCK_VAULT
        }
        val lockPendingIntent = PendingIntent.getBroadcast(
            context, 0, lockIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_lock_widget, lockPendingIntent)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("shortcut_action", "VIEW_VAULT")
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 1, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tv_protected_count, openPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
