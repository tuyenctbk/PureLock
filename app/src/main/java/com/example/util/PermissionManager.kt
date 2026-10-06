package com.example.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.example.R
import com.example.service.PureLockAccessibilityService

enum class PermissionPriority {
    CORE_REQUIRED,
    RECOMMENDED,
    FEATURE_OPTIONAL
}

data class PermissionGuideItem(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val priority: PermissionPriority,
    val isGranted: Boolean,
    val stepStrings: List<Int>,
    val openAction: (Context) -> Unit
)

object PermissionManager {

    /**
     * Checks if PureLockAccessibilityService is explicitly enabled in Android Accessibility settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${PureLockAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                (componentName.contains(context.packageName) && componentName.contains("PureLockAccessibilityService"))
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if Display Over Other Apps (Overlay / SYSTEM_ALERT_WINDOW) is granted.
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Checks if Usage Stats permission is granted.
     */
    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks if Camera permission is granted (used for silent intruder selfie).
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if Post Notifications permission is granted (Android 13+).
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Core privileges: Accessibility & Overlay are required for locked apps to be blocked.
     */
    fun areCorePermissionsGranted(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context) && isOverlayPermissionGranted(context)
    }

    @Volatile
    var lastSettingsOpenedTimestamp: Long = 0L

    fun recordSettingsOpened() {
        lastSettingsOpenedTimestamp = System.currentTimeMillis()
    }

    fun isReturningFromSettings(): Boolean {
        val elapsed = System.currentTimeMillis() - lastSettingsOpenedTimestamp
        return elapsed in 0L..120_000L // 2-minute grace period when user went to system settings
    }

    fun clearReturningFromSettings() {
        lastSettingsOpenedTimestamp = 0L
    }

    /**
     * Launches Accessibility settings screen.
     */
    fun openAccessibilitySettings(context: Context) {
        recordSettingsOpened()
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    /**
     * Launches Display Over Other Apps settings screen directly targeting PureLock.
     */
    fun openOverlaySettings(context: Context) {
        recordSettingsOpened()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    openAppSettings(context)
                }
            }
        }
    }

    /**
     * Launches Usage Access settings screen.
     */
    fun openUsageAccessSettings(context: Context) {
        recordSettingsOpened()
        try {
            val intent = Intent(
                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                openAppSettings(context)
            }
        }
    }

    /**
     * Launches App Details Settings (fallback for Camera, Notifications, etc.)
     */
    fun openAppSettings(context: Context) {
        recordSettingsOpened()
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns full guide items list with live evaluation.
     */
    fun getGuideItems(context: Context): List<PermissionGuideItem> {
        return listOf(
            PermissionGuideItem(
                id = "accessibility",
                titleRes = R.string.perm_guide_acc_name,
                descRes = R.string.perm_guide_acc_desc,
                priority = PermissionPriority.CORE_REQUIRED,
                isGranted = isAccessibilityServiceEnabled(context),
                stepStrings = listOf(
                    R.string.perm_guide_acc_step_1,
                    R.string.perm_guide_acc_step_2,
                    R.string.perm_guide_acc_step_3,
                    R.string.perm_guide_acc_step_4
                ),
                openAction = { openAccessibilitySettings(it) }
            ),
            PermissionGuideItem(
                id = "overlay",
                titleRes = R.string.perm_guide_overlay_name,
                descRes = R.string.perm_guide_overlay_desc,
                priority = PermissionPriority.CORE_REQUIRED,
                isGranted = isOverlayPermissionGranted(context),
                stepStrings = listOf(
                    R.string.perm_guide_overlay_step_1,
                    R.string.perm_guide_overlay_step_2,
                    R.string.perm_guide_overlay_step_3
                ),
                openAction = { openOverlaySettings(it) }
            ),
            PermissionGuideItem(
                id = "usage",
                titleRes = R.string.perm_guide_usage_name,
                descRes = R.string.perm_guide_usage_desc,
                priority = PermissionPriority.RECOMMENDED,
                isGranted = isUsageAccessGranted(context),
                stepStrings = listOf(
                    R.string.perm_guide_usage_step_1,
                    R.string.perm_guide_usage_step_2,
                    R.string.perm_guide_usage_step_3
                ),
                openAction = { openUsageAccessSettings(it) }
            ),
            PermissionGuideItem(
                id = "camera",
                titleRes = R.string.perm_guide_camera_name,
                descRes = R.string.perm_guide_camera_desc,
                priority = PermissionPriority.FEATURE_OPTIONAL,
                isGranted = isCameraPermissionGranted(context),
                stepStrings = emptyList(),
                openAction = { openAppSettings(it) }
            ),
            PermissionGuideItem(
                id = "notification",
                titleRes = R.string.perm_guide_notif_name,
                descRes = R.string.perm_guide_notif_desc,
                priority = PermissionPriority.RECOMMENDED,
                isGranted = isNotificationPermissionGranted(context),
                stepStrings = emptyList(),
                openAction = { openAppSettings(it) }
            )
        )
    }
}
