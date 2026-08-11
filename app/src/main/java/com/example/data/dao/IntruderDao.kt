package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.IntruderSelfieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntruderDao {
    @Query("SELECT * FROM intruder_selfies ORDER BY timestamp DESC")
    fun getAllIntruderSelfies(): Flow<List<IntruderSelfieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderSelfie(selfie: IntruderSelfieEntity)

    @Query("DELETE FROM intruder_selfies WHERE id = :id")
    suspend fun deleteSelfieById(id: Long)

    @Query("DELETE FROM intruder_selfies")
    suspend fun clearAllSelfies()
}
