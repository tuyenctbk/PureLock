package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.PureLockDatabase
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class OfflineBackupManager(context: Context) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val LEGACY_ITERATIONS = 10_000
        private const val ITERATIONS = 120_000
        private const val MIN_ITERATIONS = 1_000
        private const val MAX_ITERATIONS = 1_000_000
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val SALT_LENGTH = 16
        private val secureRandom = SecureRandom()
    }

    suspend fun exportEncryptedBackup(passphrase: String): String = withContext(Dispatchers.IO) {
        val db = PureLockDatabase.getDatabase(appContext)

        val lockedApps = db.appLockDao().getAllLockedApps().first()
        val scheduleRules = db.scheduleRuleDao().getAllRules().first()
        val securityLogs = db.logDao().getAllLogs().first()
        val vaultItems = db.encryptedVaultDao().getAllVaultItems().first()

        val backupDataJson = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("app_version", com.example.BuildConfig.VERSION_NAME)

            val appsArray = JSONArray()
            lockedApps.forEach { app ->
                appsArray.put(JSONObject().apply {
                    put("packageName", app.packageName)
                    put("appName", app.appName)
                    put("isLocked", app.isLocked)
                    put("category", app.category)
                    put("lockType", app.lockType)
                })
            }
            put("lockedApps", appsArray)

            val rulesArray = JSONArray()
            scheduleRules.forEach { rule ->
                rulesArray.put(JSONObject().apply {
                    put("id", rule.id)
                    put("packageName", rule.packageName)
                    put("appName", rule.appName)
                    put("startHour", rule.startHour)
                    put("startMinute", rule.startMinute)
                    put("endHour", rule.endHour)
                    put("endMinute", rule.endMinute)
                    put("isEnabled", rule.isEnabled)
                    put("daysString", rule.daysString)
                })
            }
            put("scheduleRules", rulesArray)

            val logsArray = JSONArray()
            securityLogs.take(50).forEach { log ->
                logsArray.put(JSONObject().apply {
                    put("id", log.id)
                    put("action", log.action)
                    put("details", log.details)
                    put("timestamp", log.timestamp)
                })
            }
            put("securityLogs", logsArray)

            val vaultArray = JSONArray()
            vaultItems.forEach { item ->
                vaultArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("secretContent", item.secretContent)
                    put("category", item.category)
                    put("username", item.username)
                    put("websiteOrApp", item.websiteOrApp)
                    put("notes", item.notes)
                    put("isPinned", item.isPinned)
                    put("isFavorite", item.isFavorite)
                    put("timestamp", item.timestamp)
                    put("updatedTimestamp", item.updatedTimestamp)
                    put("isDeleted", item.isDeleted)
                    put("deletedTimestamp", item.deletedTimestamp)
                })
            }
            put("vaultItems", vaultArray)
        }

        val encryptedPackage = encryptPayload(backupDataJson.toString(), passphrase, typeHeader = null)
        return@withContext encryptedPackage.toString(2)
    }

    suspend fun exportEncryptedNotesJson(passphrase: String): String = withContext(Dispatchers.IO) {
        val db = PureLockDatabase.getDatabase(appContext)
        val vaultItems = db.encryptedVaultDao().getAllVaultItems().first()

        val dataJson = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("type", "ENCRYPTED_NOTES_VAULT")
            put("app_version", com.example.BuildConfig.VERSION_NAME)

            val vaultArray = JSONArray()
            vaultItems.forEach { item ->
                vaultArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("secretContent", item.secretContent)
                    put("category", item.category)
                    put("username", item.username)
                    put("websiteOrApp", item.websiteOrApp)
                    put("notes", item.notes)
                    put("isPinned", item.isPinned)
                    put("isFavorite", item.isFavorite)
                    put("timestamp", item.timestamp)
                    put("updatedTimestamp", item.updatedTimestamp)
                    put("isDeleted", item.isDeleted)
                    put("deletedTimestamp", item.deletedTimestamp)
                })
            }
            put("vaultItems", vaultArray)
        }

        val encryptedPackage = encryptPayload(dataJson.toString(), passphrase, typeHeader = "PURELOCK_ENCRYPTED_NOTES")
        return@withContext encryptedPackage.toString(2)
    }

    private fun encryptPayload(plainJson: String, passphrase: String, typeHeader: String?): JSONObject {
        val plainBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        secureRandom.nextBytes(salt)
        secureRandom.nextBytes(iv)

        val keyBytes = deriveKeyBytes(passphrase, salt, ITERATIONS)
        try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(plainBytes)

            return JSONObject().apply {
                put("version", 2)
                if (typeHeader != null) put("type", typeHeader)
                put("algorithm", "AES-256-GCM")
                put("iterations", ITERATIONS)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            }
        } finally {
            Arrays.fill(plainBytes, 0.toByte())
            Arrays.fill(keyBytes, 0.toByte())
        }
    }

    private fun decryptPayload(jsonPackageStr: String, passphrase: String): JSONObject {
        val packageObj = JSONObject(jsonPackageStr)
        val iterations = packageObj.optInt("iterations", LEGACY_ITERATIONS).coerceIn(MIN_ITERATIONS, MAX_ITERATIONS)
        val salt = Base64.decode(packageObj.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(packageObj.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(packageObj.getString("ciphertext"), Base64.NO_WRAP)

        val keyBytes = deriveKeyBytes(passphrase, salt, iterations)
        var plainBytes: ByteArray? = null
        try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            plainBytes = cipher.doFinal(ciphertext)
            val decryptedJsonStr = String(plainBytes, StandardCharsets.UTF_8)
            return JSONObject(decryptedJsonStr)
        } finally {
            Arrays.fill(keyBytes, 0.toByte())
            plainBytes?.let { Arrays.fill(it, 0.toByte()) }
        }
    }

    fun getBackupDirectory(): java.io.File {
        val dir = appContext.getExternalFilesDir("backups") ?: java.io.File(appContext.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun exportEncryptedBackupToUri(uri: Uri, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedJson = exportEncryptedBackup(passphrase)
            appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(encryptedJson.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            } ?: return@withContext false
            com.example.data.UserSettingsManager(appContext).setLastBackupTimestamp(System.currentTimeMillis())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importEncryptedBackupFromUri(uri: Uri, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).readText()
            } ?: return@withContext false
            importEncryptedBackup(content, passphrase)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportEncryptedNotesToFile(passphrase: String): java.io.File = withContext(Dispatchers.IO) {
        val encryptedJson = exportEncryptedNotesJson(passphrase)
        val backupDir = getBackupDirectory()
        val fileName = "purelock_notes_encrypted_${System.currentTimeMillis()}.plk"
        val file = java.io.File(backupDir, fileName)
        file.writeText(encryptedJson, StandardCharsets.UTF_8)
        com.example.data.UserSettingsManager(appContext).setLastBackupTimestamp(System.currentTimeMillis())
        file
    }

    suspend fun importEncryptedNotesFromFile(file: java.io.File, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val content = file.readText(StandardCharsets.UTF_8)
        importEncryptedNotesJson(content, passphrase)
    }

    suspend fun importEncryptedNotesJson(jsonPackageStr: String, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDataObj = decryptPayload(jsonPackageStr, passphrase)
            if (!backupDataObj.has("vaultItems")) {
                return@withContext false
            }
            val db = PureLockDatabase.getDatabase(appContext)
            db.withTransaction {
                restoreVaultItems(db, backupDataObj.getJSONArray("vaultItems"))
            }
            VaultWidgetProvider.requestUpdate(appContext)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportEncryptedBackupToFile(passphrase: String): java.io.File = withContext(Dispatchers.IO) {
        val encryptedJson = exportEncryptedBackup(passphrase)
        val backupDir = getBackupDirectory()
        val fileName = "purelock_full_backup_${System.currentTimeMillis()}.plk"
        val file = java.io.File(backupDir, fileName)
        file.writeText(encryptedJson, StandardCharsets.UTF_8)
        com.example.data.UserSettingsManager(appContext).setLastBackupTimestamp(System.currentTimeMillis())
        file
    }

    suspend fun importEncryptedBackupFromFile(file: java.io.File, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val content = file.readText(StandardCharsets.UTF_8)
        importEncryptedBackup(content, passphrase)
    }

    suspend fun importEncryptedBackup(jsonPackageStr: String, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDataObj = decryptPayload(jsonPackageStr, passphrase)
            val hasRecognizedSection = backupDataObj.has("lockedApps") ||
                backupDataObj.has("scheduleRules") ||
                backupDataObj.has("vaultItems")
            if (!hasRecognizedSection) {
                return@withContext false
            }

            val db = PureLockDatabase.getDatabase(appContext)

            db.withTransaction {
                // Restore Locked Apps while preserving existing unlock counters & timestamps
                if (backupDataObj.has("lockedApps")) {
                    val existingApps = db.appLockDao().getAllLockedApps().first().associateBy { it.packageName }
                    val appsArray = backupDataObj.getJSONArray("lockedApps")
                    val appsToUpsert = mutableListOf<LockedAppEntity>()
                    for (i in 0 until appsArray.length()) {
                        val appObj = appsArray.getJSONObject(i)
                        val pkg = appObj.getString("packageName")
                        if (pkg.isBlank() || pkg == appContext.packageName) continue
                        val prev = existingApps[pkg]
                        appsToUpsert.add(
                            LockedAppEntity(
                                packageName = pkg,
                                appName = appObj.getString("appName"),
                                isLocked = appObj.getBoolean("isLocked"),
                                category = appObj.optString("category", prev?.category ?: "OTHER"),
                                lockType = appObj.optString("lockType", prev?.lockType ?: "PIN"),
                                lastUnlockedTimestamp = prev?.lastUnlockedTimestamp ?: 0L,
                                unlockCount = prev?.unlockCount ?: 0
                            )
                        )
                    }
                    if (appsToUpsert.isNotEmpty()) {
                        db.appLockDao().upsertApps(appsToUpsert)
                    }
                }

                // Restore Schedule Rules (deduplicated)
                if (backupDataObj.has("scheduleRules")) {
                    val existingRuleKeys = db.scheduleRuleDao().getAllRules().first()
                        .map { "${it.packageName}|${it.startHour}:${it.startMinute}|${it.endHour}:${it.endMinute}|${it.daysString}" }
                        .toMutableSet()
                    val rulesArray = backupDataObj.getJSONArray("scheduleRules")
                    for (i in 0 until rulesArray.length()) {
                        val ruleObj = rulesArray.getJSONObject(i)
                        val pkgName = ruleObj.getString("packageName")
                        if (pkgName.isBlank() || pkgName == appContext.packageName) continue
                        val entity = ScheduleRuleEntity(
                            id = 0L,
                            packageName = pkgName,
                            appName = ruleObj.optString("appName", pkgName),
                            startHour = ruleObj.optInt("startHour", 18).coerceIn(0, 23),
                            startMinute = ruleObj.optInt("startMinute", 0).coerceIn(0, 59),
                            endHour = ruleObj.optInt("endHour", 23).coerceIn(0, 23),
                            endMinute = ruleObj.optInt("endMinute", 0).coerceIn(0, 59),
                            isEnabled = ruleObj.optBoolean("isEnabled", true),
                            daysString = ruleObj.optString("daysString", "MON,TUE,WED,THU,FRI,SAT,SUN")
                        )
                        val ruleKey = "${entity.packageName}|${entity.startHour}:${entity.startMinute}|${entity.endHour}:${entity.endMinute}|${entity.daysString}"
                        if (existingRuleKeys.add(ruleKey)) {
                            db.scheduleRuleDao().insertRule(entity)
                        }
                    }
                }

                // Restore Vault Items (deduplicated, preserving isFavorite and updatedTimestamp)
                if (backupDataObj.has("vaultItems")) {
                    restoreVaultItems(db, backupDataObj.getJSONArray("vaultItems"))
                }
            }

            VaultWidgetProvider.requestUpdate(appContext)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun restoreVaultItems(db: PureLockDatabase, vaultArray: JSONArray) {
        val existingVaultKeys = db.encryptedVaultDao().getAllVaultItems().first()
            .map { "${it.title}|${it.secretContent}|${it.category}|${it.timestamp}" }
            .toMutableSet()
        for (i in 0 until vaultArray.length()) {
            val itemObj = vaultArray.getJSONObject(i)
            val ts = itemObj.optLong("timestamp", System.currentTimeMillis())
            val entity = EncryptedVaultEntity(
                id = 0L,
                title = itemObj.getString("title"),
                secretContent = itemObj.getString("secretContent"),
                category = itemObj.optString("category", "NOTE"),
                username = itemObj.optString("username", ""),
                websiteOrApp = itemObj.optString("websiteOrApp", ""),
                notes = itemObj.optString("notes", ""),
                isPinned = itemObj.optBoolean("isPinned", false),
                isFavorite = itemObj.optBoolean("isFavorite", false),
                timestamp = ts,
                updatedTimestamp = itemObj.optLong("updatedTimestamp", ts),
                isDeleted = itemObj.optBoolean("isDeleted", false),
                deletedTimestamp = itemObj.optLong("deletedTimestamp", 0L)
            )
            val key = "${entity.title}|${entity.secretContent}|${entity.category}|${entity.timestamp}"
            if (existingVaultKeys.add(key)) {
                db.encryptedVaultDao().insertVaultItem(entity)
            }
        }
    }

    private fun deriveKeyBytes(passphrase: String, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val chars = passphrase.toCharArray()
        val spec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH)
        try {
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            Arrays.fill(chars, '\u0000')
        }
    }
}
