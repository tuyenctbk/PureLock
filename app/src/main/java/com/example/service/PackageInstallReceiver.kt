package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            if (packageName == context.packageName) return

            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    val db = PureLockDatabase.getDatabase(context)
                    val prefs = PureLockPreferences(context)
                    val repository = PureLockRepository(
                        context,
                        db.appLockDao(),
                        db.intruderDao(),
                        db.logDao(),
                        db.scheduleRuleDao(),
                        db.encryptedVaultDao(),
                        prefs
                    )

                    val lowerPkg = packageName.lowercase()
                    val lowerName = appName.lowercase()
                    val category = when {
                        lowerPkg.contains("bank") || lowerPkg.contains("pay") || lowerName.contains("bank") -> "FINANCIAL"
                        lowerPkg.contains("chat") || lowerPkg.contains("message") || lowerPkg.contains("social") -> "SOCIAL"
                        lowerPkg.contains("photo") || lowerPkg.contains("gallery") -> "MEDIA"
                        else -> "OTHER"
                    }

                    // Register package in local DB
                    val autoLock = category in listOf("FINANCIAL", "SOCIAL")
                    repository.addNewlyInstalledApp(packageName, appName, category, autoLock)

                    // Send local notification
                    sendNewAppNotification(context, packageName, appName, autoLock)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun sendNewAppNotification(context: Context, packageName: String, appName: String, isAlreadyLocked: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "purelock_new_app_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "New App Installation Detector",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when new applications are installed to offer PureLock protection."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "New App Installed: $appName"
        val text = if (isAlreadyLocked) {
            "PureLock automatically protected $appName based on high-privacy settings."
        } else {
            "Would you like to lock $appName to secure your privacy?"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(packageName.hashCode(), builder.build())
    }
}
