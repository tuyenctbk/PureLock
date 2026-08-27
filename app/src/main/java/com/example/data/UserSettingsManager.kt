package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "purelock_user_settings")

/**
 * UserSettingsManager manages all persistent user preferences using Jetpack DataStore.
 * Provides reactive Flows and suspend mutating functions for settings like auto-lock intervals,
 * biometric authentication status, dynamic theming, and security policies.
 */
class UserSettingsManager(private val context: Context) {

    companion object {
        // Security & Authentication Keys
        val KEY_AUTO_LOCK_INACTIVITY_SEC = intPreferencesKey("auto_lock_inactivity_sec")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_SECURITY_TYPE = stringPreferencesKey("security_type") // "PIN", "PATTERN", "BIOMETRIC"
        val KEY_MASTER_PIN = stringPreferencesKey("master_pin")
        val KEY_MASTER_PATTERN = stringPreferencesKey("master_pattern")
        val KEY_DURESS_PIN = stringPreferencesKey("duress_pin")
        val KEY_GRACE_PERIOD_MS = longPreferencesKey("grace_period_ms")
        val KEY_RANDOM_KEYBOARD = booleanPreferencesKey("random_keyboard")
        val KEY_HIDE_PATTERN_PATH = booleanPreferencesKey("hide_pattern_path")
        val KEY_SHAKE_TO_LOCK = booleanPreferencesKey("shake_to_lock")
        val KEY_INTRUDER_CAPTURE = booleanPreferencesKey("intruder_capture")
        val KEY_STEALTH_DECOY = booleanPreferencesKey("stealth_decoy")
        val KEY_STEALTH_MODE_ACTIVE = booleanPreferencesKey("stealth_mode_active")
        val KEY_BIOMETRIC_SETTINGS_SECURED = booleanPreferencesKey("biometric_settings_secured")

        // Theme & UI Keys
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK", "AMOLED"
        val KEY_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val KEY_SOUND_FEEDBACK_ENABLED = booleanPreferencesKey("sound_feedback_enabled")
        val KEY_TV_MODE = booleanPreferencesKey("tv_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order")
        val KEY_TRASH_PURGE_DAYS = intPreferencesKey("trash_purge_days")
        val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val KEY_CLIPBOARD_AUTO_CLEAR_SEC = intPreferencesKey("clipboard_auto_clear_sec")
    }

    private val dataStore: DataStore<Preferences> = context.userSettingsDataStore

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // --- Flows for observing settings ---

    val autoLockInactivitySec: Flow<Int> = safeData.map { prefs ->
        prefs[KEY_AUTO_LOCK_INACTIVITY_SEC] ?: 60 // Default 60 seconds
    }

    val biometricEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: true
    }

    val securityType: Flow<String> = safeData.map { prefs ->
        prefs[KEY_SECURITY_TYPE] ?: "PIN"
    }

    val masterPin: Flow<String> = safeData.map { prefs ->
        prefs[KEY_MASTER_PIN] ?: "1234"
    }

    val masterPattern: Flow<String> = safeData.map { prefs ->
        prefs[KEY_MASTER_PATTERN] ?: "1,2,5,8,9"
    }

    val duressPin: Flow<String> = safeData.map { prefs ->
        prefs[KEY_DURESS_PIN] ?: "0000"
    }

    val gracePeriodMs: Flow<Long> = safeData.map { prefs ->
        prefs[KEY_GRACE_PERIOD_MS] ?: 30000L
    }

    val randomKeyboard: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_RANDOM_KEYBOARD] ?: false
    }

    val hidePatternPath: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_HIDE_PATTERN_PATH] ?: false
    }

    val shakeToLock: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHAKE_TO_LOCK] ?: true
    }

    val intruderCapture: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_INTRUDER_CAPTURE] ?: true
    }

    val stealthDecoy: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_STEALTH_DECOY] ?: false
    }

    val stealthModeActive: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_STEALTH_MODE_ACTIVE] ?: false
    }

    val biometricSettingsSecured: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_BIOMETRIC_SETTINGS_SECURED] ?: false
    }

    val themeMode: Flow<String> = safeData.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val dynamicColorEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR_ENABLED] ?: false
    }

    val hapticFeedbackEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true
    }

    val soundFeedbackEnabled: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SOUND_FEEDBACK_ENABLED] ?: false
    }

    val tvMode: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_TV_MODE] ?: false
    }

    val appLanguage: Flow<String> = safeData.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "SYSTEM"
    }

    val dashboardCardOrder: Flow<List<String>> = safeData.map { prefs ->
        val raw = prefs[KEY_DASHBOARD_CARD_ORDER] ?: "TIP,TREND,STATS,BATTERY,TRANSPARENCY,APPS"
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val trashPurgeDays: Flow<Int> = safeData.map { prefs ->
        prefs[KEY_TRASH_PURGE_DAYS] ?: 30
    }

    val lastBackupTimestamp: Flow<Long> = safeData.map { prefs ->
        prefs[KEY_LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    val clipboardAutoClearSec: Flow<Int> = safeData.map { prefs ->
        prefs[KEY_CLIPBOARD_AUTO_CLEAR_SEC] ?: 30
    }

    // --- Suspend Mutators ---

    suspend fun setClipboardAutoClearSec(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CLIPBOARD_AUTO_CLEAR_SEC] = seconds
        }
    }

    suspend fun setAutoLockInactivitySec(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_INACTIVITY_SEC] = seconds
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setSecurityType(type: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SECURITY_TYPE] = type
        }
    }

    suspend fun setMasterPin(pin: String) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER_PIN] = pin
        }
    }

    suspend fun setMasterPattern(pattern: String) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER_PATTERN] = pattern
        }
    }

    suspend fun setDuressPin(pin: String) {
        dataStore.edit { prefs ->
            prefs[KEY_DURESS_PIN] = pin
        }
    }

    suspend fun setGracePeriodMs(ms: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_GRACE_PERIOD_MS] = ms
        }
    }

    suspend fun setRandomKeyboard(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_RANDOM_KEYBOARD] = enabled
        }
    }

    suspend fun setHidePatternPath(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HIDE_PATTERN_PATH] = enabled
        }
    }

    suspend fun setShakeToLock(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHAKE_TO_LOCK] = enabled
        }
    }

    suspend fun setIntruderCapture(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_INTRUDER_CAPTURE] = enabled
        }
    }

    suspend fun setStealthDecoy(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_STEALTH_DECOY] = enabled
        }
    }

    suspend fun setStealthModeActive(active: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_STEALTH_MODE_ACTIVE] = active
        }
    }

    suspend fun setBiometricSettingsSecured(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_SETTINGS_SECURED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setSoundFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SOUND_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setTvMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TV_MODE] = enabled
        }
    }

    suspend fun setAppLanguage(lang: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = lang
        }
    }

    suspend fun setDashboardCardOrder(order: List<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_DASHBOARD_CARD_ORDER] = order.joinToString(",")
        }
    }

    suspend fun setTrashPurgeDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_TRASH_PURGE_DAYS] = days
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun getLastBackupTimestamp(): Long = lastBackupTimestamp.first()
}
