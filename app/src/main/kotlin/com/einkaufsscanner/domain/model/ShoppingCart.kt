package com.einkaufsscanner.domain.model

import java.util.concurrent.atomic.AtomicLong

private val idGenerator = AtomicLong(System.currentTimeMillis())

data class CartItem(
    val id: Long = idGenerator.incrementAndGet(),
    val name: String,
    val price: Float,
)

data class ShoppingCart(
    val items: List<CartItem> = emptyList(),
) {
    val total: Float
        get() = items.sumOf { it.price.toDouble() }.toFloat()

    fun addItem(price: Float, name: String? = null): ShoppingCart {
        val itemName = name ?: "Artikel ${items.size + 1}"
        return copy(items = items + CartItem(name = itemName, price = price))
    }

    fun removeItem(id: Long): ShoppingCart {
        return copy(items = items.filterNot { it.id == id })
    }
}
