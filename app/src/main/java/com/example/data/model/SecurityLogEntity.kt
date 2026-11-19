package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "security_logs",
    indices = [
        Index(value = ["timestamp"], name = "idx_security_logs_timestamp"),
        Index(value = ["action"], name = "idx_security_logs_action")
    ]
)
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // e.g. "APP_LOCKED", "APP_UNLOCKED", "INTRUDER_CAPTURED", "SETTINGS_CHANGED"
    val details: String
)
