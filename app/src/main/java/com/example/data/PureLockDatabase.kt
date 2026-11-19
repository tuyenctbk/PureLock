package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import net.zetetic.database.sqlcipher.SQLiteConnection
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

data class DatabaseEncryptionStatus(
    val databaseName: String,
    val isSqlCipherLoaded: Boolean,
    val isEncryptedOnDisk: Boolean,
    val isKeyHardwareWrapped: Boolean,
    val cipherSuite: String = "AES-256-CBC + HMAC-SHA512 (SQLCipher 4)",
    val kdfIterations: Int = 256000
)

@Database(
    entities = [
        LockedAppEntity::class,
        IntruderSelfieEntity::class,
        SecurityLogEntity::class,
        ScheduleRuleEntity::class,
        EncryptedVaultEntity::class,
        UserSettingEntity::class
    ],
    version = 8,
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
        private const val TAG = "PureLockDatabase"
        const val DATABASE_NAME = "purelock_privacy_encrypted.db"
        private val PLAINTEXT_SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

        private val nativeLibraryLoaded = AtomicBoolean(false)

        @Volatile
        private var INSTANCE: PureLockDatabase? = null

        init {
            ensureSqlCipherLoaded()
        }

        /**
         * Ensures the native SQLCipher binary (`libsqlcipher.so`) is loaded before any
         * Room database connection is opened.
         */
        fun ensureSqlCipherLoaded(): Boolean {
            if (nativeLibraryLoaded.get()) return true
            return try {
                System.loadLibrary("sqlcipher")
                nativeLibraryLoaded.set(true)
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to load native SQLCipher library", t)
                false
            }
        }

        fun isSqlCipherNativeLoaded(): Boolean = nativeLibraryLoaded.get()

        fun getDatabase(context: Context): PureLockDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(appContext).also { INSTANCE = it }
            }
        }

        /**
         * Non-destructive schema migration from v7 -> v8 adding query performance indexes across
         * all core tables without altering or wiping encrypted user data.
         */
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_locked_apps_is_locked` ON `locked_apps` (`isLocked`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_locked_apps_app_name` ON `locked_apps` (`appName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_locked_apps_category` ON `locked_apps` (`category`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_vault_active_sort` ON `encrypted_vault_items` (`isDeleted`, `isPinned`, `isFavorite`, `updatedTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_vault_active_category` ON `encrypted_vault_items` (`isDeleted`, `category`, `updatedTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_vault_trash_purge` ON `encrypted_vault_items` (`isDeleted`, `deletedTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_vault_updated_ts` ON `encrypted_vault_items` (`updatedTimestamp`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_schedule_rules_pkg_enabled` ON `schedule_rules` (`packageName`, `isEnabled`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_security_logs_timestamp` ON `security_logs` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_security_logs_action` ON `security_logs` (`action`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_intruder_selfies_timestamp` ON `intruder_selfies` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_intruder_selfies_package` ON `intruder_selfies` (`attemptedPackageName`)")
            }
        }

        private fun buildEncryptedDatabase(appContext: Context): PureLockDatabase {
            ensureSqlCipherLoaded()
            val passphrase = DatabaseKeyManager.getOrCreatePassphrase(appContext)
            val hook = createSqlCipherSecurityHook()
            // clearPassphrase = true instructs SQLCipher to zero the key buffer when closed
            val factory = SupportOpenHelperFactory(passphrase, hook, true)

            return Room.databaseBuilder(
                appContext,
                PureLockDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        try {
                            db.execSQL("PRAGMA foreign_keys = ON;")
                            // Pair WAL mode with NORMAL sync, in-memory temp B-trees, busy timeout, and 2MB page cache
                            db.execSQL("PRAGMA synchronous = NORMAL;")
                            db.execSQL("PRAGMA temp_store = MEMORY;")
                            db.execSQL("PRAGMA busy_timeout = 5000;")
                            db.execSQL("PRAGMA cache_size = -2000;")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to apply SQLite/SQLCipher runtime PRAGMAs on open", e)
                        }
                    }
                })
                .build()
        }

        private fun createSqlCipherSecurityHook(): SQLiteDatabaseHook {
            return object : SQLiteDatabaseHook {
                override fun preKey(connection: SQLiteConnection) {
                    // Hook executed before key material is applied
                }

                override fun postKey(connection: SQLiteConnection) {
                    // Wipe freed SQLCipher pages containing decrypted data from native memory
                    try {
                        connection.execute("PRAGMA cipher_memory_security = ON;", null, null)
                    } catch (e: Exception) {
                        Log.w(TAG, "Unable to enable SQLCipher memory security pragma", e)
                    }
                }
            }
        }

        /**
         * Verifies the database file on disk does not begin with the plaintext SQLite
         * magic header ("SQLite format 3\0"). SQLCipher encrypts page 1 including the
         * header, replacing the first 16 bytes with a random cryptographic salt.
         */
        fun isEncryptedOnDisk(context: Context): Boolean {
            val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
            return isEncryptedDatabaseFile(dbFile)
        }

        internal fun isEncryptedDatabaseFile(dbFile: File): Boolean {
            if (!dbFile.exists() || dbFile.length() < PLAINTEXT_SQLITE_HEADER.size) {
                return false
            }
            val header = ByteArray(PLAINTEXT_SQLITE_HEADER.size)
            val bytesRead = try {
                FileInputStream(dbFile).use { input -> input.read(header) }
            } catch (_: Exception) {
                return false
            }
            if (bytesRead < PLAINTEXT_SQLITE_HEADER.size) return false
            return isSqlCipherHeaderEncrypted(header)
        }

        internal fun isSqlCipherHeaderEncrypted(headerBytes: ByteArray): Boolean {
            if (headerBytes.size < PLAINTEXT_SQLITE_HEADER.size) return false
            val matchesPlaintext = PLAINTEXT_SQLITE_HEADER.indices.all { i ->
                headerBytes[i] == PLAINTEXT_SQLITE_HEADER[i]
            }
            return !matchesPlaintext
        }

        fun getEncryptionStatus(context: Context): DatabaseEncryptionStatus {
            val appContext = context.applicationContext
            return DatabaseEncryptionStatus(
                databaseName = DATABASE_NAME,
                isSqlCipherLoaded = isSqlCipherNativeLoaded(),
                isEncryptedOnDisk = isEncryptedOnDisk(appContext),
                isKeyHardwareWrapped = DatabaseKeyManager.isPassphraseHardwareProtected(appContext)
            )
        }

        /**
         * Safely closes and releases the singleton database instance.
         */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
