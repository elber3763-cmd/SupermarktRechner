package com.einkaufsscanner.data.repository

import com.einkaufsscanner.domain.model.CartItem
import com.einkaufsscanner.domain.model.ShoppingCart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

@Singleton
class ShoppingCartRepository {
    private val _cartFlow = MutableStateFlow(ShoppingCart())
    val cartFlow: Flow<ShoppingCart> = _cartFlow.asStateFlow()

    fun getCurrentCart(): ShoppingCart = _cartFlow.value

    fun addItem(price: Float, name: String? = null) {
        val newCart = _cartFlow.value.addItem(price, name)
        _cartFlow.value = newCart
    }

    fun removeItem(id: Long) {
        val newCart = _cartFlow.value.removeItem(id)
        _cartFlow.value = newCart
    }

    fun clearCart() {
        _cartFlow.value = ShoppingCart()
    }

    fun getCartItems(): List<CartItem> = _cartFlow.value.items

    fun getTotal(): Float = _cartFlow.value.total
}
