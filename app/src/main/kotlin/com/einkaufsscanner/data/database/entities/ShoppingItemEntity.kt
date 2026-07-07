package com.einkaufsscanner.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Float,
    val quantity: Int = 1,
    val isChecked: Boolean = false,
    val itemType: String = "scanned", // "scanned" oder "manual"
    val timestamp: Long = System.currentTimeMillis()
)

enum class ItemType {
    SCANNED,
    MANUAL
}
