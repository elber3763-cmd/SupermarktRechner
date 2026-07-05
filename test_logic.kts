#!/usr/bin/env kotlin

// Quick Kotlin script to validate core logic (can be run with `kotlin test_logic.kts`)

// ============================================================================
// TEST 1: Price Extraction
// ============================================================================
val PRICE_PATTERN = Regex("""(?<!\d)(\d{1,4})[ \t]*[.,][ \t]*(\d{2})(?!\d)""")

fun extractPrice(text: String?): Map<String, Any?> {
    if (text.isNullOrEmpty()) {
        return mapOf("price" to null, "candidates" to emptyList<Float>(), "ambiguous" to false)
    }

    val candidates = mutableListOf<Float>()
    val seenCents = mutableSetOf<Int>()

    PRICE_PATTERN.findAll(text).forEach { match ->
        val euros = match.groupValues[1].toIntOrNull() ?: return@forEach
        val cents = match.groupValues[2].toIntOrNull() ?: return@forEach
        val value = euros + (cents / 100f)

        if (value > 0f && value <= 9999f) {
            val key = (value * 100).toInt()
            if (key !in seenCents) {
                seenCents.add(key)
                candidates.add(value)
            }
        }
    }

    val distinct = candidates.distinct()
    return mapOf(
        "price" to distinct.firstOrNull(),
        "candidates" to distinct,
        "ambiguous" to (distinct.size > 1)
    )
}

println("=== TEST 1: Price Extraction ===")
println("Test 1a: Simple price")
val test1a = extractPrice("Preis: 2,99 EUR")
println("  Input: 'Preis: 2,99 EUR'")
println("  Result: $test1a")
assert(test1a["price"] == 2.99f, "Price should be 2.99")
assert(test1a["ambiguous"] == false, "Should not be ambiguous")
println("  ✓ PASS\n")

println("Test 1b: Ambiguous prices")
val test1b = extractPrice("Alt: 5,99 EUR, Neu: 3,49 EUR")
println("  Input: 'Alt: 5,99 EUR, Neu: 3,49 EUR'")
println("  Result: $test1b")
assert(test1b["ambiguous"] == true, "Should be ambiguous")
assert((test1b["candidates"] as List<*>).size == 2, "Should have 2 candidates")
println("  ✓ PASS\n")

println("Test 1c: Empty text")
val test1c = extractPrice("")
println("  Input: ''")
println("  Result: $test1c")
assert(test1c["price"] == null, "Price should be null")
println("  ✓ PASS\n")

// ============================================================================
// TEST 2: Shopping Cart
// ============================================================================
data class CartItem(val name: String, val price: Float)
data class ShoppingCart(val items: List<CartItem> = emptyList()) {
    val total: Float get() = items.sumOf { it.price.toDouble() }.toFloat()
}

fun addItem(cart: ShoppingCart, price: Float, name: String? = null): ShoppingCart {
    val itemName = name ?: "Artikel ${cart.items.size + 1}"
    return cart.copy(items = cart.items + CartItem(itemName, price))
}

println("=== TEST 2: Shopping Cart ===")
println("Test 2a: Add single item")
var cart = ShoppingCart()
cart = addItem(cart, 2.99f)
println("  Added: 2.99 EUR")
println("  Total: ${cart.total}")
assert(cart.total == 2.99f, "Total should be 2.99")
assert(cart.items.size == 1, "Should have 1 item")
println("  ✓ PASS\n")

println("Test 2b: Add multiple items")
cart = addItem(cart, 5.50f)
cart = addItem(cart, 1.00f)
println("  Added: 5.50 EUR, 1.00 EUR")
println("  Total: ${cart.total}")
assert(cart.total == 9.49f, "Total should be 9.49")
assert(cart.items.size == 3, "Should have 3 items")
println("  ✓ PASS\n")

println("Test 2c: Custom item names")
cart = ShoppingCart()
cart = addItem(cart, 4.99f, "Milch 1L")
println("  Added: 4.99 EUR (Milch 1L)")
println("  Item name: ${cart.items[0].name}")
assert(cart.items[0].name == "Milch 1L", "Item name should be 'Milch 1L'")
println("  ✓ PASS\n")

// ============================================================================
// TEST 3: Floating Point Precision
// ============================================================================
println("=== TEST 3: Floating Point Precision ===")
println("Test 3a: Sum precision")
var precCart = ShoppingCart()
precCart = addItem(precCart, 0.10f)
precCart = addItem(precCart, 0.20f)
precCart = addItem(precCart, 0.30f)
println("  Added: 0.10 + 0.20 + 0.30")
println("  Total: ${precCart.total}")
assert(kotlin.math.abs(precCart.total - 0.60f) < 0.01f, "Total should be ~0.60")
println("  ✓ PASS\n")

// ============================================================================
// SUMMARY
// ============================================================================
println("=" * 50)
println("✅ ALL TESTS PASSED!")
println("=" * 50)
println("\nCore logic is working correctly:")
println("  ✓ Price extraction with Regex")
println("  ✓ Shopping cart management")
println("  ✓ Floating point handling")
println("\nReady to build Android app with 'gradle build'")
