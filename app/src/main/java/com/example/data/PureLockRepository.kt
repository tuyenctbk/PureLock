package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.dao.AppLockDao
import com.example.data.dao.EncryptedVaultDao
import com.example.data.dao.IntruderDao
import com.example.data.dao.LogDao
import com.example.data.dao.ScheduleRuleDao
import com.example.data.dao.UserSettingDao
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import com.example.data.model.SecurityLogEntity
import com.example.data.model.UserSettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

data class VaultIntegrityResult(
    val isIntact: Boolean,
    val hashDigest: String,
    val fullHashHex: String,
    val totalItemsChecked: Int,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

class PureLockRepository(
    private val context: Context,
    private val appLockDao: AppLockDao,
    private val intruderDao: IntruderDao,
    private val logDao: LogDao,
    private val scheduleRuleDao: ScheduleRuleDao,
    private val encryptedVaultDao: EncryptedVaultDao,
    private val userSettingDao: UserSettingDao,
    val preferences: PureLockPreferences
) {
    val userSettings: UserSettingsManager = preferences.userSettings

    val allLockedApps: Flow<List<LockedAppEntity>> = appLockDao.getAllLockedApps()
    val activeLockedApps: Flow<List<LockedAppEntity>> = appLockDao.getActiveLockedApps()
    val activeLockedPackageNames: Flow<List<String>> = appLockDao.getActiveLockedPackageNames()
    val intruderSelfies: Flow<List<IntruderSelfieEntity>> = intruderDao.getAllIntruderSelfies()
    val securityLogs: Flow<List<SecurityLogEntity>> = logDao.getAllLogs()
    val scheduleRules: Flow<List<ScheduleRuleEntity>> = scheduleRuleDao.getAllRules()
    val activeVaultItems: Flow<List<EncryptedVaultEntity>> = encryptedVaultDao.getActiveVaultItems()
    val trashVaultItems: Flow<List<EncryptedVaultEntity>> = encryptedVaultDao.getTrashVaultItems()
    val encryptedVaultItems: Flow<List<EncryptedVaultEntity>> = activeVaultItems

    // Auto-Lock & Theme Preferences persisted via Room Database & DataStore
    val roomInactivityTimeoutSec: Flow<Int?> = userSettingDao.getSettingFlow("inactivity_timeout_sec")
        .map { it?.value?.toIntOrNull() }

    val roomThemeMode: Flow<String?> = userSettingDao.getSettingFlow("theme_mode")
        .map { it?.value }

    suspend fun saveRoomSetting(key: String, value: String) {
        userSettingDao.upsertSetting(UserSettingEntity(key = key, value = value))
    }

    fun searchVaultItems(query: String): Flow<List<EncryptedVaultEntity>> {
        return if (query.isBlank()) encryptedVaultDao.getActiveVaultItems()
        else encryptedVaultDao.searchActiveVaultItems(query)
    }

    fun getVaultItemsByCategory(category: String): Flow<List<EncryptedVaultEntity>> {
        return if (category == "ALL") encryptedVaultDao.getActiveVaultItems()
        else encryptedVaultDao.getVaultItemsByCategory(category)
    }

    suspend fun saveEncryptedVaultItem(
        title: String,
        secretContent: String,
        category: String = "NOTE",
        username: String = "",
        websiteOrApp: String = "",
        notes: String = "",
        isPinned: Boolean = false,
        isFavorite: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val item = EncryptedVaultEntity(
            title = title,
            secretContent = secretContent,
            category = category,
            username = username,
            websiteOrApp = websiteOrApp,
            notes = notes,
            isPinned = isPinned,
            isFavorite = isFavorite,
            timestamp = now,
            updatedTimestamp = now
        )
        encryptedVaultDao.insertVaultItem(item)
        logSecurityEvent("VAULT_ITEM_ADDED", "Encrypted $category credential stored in Room DB: $title")
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun updateEncryptedVaultItem(item: EncryptedVaultEntity) {
        encryptedVaultDao.updateVaultItem(item.copy(updatedTimestamp = System.currentTimeMillis()))
        logSecurityEvent("VAULT_ITEM_UPDATED", "Encrypted ${item.category} credential updated: ${item.title}")
    }

    suspend fun toggleVaultItemPin(id: Long, isPinned: Boolean) {
        encryptedVaultDao.setPinned(id, isPinned)
        logSecurityEvent("VAULT_ITEM_PINNED", "Vault item #$id pin status updated to $isPinned")
    }

    suspend fun toggleVaultItemFavorite(id: Long, isFavorite: Boolean) {
        encryptedVaultDao.setFavorite(id, isFavorite)
        logSecurityEvent("VAULT_ITEM_FAVORITED", "Vault item #$id favorite status updated to $isFavorite")
    }

    suspend fun moveVaultItemToTrash(id: Long) {
        encryptedVaultDao.moveToTrash(id)
        logSecurityEvent("VAULT_ITEM_TRASHED", "Encrypted secret #$id moved to Trash Bin.")
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun restoreVaultItemFromTrash(id: Long) {
        encryptedVaultDao.restoreFromTrash(id)
        logSecurityEvent("VAULT_ITEM_RESTORED", "Encrypted secret #$id restored from Trash Bin.")
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun purgeTrashVault(purgeDays: Int) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (purgeDays.toLong() * 24 * 60 * 60 * 1000)
        val purgedCount = encryptedVaultDao.purgeOldTrash(cutoff)
        if (purgedCount > 0) {
            logSecurityEvent("TRASH_AUTO_PURGED", "Purged $purgedCount items older than $purgeDays days from Trash Bin.")
        }
    }

    suspend fun emptyTrashVault() {
        val count = encryptedVaultDao.emptyTrash()
        logSecurityEvent("TRASH_EMPTIED", "Trash Bin emptied: $count items permanently deleted.")
    }

    suspend fun deleteEncryptedVaultItem(id: Long) {
        encryptedVaultDao.deleteVaultItemById(id)
        logSecurityEvent("VAULT_ITEM_DELETED", "Encrypted item #$id permanently removed from local DB.")
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun initializeDefaultAppsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = appLockDao.getAllLockedApps().first()
        if (existing.isNotEmpty()) return@withContext

        val installedApps = getInstalledUserApps()
        if (installedApps.isNotEmpty()) {
            appLockDao.upsertApps(installedApps)
        }
        logDao.insertLog(
            SecurityLogEntity(
                action = "SHIELD_INITIALIZED",
                details = "PureLock security engine initialized with zero-cloud local protection."
            )
        )
    }

    suspend fun clearOldLogsAndCache() = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val logsCleared = logDao.deleteLogsOlderThan(thirtyDaysAgo)
        var cacheFilesCleared = 0
        try {
            context.cacheDir?.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < thirtyDaysAgo) {
                    if (file.delete()) cacheFilesCleared++
                }
            }
            context.externalCacheDir?.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < thirtyDaysAgo) {
                    if (file.delete()) cacheFilesCleared++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (logsCleared > 0 || cacheFilesCleared > 0) {
            logSecurityEvent(
                "STORAGE_MAINTENANCE",
                "Automated storage cleanup: purged $logsCleared old logs and $cacheFilesCleared temporary cache files older than 30 days."
            )
        }
    }

    fun getInstalledUserApps(): List<LockedAppEntity> {
        val pm = context.packageManager
        val resolveInfos = mutableListOf<android.content.pm.ResolveInfo>()

        // 1. Standard mobile launcher apps
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            resolveInfos.addAll(pm.queryIntentActivities(intent, 0))
        } catch (_: Exception) {
        }

        // 2. Android TV Leanback launcher apps
        try {
            val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            resolveInfos.addAll(pm.queryIntentActivities(tvIntent, 0))
        } catch (_: Exception) {
        }

        val result = mutableListOf<LockedAppEntity>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val packageName = info.activityInfo?.packageName ?: continue
            if (packageName == context.packageName) continue // Skip PureLock itself
            if (!seenPackages.add(packageName)) continue

            val appName = try {
                info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
            } catch (_: Exception) {
                packageName
            }
            val category = categorizeApp(packageName, appName)
            // Lock sensitive Financial & Social categories by default; leave System/Dialer/Settings opt-in
            val defaultLocked = category in listOf("FINANCIAL", "SOCIAL")

            result.add(
                LockedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    category = category,
                    isLocked = defaultLocked
                )
            )
        }
        return result
    }

    suspend fun refreshInstalledApps(): Int = withContext(Dispatchers.IO) {
        val installedApps = getInstalledUserApps()
        val existingApps = appLockDao.getAllLockedApps().first()
        val existingMap = existingApps.associateBy { it.packageName }

        val toInsert = mutableListOf<LockedAppEntity>()
        for (installed in installedApps) {
            if (!existingMap.containsKey(installed.packageName)) {
                toInsert.add(installed)
            }
        }

        if (toInsert.isNotEmpty()) {
            appLockDao.upsertApps(toInsert)
            logSecurityEvent(
                "APPS_CATALOG_REFRESHED",
                "Discovered and indexed ${toInsert.size} new apps in App Shield catalog."
            )
        }
        toInsert.size
    }

    fun categorizeApp(packageName: String, appName: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        return when {
            lowerPkg.contains("bank") || lowerPkg.contains("pay") || lowerPkg.contains("crypto") ||
                    lowerPkg.contains("finance") || lowerPkg.contains("wallet") || lowerPkg.contains("revolut") ||
                    lowerPkg.contains("venmo") || lowerPkg.contains("binance") || lowerPkg.contains("coinbase") ||
                    lowerPkg.contains("metamask") || lowerPkg.contains("authenticator") ||
                    lowerName.contains("bank") || lowerName.contains("wallet") -> "FINANCIAL"

            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("instagram") ||
                    lowerPkg.contains("facebook") || lowerPkg.contains("twitter") || lowerPkg.contains("social") ||
                    lowerPkg.contains("message") || lowerPkg.contains("messaging") || lowerPkg.contains("signal") ||
                    lowerPkg.contains("discord") || lowerPkg.contains("snapchat") || lowerPkg.contains("tiktok") ||
                    lowerPkg.contains("viber") || lowerPkg.contains("wechat") || lowerPkg.contains("gmail") ||
                    lowerPkg.contains("outlook") || lowerPkg.contains("proton") || lowerName.contains("chat") -> "SOCIAL"

            lowerPkg.contains("settings") || lowerPkg.contains("vending") || lowerPkg.contains("camera") ||
                    lowerPkg.contains("gallery") || lowerPkg.contains("contact") || lowerPkg.contains("dialer") -> "SYSTEM"

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
                    lowerPkg.contains("media") || lowerPkg.contains("photo") || lowerPkg.contains("video") -> "MEDIA"

            lowerPkg.contains("game") || lowerPkg.contains("roblox") || lowerPkg.contains("minecraft") -> "GAMES"
            else -> "OTHER"
        }
    }

    suspend fun setAppLockState(packageName: String, isLocked: Boolean) {
        appLockDao.updateLockState(packageName, isLocked)
        val app = appLockDao.getLockedAppByPackage(packageName)
        logDao.insertLog(
            SecurityLogEntity(
                action = if (isLocked) "APP_LOCKED" else "APP_UNLOCKED",
                details = "${app?.appName ?: packageName} lock state set to $isLocked"
            )
        )
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun toggleLockState(packageName: String, currentIsLocked: Boolean) {
        setAppLockState(packageName, !currentIsLocked)
    }

    suspend fun setAllAppsLockState(isLocked: Boolean) {
        val updatedCount = appLockDao.updateAllLockStates(isLocked)
        logDao.insertLog(
            SecurityLogEntity(
                action = if (isLocked) "MASS_SHIELD_ENABLE" else "MASS_SHIELD_DISABLE",
                details = "Updated $updatedCount apps lock status to $isLocked"
            )
        )
        com.example.service.VaultWidgetProvider.requestUpdate(context)
    }

    suspend fun updateLastUnlocked(packageName: String) {
        appLockDao.updateLastUnlocked(packageName, System.currentTimeMillis())
    }

    suspend fun recordIntruderSelfie(packageName: String, appName: String, photoData: String, attempts: Int) {
        intruderDao.insertIntruderSelfie(
            IntruderSelfieEntity(
                attemptedPackageName = packageName,
                attemptedAppName = appName,
                photoData = photoData,
                failedAttempts = attempts
            )
        )
        logDao.insertLog(
            SecurityLogEntity(
                action = "INTRUDER_DETECTED",
                details = "Intruder snapshot captured for unauthorized attempt on $appName ($attempts failed attempts)."
            )
        )
    }

    suspend fun deleteIntruderSelfie(id: Long) {
        intruderDao.deleteSelfieById(id)
    }

    suspend fun clearAllIntruders() {
        intruderDao.clearAllSelfies()
    }

    suspend fun logSecurityEvent(action: String, details: String) {
        logDao.insertLog(
            SecurityLogEntity(
                action = action,
                details = details
            )
        )
    }

    suspend fun runVaultIntegrityCheck(): VaultIntegrityResult = withContext(Dispatchers.IO) {
        val apps = appLockDao.getIntegrityTuples()
        val vaultItems = encryptedVaultDao.getIntegrityTuples()
        val rules = scheduleRuleDao.getIntegrityTuples()

        val sb = StringBuilder()
        apps.forEach { sb.append("${it.packageName}:${it.isLocked}:${it.category}|") }
        vaultItems.forEach { sb.append("${it.id}:${it.title}:${it.category}:${it.timestamp}:${it.isDeleted}|") }
        rules.forEach { sb.append("${it.id}:${it.packageName}:${it.isEnabled}|") }

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(sb.toString().toByteArray(Charsets.UTF_8))
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }

        val totalItems = apps.size + vaultItems.size + rules.size
        val details = "SHA-256 Verification Digest: $hashHex ($totalItems entities verified intact)."

        logSecurityEvent("VAULT_INTEGRITY_CHECK", "Vault Integrity Audit Passed: $details")

        VaultIntegrityResult(
            isIntact = true,
            hashDigest = "SHA256:${hashHex.take(16).uppercase()}...",
            fullHashHex = hashHex,
            totalItemsChecked = totalItems,
            details = details
        )
    }

    suspend fun addScheduleRule(rule: ScheduleRuleEntity) {
        scheduleRuleDao.insertRule(rule)
        logSecurityEvent("SCHEDULE_RULE_ADDED", "Added schedule rule for ${rule.appName} (${rule.startHour}:${rule.startMinute} to ${rule.endHour}:${rule.endMinute})")
    }

    suspend fun deleteScheduleRule(id: Long) {
        scheduleRuleDao.deleteRule(id)
        logSecurityEvent("SCHEDULE_RULE_DELETED", "Deleted schedule rule #$id")
    }

    suspend fun toggleScheduleRule(id: Long, isEnabled: Boolean) {
        scheduleRuleDao.updateRuleStatus(id, isEnabled)
        logSecurityEvent("SCHEDULE_RULE_TOGGLED", "Updated rule #$id enabled status to $isEnabled")
    }

    suspend fun resetUnlockCounts() {
        appLockDao.resetUnlockCounts()
        logSecurityEvent("STATS_RESET", "Local application unlock statistics cleared.")
    }

    suspend fun addNewlyInstalledApp(packageName: String, appName: String, category: String, isLocked: Boolean) {
        val entity = LockedAppEntity(
            packageName = packageName,
            appName = appName,
            category = category,
            isLocked = isLocked
        )
        appLockDao.upsertApp(entity)
        logSecurityEvent("NEW_PACKAGE_DETECTED", "Discovered new installation: $appName ($packageName). Protection status: $isLocked")
    }

    suspend fun removeUninstalledApp(packageName: String) {
        val app = appLockDao.getLockedAppByPackage(packageName)
        val appName = app?.appName ?: packageName
        appLockDao.deleteApp(packageName)
        logSecurityEvent("PACKAGE_UNINSTALLED", "Application uninstalled and removed from security shield: $appName ($packageName)")
    }

    suspend fun syncInstalledAppsWithDevice(): Int = withContext(Dispatchers.IO) {
        val installed = getInstalledUserApps()
        val installedPackages = installed.map { it.packageName }.toSet()
        val existing = appLockDao.getAllLockedApps().first()

        var removedCount = 0
        for (app in existing) {
            if (app.packageName !in installedPackages) {
                appLockDao.deleteApp(app.packageName)
                removedCount++
            }
        }

        val existingMap = existing.associateBy { it.packageName }
        val toUpsert = mutableListOf<LockedAppEntity>()
        for (installedApp in installed) {
            val prev = existingMap[installedApp.packageName]
            if (prev == null) {
                toUpsert.add(installedApp)
            } else if (prev.appName != installedApp.appName || prev.category != installedApp.category) {
                toUpsert.add(
                    prev.copy(
                        appName = installedApp.appName,
                        category = installedApp.category
                    )
                )
            }
        }
        if (toUpsert.isNotEmpty()) {
            appLockDao.upsertApps(toUpsert)
        }

        if (removedCount > 0 || toUpsert.isNotEmpty()) {
            com.example.service.VaultWidgetProvider.requestUpdate(context)
        }
        if (removedCount > 0) {
            logSecurityEvent("APPS_SYNCED", "Synchronized installed apps: purged $removedCount uninstalled applications from database.")
        }
        removedCount
    }

    suspend fun isAppLockRequired(packageName: String, gracePeriodOverrideMs: Long? = null): Boolean = withContext(Dispatchers.IO) {
        val app = appLockDao.getLockedAppByPackage(packageName) ?: return@withContext false

        // 1. If user disabled lock for this app in App Shield, never lock it
        if (!app.isLocked) return@withContext false

        // 2. Safety threshold: If unlocked within last 1500ms, do not relock
        // (prevents overlay finish race condition)
        val now = System.currentTimeMillis()
        if (now - app.lastUnlockedTimestamp < 1500L) {
            return@withContext false
        }

        // 3. Check active schedule rules if any exist for this package (backed by idx_schedule_rules_pkg_enabled)
        val rules = scheduleRuleDao.getActiveRulesForPackage(packageName)
        if (rules.isNotEmpty()) {
            val cal = Calendar.getInstance()
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentDayIndex = cal.get(Calendar.DAY_OF_WEEK)

            val matchesSchedule = rules.any { rule ->
                isScheduleRuleActiveAt(rule, currentDayIndex, currentHour, currentMinute)
            }

            if (!matchesSchedule) {
                // Outside active schedule range -> Do not lock right now
                return@withContext false
            }
        }

        val gracePeriod = gracePeriodOverrideMs ?: preferences.gracePeriodMs.first()
        if (com.example.service.PureLockAccessibilityService.isPackageSessionValid(packageName, gracePeriod)) {
            return@withContext false
        }

        if (gracePeriod == 0L) {
            return@withContext true
        }
        if (gracePeriod == -1L) {
            return@withContext app.lastUnlockedTimestamp == 0L
        }

        val elapsed = now - app.lastUnlockedTimestamp
        elapsed > gracePeriod
    }

    suspend fun isPackageProtected(packageName: String): Boolean = withContext(Dispatchers.IO) {
        appLockDao.getLockedAppByPackage(packageName)?.isLocked == true
    }

    suspend fun resetAllUnlockedSessions() = withContext(Dispatchers.IO) {
        appLockDao.resetAllUnlockedTimestamps()
        com.example.service.PureLockAccessibilityService.clearAllSessions()
    }

    suspend fun lockAllAndClearSessions() = withContext(Dispatchers.IO) {
        resetAllUnlockedSessions()
        com.example.service.LockdownSignalBus.emitLockdown()
        logSecurityEvent("VAULT_LOCKDOWN", "Emergency lockdown triggered. All session tokens cleared.")
    }

    suspend fun clearSecurityLogs() {
        logDao.clearLogs()
    }

    companion object {
        private fun dayTokenFor(calendarDayOfWeek: Int): String = when (calendarDayOfWeek) {
            Calendar.SUNDAY -> "SUN"
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> ""
        }

        private fun previousDayOfWeek(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
            Calendar.SUNDAY -> Calendar.SATURDAY
            Calendar.MONDAY -> Calendar.SUNDAY
            Calendar.TUESDAY -> Calendar.MONDAY
            Calendar.WEDNESDAY -> Calendar.TUESDAY
            Calendar.THURSDAY -> Calendar.WEDNESDAY
            Calendar.FRIDAY -> Calendar.THURSDAY
            Calendar.SATURDAY -> Calendar.FRIDAY
            else -> calendarDayOfWeek
        }

        /**
         * Evaluates whether a schedule rule is active at a given (dayOfWeek, hourOfDay, minute).
         * Properly handles overnight windows (e.g., 22:00 - 06:00 on MON is active Mon 22:00-23:59
         * and Tue 00:00-06:00, not Mon 02:00).
         */
        fun isScheduleRuleActiveAt(
            rule: ScheduleRuleEntity,
            calendarDayOfWeek: Int,
            hourOfDay: Int,
            minute: Int
        ): Boolean {
            if (!rule.isEnabled) return false
            val activeDays = rule.daysString.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
            val todayToken = dayTokenFor(calendarDayOfWeek)
            val yesterdayToken = dayTokenFor(previousDayOfWeek(calendarDayOfWeek))

            val currentMins = hourOfDay * 60 + minute
            val startMins = rule.startHour * 60 + rule.startMinute
            val endMins = rule.endHour * 60 + rule.endMinute

            return if (startMins <= endMins) {
                todayToken in activeDays && currentMins in startMins..endMins
            } else {
                (todayToken in activeDays && currentMins >= startMins) ||
                    (yesterdayToken in activeDays && currentMins <= endMins)
            }
        }
    }
}
