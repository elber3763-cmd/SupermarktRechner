package com.einkaufsscanner.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PriceInputDialog(
    onPriceSubmit: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf("") }
    var isEditingQuantity by remember { mutableStateOf(false) }

    val totalPrice = try {
        val price = parsePrice(priceText)
        val quantity = quantityText.toIntOrNull() ?: 1
        price * quantity
    } catch (e: Exception) {
        0f
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    "Preis & Menge",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Display - Preis
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp)
                        .background(
                            if (isEditingQuantity) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                            RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEditingQuantity) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preis:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (priceText.isEmpty()) "0,00€" else "$priceText€",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Display - Menge
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp)
                        .background(
                            if (isEditingQuantity) Color(0xFFF5F5F5) else Color(0xFFE3F2FD),
                            RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isEditingQuantity) Color(0xFFF5F5F5) else Color(0xFFE3F2FD)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Menge:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = quantityText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Total
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gesamt:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format("%.2f€", totalPrice),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                // Error message
                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth()
                    )
                }

                // Numpad
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    // Row 1
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadButton("1", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "1")
                            else priceText = addDigit(priceText, "1")
                        }
                        NumpadButton("2", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "2")
                            else priceText = addDigit(priceText, "2")
                        }
                        NumpadButton("3", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "3")
                            else priceText = addDigit(priceText, "3")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadButton("4", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "4")
                            else priceText = addDigit(priceText, "4")
                        }
                        NumpadButton("5", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "5")
                            else priceText = addDigit(priceText, "5")
                        }
                        NumpadButton("6", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "6")
                            else priceText = addDigit(priceText, "6")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumpadButton("7", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "7")
                            else priceText = addDigit(priceText, "7")
                        }
                        NumpadButton("8", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "8")
                            else priceText = addDigit(priceText, "8")
                        }
                        NumpadButton("9", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "9")
                            else priceText = addDigit(priceText, "9")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 4
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isEditingQuantity) {
                            // For quantity: show clear button
                            NumpadButton("C", Modifier.weight(1f)) { quantityText = "1" }
                        } else {
                            // For price: show comma
                            NumpadButton(",", Modifier.weight(1f)) { priceText = addComma(priceText) }
                        }
                        NumpadButton("0", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = addQuantity(quantityText, "0")
                            else priceText = addDigit(priceText, "0")
                        }
                        NumpadButton("⌫", Modifier.weight(1f)) {
                            if (isEditingQuantity) quantityText = quantityText.dropLast(1).takeIf { it.isNotEmpty() } ?: "1"
                            else priceText = priceText.dropLast(1)
                        }
                    }
                }

                // Tab button - switch between price and quantity
                Button(
                    onClick = { isEditingQuantity = !isEditingQuantity },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D))
                ) {
                    Text(
                        if (isEditingQuantity) "← Zurück zu Preis" else "Menge eingeben →",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Text("Abbrechen", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            error = ""
                            val price = parsePrice(priceText)
                            if (price > 0f) {
                                onPriceSubmit(totalPrice)
                            } else {
                                error = "Bitte gültige Preis eingeben (z.B. 2,99)"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

private fun addDigit(current: String, digit: String): String {
    // If no comma yet, just add digit (max 4 euros)
    if (!current.contains(",")) {
        if (current.length >= 4) return current
        return current + digit
    }

    // After comma: max 2 cents
    val parts = current.split(",")
    val cents = parts[1]
    if (cents.length >= 2) return current

    return current + digit
}

private fun addComma(current: String): String {
    // Already has comma? Don't add again
    if (current.contains(",")) return current

    // Empty? Add "0,"
    if (current.isEmpty()) return "0,"

    // Otherwise add comma
    return "$current,"
}

private fun addQuantity(current: String, digit: String): String {
    // If default value "1", replace it instead of appending
    if (current == "1") return digit
    // Max 3 digits for quantity (up to 999)
    if (current.length >= 3) return current
    // Can't start with 0
    if (current == "0") return digit
    return current + digit
}

private fun parsePrice(text: String): Float {
    if (text.isEmpty()) return 0f
    return try {
        val normalized = text.replace(",", ".")
        normalized.toFloatOrNull()?.takeIf { it in 0.05f..9999.99f } ?: 0f
    } catch (e: Exception) {
        0f
    }
}
