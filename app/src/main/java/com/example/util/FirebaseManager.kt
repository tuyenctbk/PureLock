package com.example.util

import android.content.Context

/**
 * Strictly offline, zero-telemetry facade preserving PureLock's 100% local privacy guarantee.
 * No analytics, crash dumps, advertising identifiers, or remote config requests ever leave the device.
 */
object FirebaseManager {
    private const val DEFAULT_MIN_APPS_LOCKED_FOR_SHARE = 3
    private const val DEFAULT_MIN_UNLOCKS_FOR_SHARE = 5

    @Suppress("UNUSED_PARAMETER")
    fun initialize(context: Context) {
        // Intentionally no-op: zero cloud telemetry or network initialization.
    }

    @Suppress("UNUSED_PARAMETER")
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        // Intentionally no-op: all security audit records stay strictly inside local SQLCipher Room DB.
    }

    @Suppress("UNUSED_PARAMETER")
    fun logSecurityAction(action: String, status: String = "SUCCESS") {
        // Intentionally no-op.
    }

    @Suppress("UNUSED_PARAMETER")
    fun logScreenView(screenName: String) {
        // Intentionally no-op.
    }

    @Suppress("UNUSED_PARAMETER")
    fun recordException(throwable: Throwable) {
        // Intentionally no-op.
    }

    @Suppress("UNUSED_PARAMETER")
    fun setCustomKey(key: String, value: String) {
        // Intentionally no-op.
    }

    fun getMinAppsLockedForSharePrompt(): Int = DEFAULT_MIN_APPS_LOCKED_FOR_SHARE

    fun getMinUnlocksForSharePrompt(): Int = DEFAULT_MIN_UNLOCKS_FOR_SHARE

    fun isSharePromptEnabled(): Boolean = true

    fun isRatePromptEnabled(): Boolean = true
}
