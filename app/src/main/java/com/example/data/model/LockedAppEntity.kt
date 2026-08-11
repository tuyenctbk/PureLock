package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String, // "SOCIAL", "FINANCIAL", "SYSTEM", "MEDIA", "GAMES", "OTHER"
    val isLocked: Boolean = true,
    val lockType: String = "PIN", // "PIN", "PATTERN", "BIOMETRIC"
    val lastUnlockedTimestamp: Long = 0L,
    val unlockCount: Int = 0
)
