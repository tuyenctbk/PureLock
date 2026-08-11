package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_selfies")
data class IntruderSelfieEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptedPackageName: String,
    val attemptedAppName: String,
    val photoData: String, // Encrypted Base64 string or file path
    val failedAttempts: Int = 3
)
