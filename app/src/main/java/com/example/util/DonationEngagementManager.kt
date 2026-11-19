package com.example.util

/**
 * Pure, deterministic engagement engine controlling donation banners and post-operation prompts.
 *
 * Guarantees non-intrusive UX:
 * - Never shown on app start / splash / first launch.
 * - Permanently disabled once the user becomes an Active Supporter.
 * - Requires verified utility (minimum days installed, minimum launches, minimum successful tasks).
 * - Respects 14-day banner dismiss cooldown, 30-day prompt cooldown, and lifetime prompt cap.
 */
data class DonationEngagementState(
    val firstInstallTimestamp: Long = 0L,
    val appLaunchCount: Int = 0,
    val successfulOperationsCount: Int = 0,
    val lastDonationPromptTimestamp: Long = 0L,
    val donationPromptShownCount: Int = 0,
    val donationBannerDismissedAt: Long = 0L,
    val isActiveSupporter: Boolean = false,
    val supporterTier: String = ""
)

object DonationEngagementManager {

    const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    const val MIN_DAYS_SINCE_INSTALL: Long = 2L
    const val MIN_INSTALL_AGE_MS: Long = MIN_DAYS_SINCE_INSTALL * DAY_MS

    const val MIN_LAUNCH_COUNT: Int = 3
    const val MIN_OPS_FOR_BANNER: Int = 3
    const val MIN_OPS_FOR_AUTO_PROMPT: Int = 7

    const val BANNER_DISMISS_COOLDOWN_MS: Long = 14L * DAY_MS
    const val PROMPT_COOLDOWN_MS: Long = 30L * DAY_MS
    const val MAX_LIFETIME_AUTO_PROMPTS: Int = 3

    val MILESTONE_COUNTS: Set<Int> = setOf(7, 10, 25, 50, 100)

    fun hasSatisfiedInstallAge(firstInstallTimestamp: Long, nowMs: Long): Boolean {
        if (firstInstallTimestamp <= 0L) return false
        return (nowMs - firstInstallTimestamp) >= MIN_INSTALL_AGE_MS
    }

    fun shouldShowHomeBanner(
        state: DonationEngagementState,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (state.isActiveSupporter) return false
        if (!hasSatisfiedInstallAge(state.firstInstallTimestamp, nowMs)) return false
        if (state.appLaunchCount < MIN_LAUNCH_COUNT) return false
        if (state.successfulOperationsCount < MIN_OPS_FOR_BANNER) return false

        val dismissedAt = state.donationBannerDismissedAt
        if (dismissedAt > 0L && (nowMs - dismissedAt) < BANNER_DISMISS_COOLDOWN_MS) {
            return false
        }
        return true
    }

    fun shouldShowPostOperationPrompt(
        state: DonationEngagementState,
        sessionAlreadyPrompted: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (state.isActiveSupporter) return false
        if (sessionAlreadyPrompted) return false
        if (!hasSatisfiedInstallAge(state.firstInstallTimestamp, nowMs)) return false
        if (state.appLaunchCount < MIN_LAUNCH_COUNT) return false
        if (state.successfulOperationsCount < MIN_OPS_FOR_AUTO_PROMPT) return false
        if (state.donationPromptShownCount >= MAX_LIFETIME_AUTO_PROMPTS) return false

        val lastPrompt = state.lastDonationPromptTimestamp
        if (lastPrompt > 0L && (nowMs - lastPrompt) < PROMPT_COOLDOWN_MS) {
            return false
        }
        return true
    }

    fun getMilestoneReached(previousCount: Int, newCount: Int): Int? {
        if (newCount <= previousCount) return null
        return MILESTONE_COUNTS.firstOrNull { threshold ->
            threshold in (previousCount + 1)..newCount
        }
    }
}
