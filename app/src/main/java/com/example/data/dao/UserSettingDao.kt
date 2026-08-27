package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingDao {
    @Query("SELECT * FROM user_settings WHERE key = :key")
    fun getSettingFlow(key: String): Flow<UserSettingEntity?>

    @Query("SELECT * FROM user_settings WHERE key = :key")
    suspend fun getSetting(key: String): UserSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: UserSettingEntity)

    @Query("SELECT * FROM user_settings")
    fun getAllSettings(): Flow<List<UserSettingEntity>>
}
