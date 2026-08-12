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
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOCK_VAULT) {
            val pendingResult = goAsync()
            val db = PureLockDatabase.getDatabase(context)
            scope.launch {
                try {
                    val lockedApps = db.appLockDao().getAllLockedApps().first()
                    val lockedCount = lockedApps.count { it.isLocked }
                    val vaultCount = db.encryptedVaultDao().getAllVaultItems().first().size
                    val totalProtected = lockedCount + vaultCount

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, VaultWidgetProvider::class.java)
                    )
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.vault_widget)
                        views.setTextViewText(R.id.tv_protected_count, "Vault Locked! Protected Items: $totalProtected")
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

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.vault_widget)

        scope.launch {
            val db = PureLockDatabase.getDatabase(context)
            val lockedCount = db.appLockDao().getAllLockedApps().first().count { it.isLocked }
            val vaultCount = db.encryptedVaultDao().getAllVaultItems().first().size
            val totalProtected = lockedCount + vaultCount

            views.setTextViewText(R.id.tv_protected_count, "Protected Items: $totalProtected (Apps: $lockedCount, Vault: $vaultCount)")

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
}
