package com.example.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.example.service.PureLockAccessibilityService

/**
 * PermissionUtils provides reliable, centralized inspection and intent launching
 * for PureLock's core privacy and security services.
 */
object PermissionUtils {

    /**
     * Checks whether PureLockAccessibilityService is currently enabled in system accessibility settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return PermissionManager.isAccessibilityServiceEnabled(context)
    }

    /**
     * Checks whether the app has SYSTEM_ALERT_WINDOW (Display over other apps) permission.
     * Required on Android 10+ to present LockOverlayActivity over third-party applications.
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return PermissionManager.isOverlayPermissionGranted(context)
    }

    /**
     * Checks whether notification permissions are granted (Android 13+ / API 33).
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return PermissionManager.isNotificationPermissionGranted(context)
    }

    /**
     * Launches System Accessibility Settings screen.
     */
    fun openAccessibilitySettings(context: Context): Boolean {
        PermissionManager.recordSettingsOpened()
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Launches Display Over Other Apps (Overlay) permission settings screen.
     */
    fun openOverlaySettings(context: Context): Boolean {
        PermissionManager.recordSettingsOpened()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } else {
                true
            }
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    /**
     * Launches PureLock App Info screen in system settings.
     * Essential for guiding users on Android 13+ to unlock "Restricted settings"
     * via the three vertical dots (⋮) in the top-right corner.
     */
    fun openAppInfoSettings(context: Context): Boolean {
        PermissionManager.recordSettingsOpened()
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
