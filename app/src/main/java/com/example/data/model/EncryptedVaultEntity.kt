package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "encrypted_vault_items",
    indices = [
        Index(
            value = ["isDeleted", "isPinned", "isFavorite", "updatedTimestamp"],
            name = "idx_vault_active_sort"
        ),
        Index(
            value = ["isDeleted", "category", "updatedTimestamp"],
            name = "idx_vault_active_category"
        ),
        Index(
            value = ["isDeleted", "deletedTimestamp"],
            name = "idx_vault_trash_purge"
        ),
        Index(
            value = ["updatedTimestamp"],
            name = "idx_vault_updated_ts"
        )
    ]
)
data class EncryptedVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val secretContent: String,
    val category: String = "PASSWORD", // "PASSWORD", "NOTE", "CARD", "CODE_2FA", "PIN", "IDENTITY"
    val username: String = "",
    val websiteOrApp: String = "",
    val notes: String = "",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0L
)
