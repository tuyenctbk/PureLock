package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.PureLockDatabase
import com.example.data.model.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class DatabaseChecksumManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun validateChecksumAndLog() {
        scope.launch {
            try {
                val dbFile = context.getDatabasePath("purelock_privacy_db")
                if (!dbFile.exists()) return@launch

                val bytes = dbFile.readBytes()
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(bytes)
                val currentChecksum = hashBytes.joinToString("") { "%02x".format(it) }

                val prefs = context.getSharedPreferences("pure_lock_checksum_prefs", Context.MODE_PRIVATE)
                val savedChecksum = prefs.getString("last_checksum", null)

                val dao = PureLockDatabase.getDatabase(context).logDao()
                if (savedChecksum == null) {
                    prefs.edit().putString("last_checksum", currentChecksum).apply()
                    dao.insertLog(SecurityLogEntity(action = "CHECKSUM_INIT", details = "Initial DB checksum registered: ${currentChecksum.take(12)}..."))
                } else if (savedChecksum != currentChecksum) {
                    Log.w("ChecksumManager", "Database checksum mismatch detected!")
                    dao.insertLog(SecurityLogEntity(action = "CHECKSUM_MISMATCH", details = "WARNING: Database file modified externally or corrupted! Expected $savedChecksum, got $currentChecksum"))
                } else {
                    Log.i("ChecksumManager", "Database checksum verified successfully.")
                }
            } catch (e: Exception) {
                Log.e("ChecksumManager", "Error validating checksum", e)
            }
        }
    }
}
