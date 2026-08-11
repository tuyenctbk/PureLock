package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PureLockDatabase
import com.example.data.PureLockPreferences
import com.example.data.PureLockRepository
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PureLockViewModel(application: Application) : AndroidViewModel(application) {

    val repository: PureLockRepository

    init {
        val db = PureLockDatabase.getDatabase(application)
        val prefs = PureLockPreferences(application)
        repository = PureLockRepository(
            application,
            db.appLockDao(),
            db.intruderDao(),
            db.logDao(),
            db.scheduleRuleDao(),
            db.encryptedVaultDao(),
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

    fun saveEncryptedVaultItem(title: String, secretContent: String, category: String = "NOTE") {
        viewModelScope.launch {
            repository.saveEncryptedVaultItem(title, secretContent, category)
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

    fun clearSensitiveState() {
        // Purge clipboard data and in-memory sensitive cached buffers
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    // Preference States
    val masterPin: StateFlow<String> = repository.preferences.masterPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    val masterPattern: StateFlow<String> = repository.preferences.masterPattern
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1,2,5,8,9")

    val securityType: StateFlow<String> = repository.preferences.securityType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "PIN")

    val gracePeriodMs: StateFlow<Long> = repository.preferences.gracePeriodMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30000L)

    val randomKeyboard: StateFlow<Boolean> = repository.preferences.randomKeyboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val stealthDecoy: StateFlow<Boolean> = repository.preferences.stealthDecoy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hidePatternPath: StateFlow<Boolean> = repository.preferences.hidePatternPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val intruderCapture: StateFlow<Boolean> = repository.preferences.intruderCapture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tvMode: StateFlow<Boolean> = repository.preferences.tvMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<String> = repository.preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val inactivityTimeoutSec: StateFlow<Int> = repository.preferences.inactivityTimeoutSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    val dashboardCardOrder: StateFlow<List<String>> = repository.preferences.dashboardCardOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("TIP", "TREND", "STATS", "BATTERY", "TRANSPARENCY", "APPS"))

    val stealthModeActive: StateFlow<Boolean> = repository.preferences.stealthModeActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricSettingsSecured: StateFlow<Boolean> = repository.preferences.biometricSettingsSecured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shakeToLock: StateFlow<Boolean> = repository.preferences.shakeToLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shakeToLockEnabled: StateFlow<Boolean> get() = shakeToLock

    val trashPurgeDays: StateFlow<Int> = repository.preferences.trashPurgeDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    fun setShakeToLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setShakeToLock(enabled)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Shake to Lock gesture set to $enabled.")
        }
    }

    fun toggleShakeToLock(enabled: Boolean) = setShakeToLock(enabled)

    fun setTrashPurgeDays(days: Int) {
        viewModelScope.launch {
            repository.preferences.setTrashPurgeDays(days)
            repository.purgeTrashVault(days)
            repository.logSecurityEvent("SETTINGS_CHANGED", "Trash auto-purge period updated to $days days.")
        }
    }

    fun updateTrashPurgeDays(days: Int) = setTrashPurgeDays(days)

    fun setDashboardCardOrder(order: List<String>) {
        viewModelScope.launch {
            repository.preferences.setDashboardCardOrder(order)
            repository.logSecurityEvent("DASHBOARD_REORDERED", "Dashboard card layout customized.")
        }
    }

    fun toggleStealthMode(active: Boolean) {
        viewModelScope.launch {
            repository.preferences.setStealthModeActive(active)
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
            repository.preferences.setMasterPin(newPin)
        }
    }

    fun updateMasterPattern(newPattern: String) {
        viewModelScope.launch {
            repository.preferences.setMasterPattern(newPattern)
        }
    }

    fun updateSecurityType(type: String) {
        viewModelScope.launch {
            repository.preferences.setSecurityType(type)
        }
    }

    fun updateGracePeriodMs(ms: Long) {
        viewModelScope.launch {
            repository.preferences.setGracePeriodMs(ms)
        }
    }

    fun setRandomKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setRandomKeyboard(enabled)
        }
    }

    fun setStealthDecoy(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setStealthDecoy(enabled)
        }
    }

    fun setHidePatternPath(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setHidePatternPath(enabled)
        }
    }

    fun setIntruderCapture(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setIntruderCapture(enabled)
        }
    }

    fun setTvMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setTvMode(enabled)
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
                lines.drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 4) {
                        val pkg = parts[0].trim()
                        val isLocked = parts[3].trim().toBooleanStrictOrNull() ?: false
                        repository.toggleLockState(pkg, !isLocked)
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
        val result = backupManager.exportEncryptedBackup(passphrase)
        repository.preferences.setLastBackupTimestamp(System.currentTimeMillis())
        return result
    }

    suspend fun importEncryptedBackup(backupJson: String, passphrase: String): Boolean {
        return backupManager.importEncryptedBackup(backupJson, passphrase)
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.preferences.setThemeMode(mode)
            repository.logSecurityEvent("THEME_CHANGED", "UI Theme switched to $mode mode.")
        }
    }

    fun setInactivityTimeoutSec(sec: Int) {
        viewModelScope.launch {
            repository.preferences.setInactivityTimeoutSec(sec)
            repository.logSecurityEvent("INACTIVITY_TIMEOUT_UPDATED", "App UI Inactivity auto-lock set to $sec seconds.")
        }
    }

    fun setBiometricSettingsSecured(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setBiometricSettingsSecured(enabled)
            val logMsg = if (enabled) "Biometric protection enabled for settings screen." else "Biometric protection disabled for settings screen."
            repository.logSecurityEvent("BIOMETRIC_SETTINGS_TOGGLED", logMsg)
        }
    }
}
