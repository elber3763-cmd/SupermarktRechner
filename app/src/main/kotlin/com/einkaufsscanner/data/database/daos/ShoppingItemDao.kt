package com.einkaufsscanner.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.einkaufsscanner.data.database.entities.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @androidx.room.Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM shopping_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getItemById(id: Long): ShoppingItemEntity?

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM shopping_items")
    fun getItemCount(): Flow<Int>
}
