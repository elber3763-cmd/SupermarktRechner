package com.einkaufsscanner.data.repository

import android.util.Log
import com.einkaufsscanner.data.database.ShoppingDatabase
import com.einkaufsscanner.data.database.entities.ShoppingItemEntity
import com.einkaufsscanner.domain.model.CartItem
import com.einkaufsscanner.domain.model.ShoppingCart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingCartRepository @Inject constructor(
    private val database: ShoppingDatabase
) {
    private val dao = database.shoppingItemDao()

    val cartFlow: Flow<ShoppingCart> = dao.getAllItems().map { items ->
        val cartItems = items.filter { it.price > 0 }.map { entity ->
            CartItem(
                id = entity.id,
                price = entity.price * entity.quantity,
                name = entity.name
            )
        }
        ShoppingCart(items = cartItems)
    }

    fun getAllItems(): Flow<List<ShoppingItemEntity>> = dao.getAllItems()

    fun getManualItems(): Flow<List<ShoppingItemEntity>> = dao.getManualItems()

    fun getScannedItems(): Flow<List<ShoppingItemEntity>> = dao.getScannedItems()

    suspend fun addItem(price: Float, name: String? = null) {
        val itemName = name?.takeIf { it.isNotBlank() } ?: "Artikel"
        val entity = ShoppingItemEntity(name = itemName, price = price, itemType = "scanned")
        try {
            dao.insert(entity)
            Log.d("ShoppingCartRepository", "Added item: $itemName for $price€")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error adding item", e)
        }
    }

    suspend fun removeItem(id: Long) {
        try {
            dao.deleteById(id)
            Log.d("ShoppingCartRepository", "Removed item with id: $id")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error removing item", e)
        }
    }

    suspend fun clearCart() {
        try {
            dao.deleteAll()
            Log.d("ShoppingCartRepository", "Cart cleared")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error clearing cart", e)
        }
    }

    suspend fun updateItem(id: Long, price: Float, name: String) {
        try {
            val entity = ShoppingItemEntity(id = id, name = name, price = price)
            dao.update(entity)
            Log.d("ShoppingCartRepository", "Updated item: id=$id, name=$name, price=$price€")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error updating item", e)
        }
    }

    suspend fun addManualItem(name: String, price: Float = 0f, quantity: Int = 1) {
        try {
            val entity = ShoppingItemEntity(
                name = name,
                price = price,
                quantity = quantity,
                isChecked = false,
                itemType = "manual"
            )
            dao.insert(entity)
            Log.d("ShoppingCartRepository", "Added manual item: $name")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error adding manual item", e)
        }
    }

    suspend fun updateManualItem(item: ShoppingItemEntity) {
        try {
            dao.update(item)
            Log.d("ShoppingCartRepository", "Updated manual item: ${item.name}")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error updating manual item", e)
        }
    }

    suspend fun updateItemCheckedStatus(id: Long, isChecked: Boolean) {
        try {
            dao.updateCheckedStatus(id, isChecked)
            Log.d("ShoppingCartRepository", "Updated checked status for item $id: $isChecked")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error updating checked status", e)
        }
    }

    suspend fun deleteShoppingItem(id: Long) {
        try {
            dao.deleteById(id)
            Log.d("ShoppingCartRepository", "Deleted shopping item with id: $id")
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error deleting shopping item", e)
        }
    }

    suspend fun convertManualToScanned(manualItemId: Long, price: Float) {
        try {
            val item = dao.getItemById(manualItemId)
            if (item != null) {
                val updatedItem = item.copy(
                    price = price,
                    itemType = "scanned"
                )
                dao.update(updatedItem)
                Log.d("ShoppingCartRepository", "Converted manual item $manualItemId to scanned with price $price€")
            }
        } catch (e: Exception) {
            Log.e("ShoppingCartRepository", "Error converting manual item to scanned", e)
        }
    }
}
