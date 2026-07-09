package com.einkaufsscanner.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * UNIFIED Dialog for ALL scanner result types:
 * - Detected price from scan (gedruckt & handschriftlich)
 * - Manual price entry
 *
 * Features ONE consistent layout with:
 * - Article name field (required, always visible)
 * - Price field (editable or pre-filled)
 * - Quantity field with numpad
 */
@Composable
fun ScannerResultDialog(
    detectedPrice: Float? = null,
    detectedName: String? = null,
    isEditMode: Boolean = false,
    hasSelectedManualItem: Boolean = false,
    quantityText: String = "1",
    onQuantityChange: (String) -> Unit = {},
    onConfirm: (price: Float, name: String, quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember(detectedPrice) { mutableStateOf(detectedPrice?.let { String.format("%.2f", it) } ?: "") }
    var articleName by remember(detectedName) { mutableStateOf(detectedName ?: "") }

    val parsedPrice = try {
        priceText.replace(",", ".").toFloatOrNull() ?: 0f
    } catch (e: Exception) {
        0f
    }

    val quantity = try {
        val filtered = quantityText.filter { it.isDigit() }
        val parsed = filtered.toIntOrNull() ?: 1
        android.util.Log.d("ScannerDialog", "quantityText='$quantityText', filtered='$filtered', quantity=$parsed")
        parsed
    } catch (e: Exception) {
        android.util.Log.e("ScannerDialog", "Quantity calculation error", e)
        1
    }
    val totalPrice = parsedPrice * quantity
    val isPriceValid = parsedPrice > 0f

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
                Text(
                    if (isEditMode) "Artikel bearbeiten" else if (hasSelectedManualItem) "Preis erfassen" else "Preis bestätigen",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ========== PRICE FIELD (EDITABLE) ==========
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        priceText = newValue.filter { it.isDigit() || it in ",.".toCharArray() }
                    },
                    label = { Text("Preis €") },
                    placeholder = { Text("z.B. 2,99") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotEmpty() && !isPriceValid
                )

                // ========== QUANTITY FIELD ==========
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Menge") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                // DEBUG: Show current quantity value
                Text(
                    text = "Menge: '$quantityText' → Berechnet: $quantity",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ========== TOTAL ==========
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

                // ========== QUANTITY NUMPAD ==========
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("1", Modifier.weight(1f)) { onQuantityChange("1") }
                        QuantityButton("2", Modifier.weight(1f)) { onQuantityChange("2") }
                        QuantityButton("3", Modifier.weight(1f)) { onQuantityChange("3") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("4", Modifier.weight(1f)) { onQuantityChange("4") }
                        QuantityButton("5", Modifier.weight(1f)) { onQuantityChange("5") }
                        QuantityButton("6", Modifier.weight(1f)) { onQuantityChange("6") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("7", Modifier.weight(1f)) { onQuantityChange("7") }
                        QuantityButton("8", Modifier.weight(1f)) { onQuantityChange("8") }
                        QuantityButton("9", Modifier.weight(1f)) { onQuantityChange("9") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("C", Modifier.weight(1f)) { onQuantityChange("1") }
                        QuantityButton("0", Modifier.weight(1f)) { onQuantityChange(addQuantityDigit(quantityText, "0")) }
                        QuantityButton("⌫", Modifier.weight(1f)) { onQuantityChange(quantityText.dropLast(1).takeIf { it.isNotEmpty() } ?: "1") }
                    }
                }

                // ========== ACTION BUTTONS ==========
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            val finalQuantity = quantityText.filter { it.isDigit() }.toIntOrNull() ?: 1
                            android.util.Log.d("ScannerDialog", "FINAL: quantityText='$quantityText' → finalQuantity=$finalQuantity")
                            // Use articleName if not empty, otherwise default to "Artikel"
                            val nameToSave = articleName.ifBlank { "Artikel" }
                            onConfirm(parsedPrice, nameToSave.trim(), finalQuantity)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = isPriceValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        Text(
                            "OK",
                            color = if (isPriceValid) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(
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

private fun addQuantityDigit(current: String, digit: String): String {
    if (current == "1") return digit
    if (current.length >= 3) return current
    if (current == "0") return digit
    return current + digit
}
