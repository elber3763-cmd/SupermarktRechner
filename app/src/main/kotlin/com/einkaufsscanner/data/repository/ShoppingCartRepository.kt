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
        val cartItems = items.mapIndexed { index, entity ->
            CartItem(
                id = entity.id,
                price = entity.price,
                name = entity.name
            )
        }
        ShoppingCart(items = cartItems)
    }

    suspend fun addItem(price: Float, name: String? = null) {
        val itemName = name?.takeIf { it.isNotBlank() } ?: "Artikel"
        val entity = ShoppingItemEntity(name = itemName, price = price)
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
}
