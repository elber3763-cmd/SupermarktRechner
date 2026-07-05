package com.einkaufsscanner.domain.model

import org.junit.Test
import org.junit.Assert.*

class ShoppingCartTest {

    @Test
    fun testEmptyCartTotal() {
        val cart = ShoppingCart()
        assertEquals(0f, cart.total, 0.01f)
    }

    @Test
    fun testAddItem() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 2.99f)

        assertEquals(1, cart.items.size)
        assertEquals(2.99f, cart.total, 0.01f)
        assertEquals("Artikel 1", cart.items[0].name)
    }

    @Test
    fun testAddMultipleItems() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 2.99f)
        cart = cart.addItem(price = 5.50f)
        cart = cart.addItem(price = 1.00f)

        assertEquals(3, cart.items.size)
        assertEquals(9.49f, cart.total, 0.01f)
    }

    @Test
    fun testAddItemWithCustomName() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 4.99f, name = "Milch 1L")

        assertEquals(1, cart.items.size)
        assertEquals("Milch 1L", cart.items[0].name)
    }

    @Test
    fun testRemoveItem() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 2.99f, name = "Item 1")
        cart = cart.addItem(price = 5.50f, name = "Item 2")

        val itemId = cart.items[0].id
        cart = cart.removeItem(itemId)

        assertEquals(1, cart.items.size)
        assertEquals("Item 2", cart.items[0].name)
        assertEquals(5.50f, cart.total, 0.01f)
    }

    @Test
    fun testRemoveNonExistentItem() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 2.99f)

        val originalSize = cart.items.size
        cart = cart.removeItem(999L) // Non-existent ID

        assertEquals(originalSize, cart.items.size)
    }

    @Test
    fun testCartImmutability() {
        val cart1 = ShoppingCart()
        val cart2 = cart1.addItem(price = 2.99f)

        assertEquals(0, cart1.items.size)
        assertEquals(1, cart2.items.size)
    }

    @Test
    fun testLargeCart() {
        var cart = ShoppingCart()

        // Add 100 items
        repeat(100) {
            cart = cart.addItem(price = (Math.random() * 20f).toFloat())
        }

        assertEquals(100, cart.items.size)
        // Verify total is calculated
        assertTrue(cart.total > 0)
    }

    @Test
    fun testFloatingPointPrecision() {
        var cart = ShoppingCart()
        cart = cart.addItem(price = 0.10f)
        cart = cart.addItem(price = 0.20f)
        cart = cart.addItem(price = 0.30f)

        // Common floating point issue: 0.1 + 0.2 + 0.3 != 0.6
        assertEquals(0.60f, cart.total, 0.01f)
    }
}
