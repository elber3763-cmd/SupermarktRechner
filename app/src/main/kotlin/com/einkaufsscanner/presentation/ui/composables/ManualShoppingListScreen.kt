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
    val selectedManualItemId by viewModel.selectedManualItemId.collectAsState()
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
                            isSelected = item.id == selectedManualItemId,
                            onSelect = {
                                if (selectedManualItemId == item.id) {
                                    viewModel.selectManualItem(null)
                                } else {
                                    viewModel.selectManualItem(item.id)
                                }
                            },
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


