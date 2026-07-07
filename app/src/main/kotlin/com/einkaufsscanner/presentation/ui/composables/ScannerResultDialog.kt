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
    onConfirm: (price: Float, name: String, quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(detectedPrice?.let { String.format("%.2f", it) } ?: "") }
    var articleName by remember { mutableStateOf(detectedName ?: "") }
    var quantityText by remember { mutableStateOf("1") }

    val parsedPrice = try {
        priceText.replace(",", ".").toFloatOrNull() ?: 0f
    } catch (e: Exception) {
        0f
    }

    val quantity = quantityText.toIntOrNull() ?: 1
    val totalPrice = parsedPrice * quantity
    val isNameValid = if (hasSelectedManualItem) true else articleName.isNotBlank()
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
                    if (isEditMode) "Artikel bearbeiten" else if (hasSelectedManualItem) "Preis erfassen" else "Artikel erfassen",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ========== ARTICLE NAME FIELD (HIDDEN IF MANUAL ITEM SELECTED) ==========
                if (!hasSelectedManualItem) {
                    OutlinedTextField(
                        value = articleName,
                        onValueChange = { articleName = it },
                        label = { Text("Artikelname *") },
                        placeholder = { Text("z.B. Apfel, Bio-Milch, ...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        isError = articleName.isNotEmpty() && !isNameValid
                    )
                }

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
                    onValueChange = { newValue ->
                        quantityText = newValue.filter { it.isDigit() }.takeIf { it.isNotEmpty() } ?: "1"
                    },
                    label = { Text("Menge") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        QuantityButton("1", Modifier.weight(1f)) { quantityText = "1" }
                        QuantityButton("2", Modifier.weight(1f)) { quantityText = "2" }
                        QuantityButton("3", Modifier.weight(1f)) { quantityText = "3" }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("4", Modifier.weight(1f)) { quantityText = "4" }
                        QuantityButton("5", Modifier.weight(1f)) { quantityText = "5" }
                        QuantityButton("6", Modifier.weight(1f)) { quantityText = "6" }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("7", Modifier.weight(1f)) { quantityText = "7" }
                        QuantityButton("8", Modifier.weight(1f)) { quantityText = "8" }
                        QuantityButton("9", Modifier.weight(1f)) { quantityText = "9" }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuantityButton("C", Modifier.weight(1f)) { quantityText = "1" }
                        QuantityButton("0", Modifier.weight(1f)) { quantityText = addQuantityDigit(quantityText, "0") }
                        QuantityButton("⌫", Modifier.weight(1f)) { quantityText = quantityText.dropLast(1).takeIf { it.isNotEmpty() } ?: "1" }
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
                            onConfirm(parsedPrice, articleName.trim(), quantity)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = isNameValid && isPriceValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        Text(
                            "OK",
                            color = if (isNameValid && isPriceValid) Color.White else Color.Gray,
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
