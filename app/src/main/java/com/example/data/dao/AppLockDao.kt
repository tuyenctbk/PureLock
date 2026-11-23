package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LockedAppEntity
import kotlinx.coroutines.flow.Flow

data class LockedAppIntegrityTuple(
    val packageName: String,
    val isLocked: Boolean,
    val category: String
)

@Dao
interface AppLockDao {
    @Query("SELECT * FROM locked_apps ORDER BY appName ASC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1 ORDER BY appName ASC")
    fun getActiveLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT COUNT(*) FROM locked_apps WHERE isLocked = 1")
    fun getActiveLockedAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM locked_apps WHERE isLocked = 1")
    suspend fun getActiveLockedAppsCountSync(): Int

    @Query("SELECT packageName FROM locked_apps WHERE isLocked = 1")
    fun getActiveLockedPackageNames(): Flow<List<String>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getActiveLockedAppsSync(): List<LockedAppEntity>

    @Query("SELECT packageName, isLocked, category FROM locked_apps ORDER BY packageName ASC")
    suspend fun getIntegrityTuples(): List<LockedAppIntegrityTuple>

    @Query("SELECT * FROM locked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getLockedAppByPackage(packageName: String): LockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: LockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(apps: List<LockedAppEntity>)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName AND isLocked != :isLocked")
    suspend fun updateLockState(packageName: String, isLocked: Boolean)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE isLocked != :isLocked")
    suspend fun updateAllLockStates(isLocked: Boolean): Int

    @Query("UPDATE locked_apps SET lastUnlockedTimestamp = :timestamp, unlockCount = unlockCount + 1 WHERE packageName = :packageName")
    suspend fun updateLastUnlocked(packageName: String, timestamp: Long)

    @Query("UPDATE locked_apps SET unlockCount = 0 WHERE unlockCount != 0")
    suspend fun resetUnlockCounts()

    @Query("UPDATE locked_apps SET lastUnlockedTimestamp = 0 WHERE lastUnlockedTimestamp != 0")
    suspend fun resetAllUnlockedTimestamps()

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
}
