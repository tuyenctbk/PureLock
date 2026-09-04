package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * PureLockPreferences delegates to the unified UserSettingsManager,
 * ensuring backwards compatibility while providing typed Jetpack DataStore access.
 */
class PureLockPreferences(context: Context) {

    val userSettings = UserSettingsManager(context)

    val duressPin: Flow<String> = userSettings.duressPin
    val shakeToLock: Flow<Boolean> = userSettings.shakeToLock
    val appLanguage: Flow<String> = userSettings.appLanguage
    val trashPurgeDays: Flow<Int> = userSettings.trashPurgeDays
    val biometricSettingsSecured: Flow<Boolean> = userSettings.biometricSettingsSecured
    val dashboardCardOrder: Flow<List<String>> = userSettings.dashboardCardOrder
    val stealthModeActive: Flow<Boolean> = userSettings.stealthModeActive
    val themeMode: Flow<String> = userSettings.themeMode
    val dynamicColorEnabled: Flow<Boolean> = userSettings.dynamicColorEnabled
    val inactivityTimeoutSec: Flow<Int> = userSettings.autoLockInactivitySec
    val masterPin: Flow<String> = userSettings.masterPin
    val masterPattern: Flow<String> = userSettings.masterPattern
    val securityType: Flow<String> = userSettings.securityType
    val biometricEnabled: Flow<Boolean> = userSettings.biometricEnabled
    val gracePeriodMs: Flow<Long> = userSettings.gracePeriodMs
    val randomKeyboard: Flow<Boolean> = userSettings.randomKeyboard
    val stealthDecoy: Flow<Boolean> = userSettings.stealthDecoy
    val decoyType: Flow<String> = userSettings.decoyType
    val masterKnock: Flow<String> = userSettings.masterKnock
    val hidePatternPath: Flow<Boolean> = userSettings.hidePatternPath
    val intruderCapture: Flow<Boolean> = userSettings.intruderCapture
    val tvMode: Flow<Boolean> = userSettings.tvMode
    val lastBackupTimestamp: Flow<Long> = userSettings.lastBackupTimestamp
    val hapticFeedbackEnabled: Flow<Boolean> = userSettings.hapticFeedbackEnabled
    val soundFeedbackEnabled: Flow<Boolean> = userSettings.soundFeedbackEnabled
    val clipboardAutoClearSec: Flow<Int> = userSettings.clipboardAutoClearSec

    val isOnboardingCompleted: Flow<Boolean> = userSettings.isOnboardingCompleted
    val hasSharedApp: Flow<Boolean> = userSettings.hasSharedApp
    val sharePromptCount: Flow<Int> = userSettings.sharePromptCount
    val lastSharePromptTimestamp: Flow<Long> = userSettings.lastSharePromptTimestamp
    val hasRatedApp: Flow<Boolean> = userSettings.hasRatedApp
    val ratePromptCount: Flow<Int> = userSettings.ratePromptCount
    val lastRatePromptTimestamp: Flow<Long> = userSettings.lastRatePromptTimestamp
    val totalVaultUnlocks: Flow<Int> = userSettings.totalVaultUnlocks

    suspend fun setMasterPin(pin: String) = userSettings.setMasterPin(pin)
    suspend fun setMasterPattern(pattern: String) = userSettings.setMasterPattern(pattern)
    suspend fun setSecurityType(type: String) = userSettings.setSecurityType(type)
    suspend fun setBiometricEnabled(enabled: Boolean) = userSettings.setBiometricEnabled(enabled)
    suspend fun setGracePeriodMs(ms: Long) = userSettings.setGracePeriodMs(ms)
    suspend fun setRandomKeyboard(enabled: Boolean) = userSettings.setRandomKeyboard(enabled)
    suspend fun setStealthDecoy(enabled: Boolean) = userSettings.setStealthDecoy(enabled)
    suspend fun setDecoyType(type: String) = userSettings.setDecoyType(type)
    suspend fun setMasterKnock(knock: String) = userSettings.setMasterKnock(knock)
    suspend fun setHidePatternPath(enabled: Boolean) = userSettings.setHidePatternPath(enabled)
    suspend fun setIntruderCapture(enabled: Boolean) = userSettings.setIntruderCapture(enabled)
    suspend fun setTvMode(enabled: Boolean) = userSettings.setTvMode(enabled)
    suspend fun setThemeMode(mode: String) = userSettings.setThemeMode(mode)
    suspend fun setDynamicColorEnabled(enabled: Boolean) = userSettings.setDynamicColorEnabled(enabled)
    suspend fun setInactivityTimeoutSec(sec: Int) = userSettings.setAutoLockInactivitySec(sec)
    suspend fun setDashboardCardOrder(order: List<String>) = userSettings.setDashboardCardOrder(order)
    suspend fun setStealthModeActive(active: Boolean) = userSettings.setStealthModeActive(active)
    suspend fun setBiometricSettingsSecured(enabled: Boolean) = userSettings.setBiometricSettingsSecured(enabled)
    suspend fun setShakeToLock(enabled: Boolean) = userSettings.setShakeToLock(enabled)
    suspend fun setTrashPurgeDays(days: Int) = userSettings.setTrashPurgeDays(days)
    suspend fun setAppLanguage(langCode: String) = userSettings.setAppLanguage(langCode)
    suspend fun setLastBackupTimestamp(timestamp: Long) = userSettings.setLastBackupTimestamp(timestamp)
    suspend fun getLastBackupTimestamp(): Long = userSettings.getLastBackupTimestamp()
    suspend fun setDuressPin(pin: String) = userSettings.setDuressPin(pin)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = userSettings.setHapticFeedbackEnabled(enabled)
    suspend fun setSoundFeedbackEnabled(enabled: Boolean) = userSettings.setSoundFeedbackEnabled(enabled)
    suspend fun setClipboardAutoClearSec(sec: Int) = userSettings.setClipboardAutoClearSec(sec)

    suspend fun setOnboardingCompleted(completed: Boolean) = userSettings.setOnboardingCompleted(completed)
    suspend fun recordSharePromptShown(hasShared: Boolean = false) = userSettings.recordSharePromptShown(hasShared)
    suspend fun recordRatePromptShown(hasRated: Boolean = false) = userSettings.recordRatePromptShown(hasRated)
    suspend fun incrementVaultUnlocks(): Int = userSettings.incrementVaultUnlocks()
}
