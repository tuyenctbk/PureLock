package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_rules",
    indices = [
        Index(value = ["packageName", "isEnabled"], name = "idx_schedule_rules_pkg_enabled")
    ]
)
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23
    val endMinute: Int, // 0..59
    val isEnabled: Boolean = true,
    val daysString: String = "MON,TUE,WED,THU,FRI,SAT,SUN"
)
