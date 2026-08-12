package com.example.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object FirebaseManager {
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var remoteConfig: FirebaseRemoteConfig? = null

    fun initialize(context: Context) {
        try {
            analytics = FirebaseAnalytics.getInstance(context)
            crashlytics = FirebaseCrashlytics.getInstance()
            remoteConfig = FirebaseRemoteConfig.getInstance()

            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig?.setConfigSettingsAsync(configSettings)

            val defaults = mapOf(
                "min_apps_locked_for_share_prompt" to 3L,
                "min_unlocks_for_share_prompt" to 5L,
                "min_protection_score_for_rate_prompt" to 100L,
                "enable_share_prompt" to true,
                "enable_rate_prompt" to true
            )
            remoteConfig?.setDefaultsAsync(defaults)
            remoteConfig?.fetchAndActivate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logEvent(eventName: String, params: Map<String, String> = emptyMap()) {
        try {
            val bundle = Bundle().apply {
                params.forEach { (key, value) ->
                    putString(key, value)
                }
            }
            analytics?.logEvent(eventName, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logSecurityAction(action: String, status: String = "SUCCESS") {
        logEvent(
            "security_action",
            mapOf(
                "action_name" to action,
                "status" to status
            )
        )
    }

    fun logScreenView(screenName: String) {
        logEvent(
            "screen_view_custom",
            mapOf("screen_name" to screenName)
        )
    }

    fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMinAppsLockedForSharePrompt(): Int {
        return remoteConfig?.getLong("min_apps_locked_for_share_prompt")?.toInt() ?: 3
    }

    fun getMinUnlocksForSharePrompt(): Int {
        return remoteConfig?.getLong("min_unlocks_for_share_prompt")?.toInt() ?: 5
    }

    fun isSharePromptEnabled(): Boolean {
        return remoteConfig?.getBoolean("enable_share_prompt") ?: true
    }

    fun isRatePromptEnabled(): Boolean {
        return remoteConfig?.getBoolean("enable_rate_prompt") ?: true
    }
}
