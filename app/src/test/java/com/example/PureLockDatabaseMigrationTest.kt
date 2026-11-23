package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.PureLockDatabase
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PureLockDatabaseMigrationTest {

    @Test
    fun `migration 7 to 8 creates all 12 performance indexes and preserves all existing data`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_7_8.db"
        File(context.getDatabasePath(dbName).path).delete()

        val callback = object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `locked_apps` (
                        `packageName` TEXT NOT NULL PRIMARY KEY,
                        `appName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `lastUnlockedTimestamp` INTEGER NOT NULL,
                        `unlockCount` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `encrypted_vault_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `secretContent` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `websiteOrApp` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `deletedTimestamp` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `schedule_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `startHour` INTEGER NOT NULL,
                        `startMinute` INTEGER NOT NULL,
                        `endHour` INTEGER NOT NULL,
                        `endMinute` INTEGER NOT NULL,
                        `daysString` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `security_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `action` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intruder_selfies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `attemptedPackageName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `failedAttempts` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_settings` (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        `value` TEXT NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )

        val db = helper.writableDatabase
        try {
            db.execSQL("INSERT INTO `locked_apps` VALUES ('com.bank.app', 'Bank App', 'FINANCIAL', 1, 0, 2)")
            db.execSQL("INSERT INTO `encrypted_vault_items` VALUES (1, 'Seed Phrase', 'word1 word2', 'NOTE', '', '', '', 1, 1, 0, 0, 1000, 2000)")
            db.execSQL("INSERT INTO `schedule_rules` VALUES (1, 'com.bank.app', 'Bank App', 9, 0, 17, 0, 'MON,TUE', 1)")
            db.execSQL("INSERT INTO `security_logs` VALUES (1, 'APP_LOCKED', 'Bank protected', 3000)")
            db.execSQL("INSERT INTO `intruder_selfies` VALUES (1, 'com.bank.app', 4000, 'base64://img', 3)")
            db.execSQL("INSERT INTO `user_settings` VALUES ('theme_mode', 'MIDNIGHT', 5000)")

            // Run non-destructive migration 7 -> 8
            PureLockDatabase.MIGRATION_7_8.migrate(db)

            val expectedIndexes = setOf(
                "idx_locked_apps_is_locked",
                "idx_locked_apps_app_name",
                "idx_locked_apps_category",
                "idx_vault_active_sort",
                "idx_vault_active_category",
                "idx_vault_trash_purge",
                "idx_vault_updated_ts",
                "idx_schedule_rules_pkg_enabled",
                "idx_security_logs_timestamp",
                "idx_security_logs_action",
                "idx_intruder_selfies_timestamp",
                "idx_intruder_selfies_package"
            )

            val foundIndexes = mutableSetOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'idx_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    foundIndexes.add(cursor.getString(0))
                }
            }

            assertEquals(expectedIndexes, foundIndexes)

            // Verify existing rows remained completely intact
            db.query("SELECT appName, isLocked FROM `locked_apps` WHERE packageName = 'com.bank.app'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Bank App", c.getString(0))
                assertEquals(1, c.getInt(1))
            }
            db.query("SELECT title, secretContent FROM `encrypted_vault_items` WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Seed Phrase", c.getString(0))
                assertEquals("word1 word2", c.getString(1))
            }
            db.query("SELECT appName, isEnabled FROM `schedule_rules` WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Bank App", c.getString(0))
                assertEquals(1, c.getInt(1))
            }
            db.query("SELECT action FROM `security_logs` WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("APP_LOCKED", c.getString(0))
            }
            db.query("SELECT attemptedPackageName FROM `intruder_selfies` WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("com.bank.app", c.getString(0))
            }
            db.query("SELECT value FROM `user_settings` WHERE key = 'theme_mode'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("MIDNIGHT", c.getString(0))
            }
        } finally {
            helper.close()
            File(context.getDatabasePath(dbName).path).delete()
        }
    }

    @Test
    fun `optimized integrity tuples and bulk lock updates work accurately`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PureLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            db.appLockDao().upsertApps(
                listOf(
                    LockedAppEntity("com.app.a", "App A", "SOCIAL", isLocked = false),
                    LockedAppEntity("com.app.b", "App B", "BANKING", isLocked = true)
                )
            )
            db.encryptedVaultDao().insertVaultItem(
                EncryptedVaultEntity(title = "Master Key", secretContent = "secret-xyz", category = "PASSWORD")
            )

            // Only 1 row actually changes from false -> true
            val changedRows = db.appLockDao().updateAllLockStates(true)
            assertEquals(1, changedRows)

            val appTuples = db.appLockDao().getIntegrityTuples()
            assertEquals(2, appTuples.size)
            assertTrue(appTuples.all { it.isLocked })

            val vaultTuples = db.encryptedVaultDao().getIntegrityTuples()
            assertEquals(1, vaultTuples.size)
            assertEquals("Master Key", vaultTuples.first().title)

            // Verify orphaned schedule cleanup for uninstalled packages
            db.scheduleRuleDao().insertRule(
                ScheduleRuleEntity(packageName = "com.app.a", appName = "App A", startHour = 9, startMinute = 0, endHour = 17, endMinute = 0)
            )
            db.scheduleRuleDao().insertRule(
                ScheduleRuleEntity(packageName = "com.app.b", appName = "App B", startHour = 22, startMinute = 0, endHour = 6, endMinute = 0)
            )
            db.scheduleRuleDao().deleteRulesForPackage("com.app.a")
            val remainingRules = db.scheduleRuleDao().getAllRules().first()
            assertEquals(1, remainingRules.size)
            assertEquals("com.app.b", remainingRules.first().packageName)
        } finally {
            db.close()
        }
    }
}
