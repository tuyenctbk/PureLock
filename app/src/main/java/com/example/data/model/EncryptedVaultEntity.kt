package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_vault_items")
data class EncryptedVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val secretContent: String,
    val category: String = "NOTE", // e.g. "PASSWORD", "NOTE", "BANK_PIN", "API_KEY"
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0L
)
