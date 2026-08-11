package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    @Query("SELECT * FROM locked_apps ORDER BY appName ASC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    fun getActiveLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getActiveLockedAppsSync(): List<LockedAppEntity>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getLockedAppByPackage(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(apps: List<LockedAppEntity>)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun updateLockState(packageName: String, isLocked: Boolean)

    @Query("UPDATE locked_apps SET lastUnlockedTimestamp = :timestamp, unlockCount = unlockCount + 1 WHERE packageName = :packageName")
    suspend fun updateLastUnlocked(packageName: String, timestamp: Long)

    @Query("UPDATE locked_apps SET unlockCount = 0")
    suspend fun resetUnlockCounts()

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
