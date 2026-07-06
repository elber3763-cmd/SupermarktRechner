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

@Composable
fun QuantityInputDialog(
    detectedPrice: Float,
    detectedArticleName: String? = null,
    onQuantityConfirm: (quantity: Int, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var articleName by remember { mutableStateOf("") }

    val quantity = quantityText.toIntOrNull() ?: 1
    val totalPrice = detectedPrice * quantity
    val isNameValid = articleName.isNotBlank()

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
                    "Preis, Name & Menge",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Article Name TextField - REQUIRED, always visible for all scan types
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
                    isError = !isNameValid && articleName.isNotEmpty()
                )

                // Price Field (Read-only)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preis (erkannt):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format("%.2f€", detectedPrice),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quantity Field
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp)
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE3F2FD)
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

                // Quantity Number Pad
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

                // Action Buttons
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
                            onQuantityConfirm(quantity, articleName.trim())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = isNameValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        Text(
                            "OK",
                            color = if (isNameValid) Color.White else Color.Gray,
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
    // If default value "1", replace it
    if (current == "1") return digit
    // Max 3 digits for quantity (up to 999)
    if (current.length >= 3) return current
    // Can't start with 0
    if (current == "0") return digit
    return current + digit
}
