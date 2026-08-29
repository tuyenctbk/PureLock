package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.data.UserSettingsManager
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PureLockViewModel(application: Application) : AndroidViewModel(application) {

    val repository: PureLockRepository
    val userSettings: UserSettingsManager

    init {
        val db = PureLockDatabase.getDatabase(application)
        val prefs = PureLockPreferences(application)
        userSettings = prefs.userSettings
        repository = PureLockRepository(
            application,
            db.appLockDao(),
            db.intruderDao(),
            db.logDao(),
            db.scheduleRuleDao(),
            db.encryptedVaultDao(),
            db.userSettingDao(),
            prefs
        )

        viewModelScope.launch {
            repository.initializeDefaultAppsIfNeeded()
            repository.clearOldLogsAndCache()
            repository.preferences.trashPurgeDays.first().let { days ->
                repository.purgeTrashVault(days)
            }
        }
    }

    val encryptedVaultItems: StateFlow<List<EncryptedVaultEntity>> = repository.encryptedVaultItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashVaultItems: StateFlow<List<EncryptedVaultEntity>> = repository.trashVaultItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveEncryptedVaultItem(
        title: String,
        secretContent: String,
        category: String = "NOTE",
        username: String = "",
        websiteOrApp: String = "",
        notes: String = "",
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            repository.saveEncryptedVaultItem(
                title = title,
                secretContent = secretContent,
                category = category,
                username = username,
                websiteOrApp = websiteOrApp,
                notes = notes,
                isPinned = isPinned
            )
        }
    }

    fun updateEncryptedVaultItem(item: EncryptedVaultEntity) {
        viewModelScope.launch {
            repository.updateEncryptedVaultItem(item)
        }
    }

    fun toggleVaultItemPin(id: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.toggleVaultItemPin(id, isPinned)
        }
    }

    fun moveVaultItemToTrash(id: Long) {
        viewModelScope.launch {
            repository.moveVaultItemToTrash(id)
        }
    }

    fun restoreVaultItemFromTrash(id: Long) {
        viewModelScope.launch {
            repository.restoreVaultItemFromTrash(id)
        }
    }

    fun emptyTrashVault() {
        viewModelScope.launch {
            repository.emptyTrashVault()
        }
    }

    fun deleteEncryptedVaultItem(id: Long) {
        viewModelScope.launch {
            repository.deleteEncryptedVaultItem(id)
        }
    }

    // --- Centralized Clipboard Monitoring with User-Configured Auto-Clear ---
    private val _activeCopiedItemId = MutableStateFlow<Long?>(null)
    val activeCopiedItemId: StateFlow<Long?> = _activeCopiedItemId.asStateFlow()

    private val _clipboardCountdown = MutableStateFlow(0)
    val clipboardCountdown: StateFlow<Int> = _clipboardCountdown.asStateFlow()

    private var clipboardJob: kotlinx.coroutines.Job? = null

    fun copyVaultItemToClipboard(item: EncryptedVaultEntity) {
        clipboardJob?.cancel()
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("VaultSecret", item.secretContent))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _activeCopiedItemId.value = item.id
        val timeoutSec = clipboardAutoClearSec.value
        if (timeoutSec <= 0) {
            _clipboardCountdown.value = 0
            return
        }

        clipboardJob = viewModelScope.launch {
            _clipboardCountdown.value = timeoutSec
            while (_clipboardCountdown.value > 0) {
                kotlinx.coroutines.delay(1000L)
                _clipboardCountdown.value = _clipboardCountdown.value - 1
            }
            clearClipboardNow()
        }
    }

    fun clearClipboardNow() {
        clipboardJob?.cancel()
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _activeCopiedItemId.value = null
        _clipboardCountdown.value = 0
    }

    fun clearSensitiveState() {
        // Purge clipboard data and in-memory sensitive cached buffers
        clearClipboardNow()
        viewModelScope.launch {
            repository.logSecurityEvent("SENSITIVE_STATE_CLEARED", "Screen lock / app background transition executed: sensitive state and clipboard purged.")
        }
    }

    val scheduleRules = repository.scheduleRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allApps: StateFlow<List<LockedAppEntity>> = repository.allLockedApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeLockedAppsCount: StateFlow<Int> = repository.activeLockedApps
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val intruderSelfies: StateFlow<List<IntruderSelfieEntity>> = repository.intruderSelfies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val securityLogs: StateFlow<List<SecurityLogEntity>> = repository.securityLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isOnboardingCompleted: StateFlow<Boolean?> = repository.preferences.isOnboardingCompleted
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val hasSharedApp: StateFlow<Boolean> = repository.preferences.hasSharedApp
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val sharePromptCount: StateFlow<Int> = repository.preferences.sharePromptCount
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val lastSharePromptTimestamp: StateFlow<Long> = repository.preferences.lastSharePromptTimestamp
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0L)

    val hasRatedApp: StateFlow<Boolean> = repository.preferences.hasRatedApp
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)

    val ratePromptCount: StateFlow<Int> = repository.preferences.ratePromptCount
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val lastRatePromptTimestamp: StateFlow<Long> = repository.preferences.lastRatePromptTimestamp
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0L)

    val totalVaultUnlocks: StateFlow<Int> = repository.preferences.totalVaultUnlocks
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    fun completeOnboarding(pin: String, securityType: String, pattern: String = "1,2,5,8,9") {
        viewModelScope.launch {
            repository.preferences.setMasterPin(pin)
            repository.preferences.setMasterPattern(pattern)
            repository.preferences.setSecurityType(securityType)
            repository.preferences.setOnboardingCompleted(true)
            com.example.util.FirebaseManager.logEvent("onboarding_complete", mapOf("security_type" to securityType))
        }
    }

    fun recordSharePromptShown(hasShared: Boolean = false) {
        viewModelScope.launch {
            repository.preferences.recordSharePromptShown(hasShared)
        }
    }

    fun recordRatePromptShown(hasRated: Boolean = false) {
        viewModelScope.launch {
            repository.preferences.recordRatePromptShown(hasRated)
        }
    }

    fun recordVaultUnlock() {
        viewModelScope.launch {
            val count = repository.preferences.incrementVaultUnlocks()
            com.example.util.FirebaseManager.logEvent("vault_unlocked", mapOf("total_unlocks" to count.toString()))
        }
    }

    // Preference States from UserSettingsManager & Room
    val masterPin: StateFlow<String> = userSettings.masterPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    val masterPattern: StateFlow<String> = userSettings.masterPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1,2,5,8,9")

    val securityType: StateFlow<String> = userSettings.securityType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PIN")

    val biometricEnabled: StateFlow<Boolean> = userSettings.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicColorEnabled: StateFlow<Boolean> = userSettings.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hapticFeedbackEnabled: StateFlow<Boolean> = userSettings.hapticFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundFeedbackEnabled: StateFlow<Boolean> = userSettings.soundFeedbackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gracePeriodMs: StateFlow<Long> = userSettings.gracePeriodMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30000L)

    val randomKeyboard: StateFlow<Boolean> = userSettings.randomKeyboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val stealthDecoy: StateFlow<Boolean> = userSettings.stealthDecoy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hidePatternPath: StateFlow<Boolean> = userSettings.hidePatternPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val intruderCapture: StateFlow<Boolean> = userSettings.intruderCapture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tvMode: StateFlow<Boolean> = userSettings.tvMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = combine(
        userSettings.themeMode,
        repository.roomThemeMode
    ) { dsTheme, roomTheme ->
        roomTheme ?: dsTheme
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val inactivityTimeoutSec: StateFlow<Int> = combine(
        userSettings.autoLockInactivitySec,
        repository.roomInactivityTimeoutSec
    ) { dsTimeout, roomTimeout ->
        roomTimeout ?: dsTimeout
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val dashboardCardOrder: StateFlow<List<String>> = userSettings.dashboardCardOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("TIP", "TREND", "STATS", "BATTERY", "TRANSPARENCY", "APPS"))

    val stealthModeActive: StateFlow<Boolean> = userSettings.stealthModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricSettingsSecured: StateFlow<Boolean> = userSettings.biometricSettingsSecured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shakeToLock: StateFlow<Boolean> = userSettings.shakeToLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shakeToLockEnabled: StateFlow<Boolean> get() = shakeToLock

    val trashPurgeDays: StateFlow<Int> = userSettings.trashPurgeDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val appLanguage: StateFlow<String> = userSettings.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val clipboardAutoClearSec: StateFlow<Int> = userSettings.clipboardAutoClearSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    fun setClipboardAutoClearSec(sec: Int) {
        viewModelScope.launch {
            userSettings.setClipboardAutoClearSec(sec)
            repository.logSecurityEvent("CLIPBOARD_SETTINGS_CHANGED", "Clipboard auto-clear delay set to $sec seconds.")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setBiometricEnabled(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Biometric authentication status set to $enabled.")
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setDynamicColorEnabled(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Dynamic colors (Material You) set to $enabled.")
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setHapticFeedbackEnabled(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Tactile haptic feedback set to $enabled.")
        }
    }

    fun setSoundFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setSoundFeedbackEnabled(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Audio feedback set to $enabled.")
        }
    }

    fun setShakeToLock(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setShakeToLock(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Shake to Lock gesture set to $enabled.")
        }
    }

    fun toggleShakeToLock(enabled: Boolean) = setShakeToLock(enabled)

    fun setTrashPurgeDays(days: Int) {
        viewModelScope.launch {
            userSettings.setTrashPurgeDays(days)
            repository.purgeTrashVault(days)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Trash auto-purge period updated to $days days.")
        }
    }

    fun updateTrashPurgeDays(days: Int) = setTrashPurgeDays(days)

    fun setAppLanguage(langCode: String) {
        viewModelScope.launch {
            userSettings.setAppLanguage(langCode)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Application display language updated to $langCode.")
        }
    }

    fun setDashboardCardOrder(order: List<String>) {
        viewModelScope.launch {
            userSettings.setDashboardCardOrder(order)
            repository.logSecurityEvent("DASHBOARD_REORDERED", "Dashboard card layout customized.")
        }
    }

    fun toggleStealthMode(active: Boolean) {
        viewModelScope.launch {
            userSettings.setStealthModeActive(active)
            if (active) {
                try {
                    val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                repository.logSecurityEvent("STEALTH_MODE_ENABLED", "Stealth Mode activated: Network traffic restricted & clipboard history purged.")
            } else {
                repository.logSecurityEvent("STEALTH_MODE_DISABLED", "Stealth Mode deactivated.")
            }
        }
    }

    fun toggleAppLock(packageName: String, currentIsLocked: Boolean) {
        viewModelScope.launch {
            repository.toggleLockState(packageName, currentIsLocked)
        }
    }

    fun toggleLockState(packageName: String, currentIsLocked: Boolean) {
        toggleAppLock(packageName, currentIsLocked)
    }

    fun setAllAppsLockState(isLocked: Boolean) {
        viewModelScope.launch {
            repository.setAllAppsLockState(isLocked)
        }
    }

    fun updateMasterPin(newPin: String) {
        viewModelScope.launch {
            userSettings.setMasterPin(newPin)
        }
    }

    fun updateMasterPattern(newPattern: String) {
        viewModelScope.launch {
            userSettings.setMasterPattern(newPattern)
        }
    }

    fun updateSecurityType(type: String) {
        viewModelScope.launch {
            userSettings.setSecurityType(type)
        }
    }

    fun updateGracePeriodMs(ms: Long) {
        viewModelScope.launch {
            userSettings.setGracePeriodMs(ms)
        }
    }

    fun setRandomKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setRandomKeyboard(enabled)
        }
    }

    fun setStealthDecoy(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setStealthDecoy(enabled)
        }
    }

    fun setHidePatternPath(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setHidePatternPath(enabled)
        }
    }

    fun setIntruderCapture(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setIntruderCapture(enabled)
        }
    }

    fun setTvMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setTvMode(enabled)
        }
    }

    fun deleteIntruderSelfie(id: Long) {
        viewModelScope.launch {
            repository.deleteIntruderSelfie(id)
        }
    }

    fun clearAllIntruders() {
        viewModelScope.launch {
            repository.clearAllIntruders()
        }
    }

    fun logSecurityEvent(action: String, details: String) {
        viewModelScope.launch {
            repository.logSecurityEvent(action, details)
        }
    }

    suspend fun runVaultIntegrityCheck(): com.example.data.VaultIntegrityResult {
        return repository.runVaultIntegrityCheck()
    }

    fun exportConfigToCsv(): String {
        viewModelScope.launch {
            repository.logSecurityEvent("EXPORT_TRIGGERED", "Export triggered: Human-readable CSV config exported.")
        }
        val apps = allApps.value
        val sb = StringBuilder()
        sb.append("PackageName,AppName,Category,IsLocked\n")
        apps.forEach { app ->
            sb.append("${app.packageName},${app.appName},${app.category},${app.isLocked}\n")
        }
        return sb.toString()
    }

    fun exportHumanReadableCsv(): String {
        viewModelScope.launch {
            repository.logSecurityEvent("EXPORT_TRIGGERED", "Export triggered: Full Human-Readable CSV Vault & Settings export generated.")
        }
        val apps = allApps.value
        val vault = encryptedVaultItems.value
        val trash = trashVaultItems.value
        val sb = StringBuilder()
        sb.append("RecordType,ID,Title/Name,Category/Package,Content/Locked,Timestamp\n")

        apps.forEach { app ->
            sb.append("APP,${app.packageName.hashCode()},\"${app.appName.replace("\"", "\"\"")}\",${app.packageName},Locked=${app.isLocked},${app.lastUnlockedTimestamp}\n")
        }
        vault.forEach { item ->
            sb.append("VAULT_SECRET,${item.id},\"${item.title.replace("\"", "\"\"")}\",${item.category},\"${item.secretContent.replace("\"", "\"\"")}\",${item.timestamp}\n")
        }
        trash.forEach { item ->
            sb.append("TRASH_SECRET,${item.id},\"${item.title.replace("\"", "\"\"")}\",${item.category},\"${item.secretContent.replace("\"", "\"\"")}\",${item.deletedTimestamp}\n")
        }
        return sb.toString()
    }

    fun importConfigFromCsv(csvData: String): Boolean {
        return try {
            val lines = csvData.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) return false
            viewModelScope.launch {
                val header = lines.first()
                if (header.startsWith("RecordType")) {
                    // Full Human-Readable CSV format
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 5 && parts[0].trim() == "APP") {
                            val pkg = parts[3].trim()
                            val lockedPart = parts[4].trim()
                            val isLocked = lockedPart.contains("true", ignoreCase = true)
                            repository.setAppLockState(pkg, isLocked)
                        }
                    }
                } else {
                    // Standard config CSV format
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 4) {
                            val pkg = parts[0].trim()
                            val isLocked = parts[3].trim().toBooleanStrictOrNull() ?: false
                            repository.setAppLockState(pkg, isLocked)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun triggerImmediateLockAll() {
        viewModelScope.launch {
            repository.setAllAppsLockState(true)
            repository.logSecurityEvent("IMMEDIATE_LOCK_TRIGGERED", "Immediate Lock executed for all apps")
        }
    }

    fun addScheduleRule(rule: com.example.data.model.ScheduleRuleEntity) {
        viewModelScope.launch {
            repository.addScheduleRule(rule)
        }
    }

    fun deleteScheduleRule(id: Long) {
        viewModelScope.launch {
            repository.deleteScheduleRule(id)
        }
    }

    fun toggleScheduleRule(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleScheduleRule(id, isEnabled)
        }
    }

    fun resetUnlockStats() {
        viewModelScope.launch {
            repository.resetUnlockCounts()
        }
    }

    private val backupManager by lazy { com.example.service.OfflineBackupManager(getApplication()) }

    suspend fun exportEncryptedBackup(passphrase: String): String {
        return backupManager.exportEncryptedBackup(passphrase)
    }

    suspend fun importEncryptedBackup(backupJson: String, passphrase: String): Boolean {
        return backupManager.importEncryptedBackup(backupJson, passphrase)
    }

    suspend fun exportEncryptedNotesJson(passphrase: String): String {
        return backupManager.exportEncryptedNotesJson(passphrase)
    }

    suspend fun importEncryptedNotesJson(jsonPackageStr: String, passphrase: String): Boolean {
        return backupManager.importEncryptedNotesJson(jsonPackageStr, passphrase)
    }

    suspend fun exportEncryptedNotesToFile(passphrase: String): java.io.File {
        return backupManager.exportEncryptedNotesToFile(passphrase)
    }

    suspend fun importEncryptedNotesFromFile(file: java.io.File, passphrase: String): Boolean {
        return backupManager.importEncryptedNotesFromFile(file, passphrase)
    }

    fun getBackupDirectory(): java.io.File {
        return backupManager.getBackupDirectory()
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.saveRoomSetting("theme_mode", mode)
            userSettings.setThemeMode(mode)
            repository.logSecurityEvent("THEME_CHANGED", "UI Theme switched to $mode mode (persisted to Room DB & DataStore).")
        }
    }

    // ViewModel Auto-Lock Timer Logic
    private var autoLockJob: kotlinx.coroutines.Job? = null
    private val _isAppAutoLocked = MutableStateFlow(false)
    val isAppAutoLocked: StateFlow<Boolean> get() = _isAppAutoLocked

    fun resetAutoLockTimer(onTimeout: () -> Unit = {}) {
        autoLockJob?.cancel()
        _isAppAutoLocked.value = false
        val sec = inactivityTimeoutSec.value
        if (sec > 0) {
            autoLockJob = viewModelScope.launch {
                kotlinx.coroutines.delay(sec * 1000L)
                _isAppAutoLocked.value = true
                clearSensitiveState()
                onTimeout()
                logSecurityEvent("AUTO_LOCK_TRIGGERED", "App automatically locked after $sec seconds of inactivity.")
            }
        }
    }

    fun onUserActivity(onTimeout: () -> Unit = {}) {
        resetAutoLockTimer(onTimeout)
    }

    fun unlockApp() {
        _isAppAutoLocked.value = false
        resetAutoLockTimer()
    }

    fun setInactivityTimeoutSec(sec: Int) {
        viewModelScope.launch {
            repository.saveRoomSetting("inactivity_timeout_sec", sec.toString())
            userSettings.setAutoLockInactivitySec(sec)
            repository.logSecurityEvent("INACTIVITY_TIMEOUT_UPDATED", "App UI Inactivity auto-lock set to $sec seconds (persisted to Room DB & DataStore).")
            resetAutoLockTimer()
        }
    }

    fun setBiometricSettingsSecured(enabled: Boolean) {
        viewModelScope.launch {
            userSettings.setBiometricSettingsSecured(enabled)
            val logMsg = if (enabled) "Biometric protection enabled for settings screen." else "Biometric protection disabled for settings screen."
            repository.logSecurityEvent("BIOMETRIC_SETTINGS_TOGGLED", logMsg)
        }
    }
}
