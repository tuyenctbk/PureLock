package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EncryptedVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedVaultDao {

    @Query("SELECT * FROM encrypted_vault_items WHERE isDeleted = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getActiveVaultItems(): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM encrypted_vault_items WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR websiteOrApp LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY isPinned DESC, timestamp DESC")
    fun searchActiveVaultItems(query: String): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM encrypted_vault_items WHERE isDeleted = 0 AND category = :category ORDER BY isPinned DESC, timestamp DESC")
    fun getVaultItemsByCategory(category: String): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM encrypted_vault_items WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getTrashVaultItems(): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM encrypted_vault_items ORDER BY timestamp DESC")
    fun getAllVaultItems(): Flow<List<EncryptedVaultEntity>>

    @Query("SELECT * FROM encrypted_vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Long): EncryptedVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: EncryptedVaultEntity): Long

    @Update
    suspend fun updateVaultItem(item: EncryptedVaultEntity)

    @Query("UPDATE encrypted_vault_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE encrypted_vault_items SET isDeleted = 1, deletedTimestamp = :deletedTimestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE encrypted_vault_items SET isDeleted = 0, deletedTimestamp = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM encrypted_vault_items WHERE isDeleted = 1 AND deletedTimestamp <= :purgeBeforeTimestamp")
    suspend fun purgeOldTrash(purgeBeforeTimestamp: Long): Int

    @Query("DELETE FROM encrypted_vault_items WHERE isDeleted = 1")
    suspend fun emptyTrash(): Int

    @Query("DELETE FROM encrypted_vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)

    @Query("DELETE FROM encrypted_vault_items")
    suspend fun clearAllVaultItems()
}
