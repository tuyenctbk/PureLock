package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BackupHealthService(context: Context) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val CHANNEL_ID = "backup_health_channel"
        private const val NOTIFICATION_ID = 9988
        private const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
        private const val WARNING_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1000L
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun checkBackupHealth() {
        scope.launch {
            try {
                val prefs = PureLockPreferences(appContext)
                val lastBackupTime = prefs.getLastBackupTimestamp()
                val firstInstallTime = prefs.getFirstInstallTimestamp()
                val lastWarnedTime = prefs.getLastBackupWarningTimestamp()
                val currentTime = System.currentTimeMillis()

                // Never fabricate a fake backup timestamp on fresh installs; measure from real backup or install date
                val baselineTime = if (lastBackupTime > 0L) lastBackupTime else firstInstallTime
                if (baselineTime <= 0L) return@launch
                if ((currentTime - baselineTime) <= THIRTY_DAYS_MS) return@launch
                if (lastWarnedTime > 0L && (currentTime - lastWarnedTime) < WARNING_COOLDOWN_MS) return@launch

                // Only warn if user actually has vault secrets or locked apps worth backing up
                val db = PureLockDatabase.getDatabase(appContext)
                val vaultCount = db.encryptedVaultDao().getActiveVaultItemsCountSync()
                val lockedCount = db.appLockDao().getActiveLockedAppsCountSync()
                if (vaultCount == 0 && lockedCount == 0) return@launch

                if (triggerBackupWarningNotification()) {
                    prefs.setLastBackupWarningTimestamp(currentTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerBackupWarningNotification(): Boolean {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.backup_health_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = appContext.getString(R.string.backup_health_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("shortcut_action", "VIEW_SETTINGS")
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(appContext.getString(R.string.backup_health_notif_title))
            .setContentText(appContext.getString(R.string.backup_overdue_notif_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            return try {
                notificationManager.notify(NOTIFICATION_ID, notification)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }
}
