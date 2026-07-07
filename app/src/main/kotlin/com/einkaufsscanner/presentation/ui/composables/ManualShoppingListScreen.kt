package com.einkaufsscanner.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.einkaufsscanner.data.database.entities.ShoppingItemEntity
import com.einkaufsscanner.presentation.viewmodel.ShoppingViewModel

@Composable
fun ManualShoppingListScreen(
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val items by viewModel.cartItems.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    if (showAddDialog || editingItem != null) {
        AddItemDialog(
            item = editingItem,
            onSave = { name, price, quantity ->
                if (editingItem != null) {
                    viewModel.updateItem(
                        editingItem!!.copy(
                            name = name,
                            price = price,
                            quantity = quantity
                        )
                    )
                } else {
                    viewModel.addManualItem(name, price, quantity)
                }
                showAddDialog = false
                editingItem = null
            },
            onDismiss = {
                showAddDialog = false
                editingItem = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine Artikel in der Liste.\nKlicke auf + um einen neuen Artikel hinzuzufügen.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items) { item ->
                        ShoppingItemRow(
                            item = item,
                            onCheckChange = { isChecked ->
                                viewModel.updateItemCheckedStatus(item.id, isChecked)
                            },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.deleteItem(item.id) },
                        )
                    }
                }
            }
        }

        // FloatingActionButton
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Artikel hinzufügen")
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItemEntity,
    onCheckChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Checkbox
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckChange,
                modifier = Modifier.size(24.dp)
            )

            // Article name with strikethrough if checked
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) Color.Gray else Color.Black,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.price > 0f) {
                        Text(
                            text = String.format("%.2f EUR", item.price),
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                    if (item.quantity > 1) {
                        Text(
                            text = "× ${item.quantity}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }

            // Edit Button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(
    item: ShoppingItemEntity? = null,
    onSave: (String, Float, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(item?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "Neuen Artikel hinzufügen" else "Artikel bearbeiten")
        },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Artikel-Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, 0f, 1)
                    }
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
