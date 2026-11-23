package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ScheduleRuleEntity
import kotlinx.coroutines.flow.Flow

data class ScheduleIntegrityTuple(
    val id: Long,
    val packageName: String,
    val isEnabled: Boolean
)

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_rules WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getActiveRulesForPackage(packageName: String): List<ScheduleRuleEntity>

    @Query("SELECT id, packageName, isEnabled FROM schedule_rules ORDER BY id ASC")
    suspend fun getIntegrityTuples(): List<ScheduleIntegrityTuple>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ScheduleRuleEntity)

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM schedule_rules WHERE packageName = :packageName")
    suspend fun deleteRulesForPackage(packageName: String)

    @Query("UPDATE schedule_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateRuleStatus(id: Long, isEnabled: Boolean)
}
