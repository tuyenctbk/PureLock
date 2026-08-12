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

                if (lastBackupTime == 0L || (currentTime - lastBackupTime) > thirtyDaysMillis) {
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
                "Backup Health Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when no encrypted backup has been performed in over 30 days."
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
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Backup Health Warning")
            .setContentText("No encrypted backup performed in over 30 days! Tap to backup now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
