package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_vault_items")
data class EncryptedVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val secretContent: String,
    val category: String = "NOTE", // e.g. "CREDENTIAL", "PASSWORD", "NOTE", "BANK_PIN", "API_KEY", "RECOVERY_KEY", "CARD"
    val username: String = "",
    val websiteOrApp: String = "",
    val notes: String = "",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0L
)
