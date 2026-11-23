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
import com.example.R
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    companion object {
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == appContext.packageName) return

        val pendingResult = goAsync()
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        if (intent.action == Intent.ACTION_PACKAGE_REMOVED || intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            // App updates fire ACTION_PACKAGE_REMOVED with EXTRA_REPLACING=true before ACTION_PACKAGE_ADDED.
            // Never remove the app or wipe its lock state during an update.
            if (isReplacing) {
                pendingResult.finish()
                return
            }
            receiverScope.launch {
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
                    repository.removeUninstalledApp(packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            receiverScope.launch {
                try {
                    val pm = appContext.packageManager
                    val isLaunchable = pm.getLaunchIntentForPackage(packageName) != null ||
                        pm.getLeanbackLaunchIntentForPackage(packageName) != null
                    if (!isLaunchable) {
                        return@launch
                    }

                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()

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

                    val existing = db.appLockDao().getLockedAppByPackage(packageName)
                    if (existing != null) {
                        // Preserve lock state, category, and stats during app update or re-index; refresh label only
                        db.appLockDao().upsertApp(existing.copy(appName = appName))
                        return@launch
                    }

                    val category = repository.categorizeApp(packageName, appName)
                    val autoLock = category in listOf("FINANCIAL", "SOCIAL")
                    repository.addNewlyInstalledApp(packageName, appName, category, autoLock)
                    VaultWidgetProvider.requestUpdate(appContext)

                    if (!isReplacing) {
                        sendNewAppNotification(appContext, packageName, appName, autoLock)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
        }
    }

    private fun sendNewAppNotification(context: Context, packageName: String, appName: String, isAlreadyLocked: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "purelock_new_app_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.new_app_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.new_app_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("shortcut_action", "VIEW_SHIELD")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.new_app_notif_title, appName)
        val text = if (isAlreadyLocked) {
            context.getString(R.string.new_app_notif_locked_text, appName)
        } else {
            context.getString(R.string.new_app_notif_prompt_text, appName)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                notificationManager.notify(packageName.hashCode(), builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
