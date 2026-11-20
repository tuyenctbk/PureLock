package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.PureLockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VaultWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LOCK_VAULT = "com.example.ACTION_LOCK_VAULT"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
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
                    val prefs = com.example.data.PureLockPreferences(appContext)
                    val repository = com.example.data.PureLockRepository(
                        appContext,
                        db.appLockDao(),
                        db.intruderDao(),
                        db.logDao(),
                        db.scheduleRuleDao(),
                        db.encryptedVaultDao(),
                        db.userSettingDao(),
                        prefs
                    )
                    // 1. Immediately revoke all active sessions and reset unlock timestamps
                    repository.lockAllAndClearSessions()
                    com.example.util.ClipboardSecurityManager.clearClipboard(appContext, showNotification = false)

                    val lockedCount = db.appLockDao().getActiveLockedAppsCount().first()
                    val vaultCount = db.encryptedVaultDao().getActiveVaultItems().first().size
                    val totalProtected = lockedCount + vaultCount

                    val appWidgetManager = AppWidgetManager.getInstance(appContext)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(appContext, VaultWidgetProvider::class.java)
                    )
                    val lockIntent = Intent(appContext, VaultWidgetProvider::class.java).apply {
                        action = ACTION_LOCK_VAULT
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext, 0, lockIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(appContext.packageName, R.layout.vault_widget)
                        views.setTextViewText(
                            R.id.tv_protected_count,
                            appContext.getString(R.string.widget_vault_secured_format, totalProtected)
                        )
                        views.setOnClickPendingIntent(R.id.btn_lock_widget, pendingIntent)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.vault_widget)

        val db = PureLockDatabase.getDatabase(context)
        val lockedCount = db.appLockDao().getActiveLockedAppsCount().first()
        val vaultCount = db.encryptedVaultDao().getActiveVaultItems().first().size
        val totalProtected = lockedCount + vaultCount

        views.setTextViewText(
            R.id.tv_protected_count,
            context.getString(R.string.widget_protected_items_format, totalProtected, lockedCount, vaultCount)
        )

        val intent = Intent(context, VaultWidgetProvider::class.java).apply {
            action = ACTION_LOCK_VAULT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_lock_widget, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
