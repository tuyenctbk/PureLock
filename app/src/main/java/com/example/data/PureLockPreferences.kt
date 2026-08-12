package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "purelock_settings")

class PureLockPreferences(private val context: Context) {

    companion object {
        val KEY_MASTER_PIN = stringPreferencesKey("master_pin")
        val KEY_MASTER_PATTERN = stringPreferencesKey("master_pattern") // "1,2,3,4"
        val KEY_SECURITY_TYPE = stringPreferencesKey("security_type") // "PIN", "PATTERN", "BIOMETRIC"
        val KEY_GRACE_PERIOD_MS = longPreferencesKey("grace_period_ms") // 0L, 30000L, 60000L, 300000L
        val KEY_RANDOM_KEYBOARD = booleanPreferencesKey("random_keyboard")
        val KEY_STEALTH_DECOY = booleanPreferencesKey("stealth_decoy")
        val KEY_HIDE_PATTERN_PATH = booleanPreferencesKey("hide_pattern_path")
        val KEY_INTRUDER_CAPTURE = booleanPreferencesKey("intruder_capture")
        val KEY_TV_MODE = booleanPreferencesKey("tv_mode")
        val KEY_ACCESSIBILITY_ACTIVE = booleanPreferencesKey("accessibility_active")
        val KEY_OVERLAY_ACTIVE = booleanPreferencesKey("overlay_active")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val KEY_INACTIVITY_TIMEOUT_SEC = stringPreferencesKey("inactivity_timeout_sec") // "0", "15", "30", "60", "120", "300"
        val KEY_DASHBOARD_CARD_ORDER = stringPreferencesKey("dashboard_card_order") // "TIP,TREND,STATS,BATTERY,TRANSPARENCY,APPS"
        val KEY_STEALTH_MODE_ACTIVE = booleanPreferencesKey("stealth_mode_active")
        val KEY_BIOMETRIC_SETTINGS_SECURED = booleanPreferencesKey("biometric_settings_secured")
        val KEY_SHAKE_TO_LOCK = booleanPreferencesKey("shake_to_lock")
        val KEY_TRASH_PURGE_DAYS = stringPreferencesKey("trash_purge_days") // "7", "14", "30", "60"
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language") // "SYSTEM", "en", "es"...
        val KEY_LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val KEY_DURESS_PIN = stringPreferencesKey("duress_pin")
    }

    val duressPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DURESS_PIN] ?: "0000"
    }

    val shakeToLock: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHAKE_TO_LOCK] ?: true
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LANGUAGE] ?: "SYSTEM"
    }

    val trashPurgeDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TRASH_PURGE_DAYS]?.toIntOrNull() ?: 30
    }

    val biometricSettingsSecured: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_SETTINGS_SECURED] ?: false
    }

    val dashboardCardOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DASHBOARD_CARD_ORDER] ?: "TIP,TREND,STATS,BATTERY,TRANSPARENCY,APPS"
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val stealthModeActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STEALTH_MODE_ACTIVE] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val inactivityTimeoutSec: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_INACTIVITY_TIMEOUT_SEC]?.toIntOrNull() ?: 60
    }

    val masterPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MASTER_PIN] ?: "1234"
    }

    val masterPattern: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MASTER_PATTERN] ?: "1,2,5,8,9"
    }

    val securityType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SECURITY_TYPE] ?: "PIN"
    }

    val gracePeriodMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRACE_PERIOD_MS] ?: 30000L // Default 30s
    }

    val randomKeyboard: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RANDOM_KEYBOARD] ?: false
    }

    val stealthDecoy: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STEALTH_DECOY] ?: false
    }

    val hidePatternPath: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HIDE_PATTERN_PATH] ?: false
    }

    val intruderCapture: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_INTRUDER_CAPTURE] ?: true
    }

    val tvMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TV_MODE] ?: false
    }

    suspend fun setMasterPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_PIN] = pin
        }
    }

    suspend fun setMasterPattern(pattern: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_PATTERN] = pattern
        }
    }

    suspend fun setSecurityType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SECURITY_TYPE] = type
        }
    }

    suspend fun setGracePeriodMs(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GRACE_PERIOD_MS] = ms
        }
    }

    suspend fun setRandomKeyboard(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RANDOM_KEYBOARD] = enabled
        }
    }

    suspend fun setStealthDecoy(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STEALTH_DECOY] = enabled
        }
    }

    suspend fun setHidePatternPath(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HIDE_PATTERN_PATH] = enabled
        }
    }

    suspend fun setIntruderCapture(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INTRUDER_CAPTURE] = enabled
        }
    }

    suspend fun setTvMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TV_MODE] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setInactivityTimeoutSec(sec: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INACTIVITY_TIMEOUT_SEC] = sec.toString()
        }
    }

    suspend fun setDashboardCardOrder(order: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DASHBOARD_CARD_ORDER] = order.joinToString(",")
        }
    }

    suspend fun setStealthModeActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STEALTH_MODE_ACTIVE] = active
        }
    }

    suspend fun setBiometricSettingsSecured(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_SETTINGS_SECURED] = enabled
        }
    }

    suspend fun setShakeToLock(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHAKE_TO_LOCK] = enabled
        }
    }

    suspend fun setTrashPurgeDays(days: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TRASH_PURGE_DAYS] = days.toString()
        }
    }

    suspend fun setAppLanguage(langCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = langCode
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    fun getLastBackupTimestamp(): Long {
        var ts = 0L
        kotlinx.coroutines.runBlocking {
            context.dataStore.data.collect { prefs ->
                ts = prefs[KEY_LAST_BACKUP_TIMESTAMP] ?: 0L
                return@collect
            }
        }
        return ts
    }

    suspend fun setDuressPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DURESS_PIN] = pin
        }
    }
}
