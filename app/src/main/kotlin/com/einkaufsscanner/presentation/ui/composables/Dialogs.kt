package com.einkaufsscanner.presentation.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.einkaufsscanner.presentation.viewmodel.ShoppingViewModel

@Composable
fun ManualPriceEntryDialog(
    viewModel: ShoppingViewModel,
    onDismiss: () -> Unit,
    prefill: String = "",
) {
    var priceText by remember { mutableStateOf(prefill) }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preis manuell eingeben") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = priceText,
                    onValueChange = { newValue ->
                        // Filter: only digits, comma, and dot
                        priceText = newValue.filter { it.isDigit() || it in ",.".toCharArray() }
                        hasError = false
                    },
                    label = { Text("Preis in EUR (z.B. 2,49)") },
                    placeholder = { Text("2,49") },
                    isError = hasError,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasError) {
                    Text(
                        "Ungültige Eingabe",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedPrice = priceText.replace(",", ".")
                    val price = normalizedPrice.toFloatOrNull()

                    if (price != null && price > 0) {
                        viewModel.addItem(price)
                        onDismiss()
                    } else {
                        hasError = true
                    }
                },
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
fun OcrResultDialog(
    recognizedText: String,
    errorMessage: String,
    onAddManually: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Erkennung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                
                if (recognizedText.isNotBlank()) {
                    Text("Erkannter Text:", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 150.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            recognizedText,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAddManually) {
                Text("Manuell eingeben")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        },
    )
}

@Composable
fun LoadingDialog(visible: Boolean) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Verarbeitung") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preis wird erkannt...")
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
fun InfoDialog(
    appVersion: String = "1.0",
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Einkaufsscanner", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Eine praktische App zum Scannen und Berechnen von Einkaufspreisen mit OCR-Unterstützung.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Version:", style = MaterialTheme.typography.labelMedium)
                    Text(appVersion, style = MaterialTheme.typography.labelMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Entwickler:", style = MaterialTheme.typography.labelMedium)
                    Text("Koorosh", style = MaterialTheme.typography.labelMedium)
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    "© 2026 Koorosh. Alle Rechte vorbehalten.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Schließen")
            }
        },
    )
}
