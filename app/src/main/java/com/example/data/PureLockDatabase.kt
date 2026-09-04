package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        LockedAppEntity::class,
        IntruderSelfieEntity::class,
        SecurityLogEntity::class,
        ScheduleRuleEntity::class,
        EncryptedVaultEntity::class,
        UserSettingEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class PureLockDatabase : RoomDatabase() {
    abstract fun appLockDao(): AppLockDao
    abstract fun intruderDao(): IntruderDao
    abstract fun logDao(): LogDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun encryptedVaultDao(): EncryptedVaultDao
    abstract fun userSettingDao(): UserSettingDao

    companion object {
        @Volatile
        private var INSTANCE: PureLockDatabase? = null

        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val prefs = context.getSharedPreferences("purelock_sec_vault", Context.MODE_PRIVATE)
            val keyAlias = "purelock_db_enc_key"
            val existing = prefs.getString(keyAlias, null)
            if (existing != null) {
                return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
            }
            val randomBytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(randomBytes)
            val encoded = android.util.Base64.encodeToString(randomBytes, android.util.Base64.NO_WRAP)
            prefs.edit().putString(keyAlias, encoded).apply()
            return randomBytes
        }

        init {
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: Throwable) {}
        }

        fun getDatabase(context: Context): PureLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = getOrCreatePassphrase(context.applicationContext)
                val factory = SupportOpenHelperFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PureLockDatabase::class.java,
                    "purelock_privacy_encrypted.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
