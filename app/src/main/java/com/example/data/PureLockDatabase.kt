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
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import com.example.data.model.SecurityLogEntity

@Database(
    entities = [
        LockedAppEntity::class,
        IntruderSelfieEntity::class,
        SecurityLogEntity::class,
        ScheduleRuleEntity::class,
        EncryptedVaultEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PureLockDatabase : RoomDatabase() {
    abstract fun appLockDao(): AppLockDao
    abstract fun intruderDao(): IntruderDao
    abstract fun logDao(): LogDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao
    abstract fun encryptedVaultDao(): EncryptedVaultDao

    companion object {
        @Volatile
        private var INSTANCE: PureLockDatabase? = null

        fun getDatabase(context: Context): PureLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PureLockDatabase::class.java,
                    "purelock_privacy_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
