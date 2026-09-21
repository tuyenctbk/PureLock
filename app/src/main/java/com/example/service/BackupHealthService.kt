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
import com.example.data.PureLockPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupHealthService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "backup_health_channel"
        private const val NOTIFICATION_ID = 9988
    }

    fun checkBackupHealth() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PureLockPreferences(context)
                val lastBackupTime = prefs.getLastBackupTimestamp()
                val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
                val currentTime = System.currentTimeMillis()

                if (lastBackupTime == 0L) {
                    // First run: establish initial baseline timestamp
                    prefs.setLastBackupTimestamp(currentTime)
                } else if ((currentTime - lastBackupTime) > thirtyDaysMillis) {
                    triggerBackupWarningNotification()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun triggerBackupWarningNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.backup_health_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.backup_health_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("shortcut_action", "VIEW_VAULT")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.splash_logo)
            .setContentTitle(context.getString(R.string.backup_health_notif_title))
            .setContentText(context.getString(R.string.backup_overdue_notif_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
