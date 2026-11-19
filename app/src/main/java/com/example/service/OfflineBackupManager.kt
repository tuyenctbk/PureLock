package com.example.service

import android.content.Context
import android.util.Base64
import com.example.data.PureLockDatabase
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class OfflineBackupManager(private val context: Context) {

    companion object {
        private const val ITERATIONS = 10000
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
        private const val SALT_LENGTH = 16
    }

    suspend fun exportEncryptedBackup(passphrase: String): String = withContext(Dispatchers.IO) {
        val db = PureLockDatabase.getDatabase(context)

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

        val plainBytes = backupDataJson.toString().toByteArray(StandardCharsets.UTF_8)

        // Generate Random Salt and IV
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(salt)
        random.nextBytes(iv)

        // Derive Key via PBKDF2
        val secretKey = deriveKey(passphrase, salt)

        // Encrypt via AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plainBytes)

        val encryptedPackage = JSONObject().apply {
            put("version", 1)
            put("algorithm", "AES-256-GCM")
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }

        com.example.data.UserSettingsManager(context).setLastBackupTimestamp(System.currentTimeMillis())
        return@withContext encryptedPackage.toString(2)
    }

    suspend fun exportEncryptedNotesJson(passphrase: String): String = withContext(Dispatchers.IO) {
        val db = PureLockDatabase.getDatabase(context)
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

        val plainBytes = dataJson.toString().toByteArray(StandardCharsets.UTF_8)
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(salt)
        random.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(plainBytes)

        val encryptedPackage = JSONObject().apply {
            put("version", 1)
            put("type", "PURELOCK_ENCRYPTED_NOTES")
            put("algorithm", "AES-256-GCM")
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }

        com.example.data.UserSettingsManager(context).setLastBackupTimestamp(System.currentTimeMillis())
        return@withContext encryptedPackage.toString(2)
    }

    fun getBackupDirectory(): java.io.File {
        val dir = context.getExternalFilesDir("backups") ?: java.io.File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun exportEncryptedNotesToFile(passphrase: String): java.io.File = withContext(Dispatchers.IO) {
        val encryptedJson = exportEncryptedNotesJson(passphrase)
        val backupDir = getBackupDirectory()
        val fileName = "purelock_notes_encrypted_${System.currentTimeMillis()}.plk"
        val file = java.io.File(backupDir, fileName)
        file.writeText(encryptedJson, StandardCharsets.UTF_8)
        file
    }

    suspend fun importEncryptedNotesFromFile(file: java.io.File, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val content = file.readText(StandardCharsets.UTF_8)
        importEncryptedNotesJson(content, passphrase)
    }

    suspend fun importEncryptedNotesJson(jsonPackageStr: String, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageObj = JSONObject(jsonPackageStr)
            val salt = Base64.decode(packageObj.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(packageObj.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(packageObj.getString("ciphertext"), Base64.NO_WRAP)

            val secretKey = deriveKey(passphrase, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plainBytes = cipher.doFinal(ciphertext)

            val decryptedJsonStr = String(plainBytes, StandardCharsets.UTF_8)
            val backupDataObj = JSONObject(decryptedJsonStr)
            val db = PureLockDatabase.getDatabase(context)

            if (backupDataObj.has("vaultItems")) {
                val existingKeys = db.encryptedVaultDao().getAllVaultItems().first()
                    .map { "${it.title}|${it.secretContent}|${it.category}|${it.timestamp}" }
                    .toMutableSet()
                val vaultArray = backupDataObj.getJSONArray("vaultItems")
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
                    if (existingKeys.add(key)) {
                        db.encryptedVaultDao().insertVaultItem(entity)
                    }
                }
            }
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
        file
    }

    suspend fun importEncryptedBackupFromFile(file: java.io.File, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val content = file.readText(StandardCharsets.UTF_8)
        importEncryptedBackup(content, passphrase)
    }

    suspend fun importEncryptedBackup(jsonPackageStr: String, passphrase: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageObj = JSONObject(jsonPackageStr)
            val salt = Base64.decode(packageObj.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(packageObj.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(packageObj.getString("ciphertext"), Base64.NO_WRAP)

            val secretKey = deriveKey(passphrase, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plainBytes = cipher.doFinal(ciphertext)

            val decryptedJsonStr = String(plainBytes, StandardCharsets.UTF_8)
            val backupDataObj = JSONObject(decryptedJsonStr)

            val db = PureLockDatabase.getDatabase(context)

            // Restore Locked Apps
            if (backupDataObj.has("lockedApps")) {
                val appsArray = backupDataObj.getJSONArray("lockedApps")
                for (i in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(i)
                    val entity = LockedAppEntity(
                        packageName = appObj.getString("packageName"),
                        appName = appObj.getString("appName"),
                        isLocked = appObj.getBoolean("isLocked"),
                        category = appObj.optString("category", "SYSTEM")
                    )
                    db.appLockDao().upsertApp(entity)
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
                    val entity = ScheduleRuleEntity(
                        id = 0L,
                        packageName = ruleObj.getString("packageName"),
                        appName = ruleObj.optString("appName", "App"),
                        startHour = ruleObj.optInt("startHour", 18),
                        startMinute = ruleObj.optInt("startMinute", 0),
                        endHour = ruleObj.optInt("endHour", 23),
                        endMinute = ruleObj.optInt("endMinute", 0),
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
                val existingVaultKeys = db.encryptedVaultDao().getAllVaultItems().first()
                    .map { "${it.title}|${it.secretContent}|${it.category}|${it.timestamp}" }
                    .toMutableSet()
                val vaultArray = backupDataObj.getJSONArray("vaultItems")
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

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }
}
