package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.R
import com.example.service.findActivity

object SmartPromptsManager {

    private const val SHARE_COOLDOWN_MS = 86400000L // 24 hours
    private const val RATE_COOLDOWN_MS = 172800000L // 48 hours

    fun shouldShowSharePrompt(
        lockedAppsCount: Int,
        totalVaultUnlocks: Int,
        hasShared: Boolean,
        promptCount: Int,
        lastTimestamp: Long
    ): Boolean {
        if (!FirebaseManager.isSharePromptEnabled()) return false
        if (hasShared) return false
        if (promptCount >= 3) return false

        val now = System.currentTimeMillis()
        if (now - lastTimestamp < SHARE_COOLDOWN_MS) return false

        val minApps = FirebaseManager.getMinAppsLockedForSharePrompt()
        val minUnlocks = FirebaseManager.getMinUnlocksForSharePrompt()

        return lockedAppsCount >= minApps || totalVaultUnlocks >= minUnlocks
    }

    fun shouldShowRatePrompt(
        protectionScore: Int,
        lockedAppsCount: Int,
        hasRated: Boolean,
        promptCount: Int,
        lastTimestamp: Long
    ): Boolean {
        if (!FirebaseManager.isRatePromptEnabled()) return false
        if (hasRated) return false
        if (promptCount >= 3) return false

        val now = System.currentTimeMillis()
        if (now - lastTimestamp < RATE_COOLDOWN_MS) return false

        return protectionScore >= 100 && lockedAppsCount >= 1
    }

    fun shareApp(context: Context) {
        val packageName = context.packageName
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                context.getString(R.string.share_app_subject)
            )
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_app_text_format, packageName)
            )
        }
        val hostActivity = context.findActivity()
        try {
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_app_chooser_title)).apply {
                if (hostActivity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            (hostActivity ?: context).startActivity(chooser)
            FirebaseManager.logEvent("share_app_action")
        } catch (_: Exception) {
        }
    }

    fun openPlayStoreForRating(context: Context) {
        val packageName = context.packageName
        val hostActivity = context.findActivity()
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                if (hostActivity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            (hostActivity ?: context).startActivity(marketIntent)
            FirebaseManager.logEvent("rate_app_action")
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    if (hostActivity == null) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                (hostActivity ?: context).startActivity(webIntent)
                FirebaseManager.logEvent("rate_app_action")
            } catch (_: Exception) {
            }
        }
    }
}
