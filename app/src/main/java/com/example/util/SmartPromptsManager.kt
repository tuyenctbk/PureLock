package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri

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
                "PureLock Zero-Knowledge Privacy"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Protect your personal privacy with PureLock! Zero-cloud offline app lock & encrypted vault. Download now: https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PureLock via"))
        FirebaseManager.logEvent("share_app_action")
    }

    fun openPlayStoreForRating(context: Context) {
        val packageName = context.packageName
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(webIntent)
        }
        FirebaseManager.logEvent("rate_app_action")
    }
}
