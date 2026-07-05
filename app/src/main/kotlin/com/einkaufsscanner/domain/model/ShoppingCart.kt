package com.einkaufsscanner.domain.model

data class CartItem(
    val id: Long = System.currentTimeMillis(),
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
