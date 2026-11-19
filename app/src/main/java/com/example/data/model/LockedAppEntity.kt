package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locked_apps",
    indices = [
        Index(value = ["isLocked"], name = "idx_locked_apps_is_locked"),
        Index(value = ["appName"], name = "idx_locked_apps_app_name"),
        Index(value = ["category"], name = "idx_locked_apps_category")
    ]
)
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
