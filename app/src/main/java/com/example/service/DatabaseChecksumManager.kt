package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.PureLockDatabase
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest

class DatabaseChecksumManager(context: Context) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "ChecksumManager"
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    fun validateChecksumAndLog() {
        scope.launch {
            try {
                val dbFile = appContext.getDatabasePath(PureLockDatabase.DATABASE_NAME)
                if (!dbFile.exists()) return@launch

                val isEncryptedOnDisk = PureLockDatabase.isEncryptedOnDisk(appContext)
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                dbFile.inputStream().buffered().use { input ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                val hashBytes = digest.digest()
                val currentChecksum = hashBytes.joinToString("") { "%02x".format(it) }

                val prefs = appContext.getSharedPreferences("pure_lock_checksum_prefs", Context.MODE_PRIVATE)
                val savedChecksum = prefs.getString("last_checksum", null)

                val dao = PureLockDatabase.getDatabase(appContext).logDao()
                if (!isEncryptedOnDisk) {
                    Log.w(TAG, "Database header verification warning: file is not SQLCipher-encrypted.")
                    dao.insertLog(
                        SecurityLogEntity(
                            action = "ENCRYPTION_WARNING",
                            details = "Database file header check detected unencrypted SQLite signature."
                        )
                    )
                }

                if (savedChecksum == null) {
                    prefs.edit().putString("last_checksum", currentChecksum).apply()
                    dao.insertLog(
                        SecurityLogEntity(
                            action = "CHECKSUM_INIT",
                            details = "Initial SQLCipher DB checksum registered: ${currentChecksum.take(12)}..."
                        )
                    )
                } else if (savedChecksum != currentChecksum) {
                    prefs.edit().putString("last_checksum", currentChecksum).apply()
                    Log.i(TAG, "Encrypted database checksum updated: ${currentChecksum.take(12)}...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error validating checksum", e)
            }
        }
    }
}
