package com.einkaufsscanner.presentation.ui.composables

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import com.einkaufsscanner.data.camera.CameraManager
import com.einkaufsscanner.domain.model.CartItem
import com.einkaufsscanner.presentation.viewmodel.ShoppingViewModel
import com.einkaufsscanner.presentation.viewmodel.ShoppingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreen(
    viewModel: ShoppingViewModel = hiltViewModel(),
    cameraManager: CameraManager,
    onScanPrice: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerWidth by viewModel.scannerWidthPercent.collectAsState()
    val scannerHeight by viewModel.scannerHeightPercent.collectAsState()
    val isCameraActive = uiState.isCameraActive

    // Auto-shutdown camera when inactive to save battery and free resources
    LaunchedEffect(isCameraActive) {
        if (!isCameraActive) {
            // Unbind all camera use cases to completely shut down camera
            cameraManager.unbindCamera()
            Log.d("CartScreen", "🔴 Camera deactivated - unbinding all use cases")
        }
    }

    // UNIFIED Scanner Result Dialog - used for ALL scan types AND editing
    // Shows same layout whether price was detected or not, or when editing an item
    if (uiState.showScannerResultDialog) {
        ScannerResultDialog(
            detectedPrice = uiState.detectedPrice,
            detectedName = uiState.editingItemName,
            isEditMode = uiState.editingItemId != null,
            onConfirm = { price, name, quantity ->
                viewModel.addItemFromScannerResult(price, name, quantity)
            },
            onDismiss = {
                if (uiState.editingItemId != null) {
                    viewModel.cancelEditingItem()
                } else {
                    viewModel.closeScannerResultDialog()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // Header with Settings button and Clear Cart button
        TopAppBar(
            title = { Text("Einkaufs-Scanner v1.0") },
            actions = {
                IconButton(onClick = {
                    // Show confirmation dialog
                    viewModel.showClearCartConfirmation()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Liste leeren",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Einstellungen",
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF009688),
                titleContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )

        // Clear Cart Confirmation Dialog
        if (uiState.showClearCartConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.hideClearCartConfirmation() },
                title = { Text("Liste leeren?") },
                text = { Text("Alle Artikel werden gelöscht. Diese Aktion kann nicht rückgängig gemacht werden.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearCart()
                            viewModel.hideClearCartConfirmation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Ja, löschen", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideClearCartConfirmation() }) {
                        Text("Abbrechen")
                    }
                }
            )
        }

        // Camera Preview Area - Dynamic height with weight(0.5f) to avoid pushing buttons off screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .background(Color.Black),
        ) {
            if (isCameraActive) {
                CameraPreviewArea(
                    cameraManager = cameraManager,
                    onScanPrice = onScanPrice,
                    scannerWidthPercent = scannerWidth,
                    scannerHeightPercent = scannerHeight,
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Kamera ist aus.\nTippe auf 'Preis scannen', um sie zu starten.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Handwriting optimization hint
                    Text(
                        "💡 Hinweis: Bitte Preise groß und in Blockschrift scannen.",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                    )
                }
            }

            // Close Button (Top Right) - Outside camera feed, positioned in corner
            if (isCameraActive) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.deactivateCamera() },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Kamera schließen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Shopping Cart List - Dynamic with weight(1f) to fill remaining space
        CartListSection(
            items = uiState.items,
            onRemoveItem = { viewModel.removeItem(it) },
            onEditItem = { viewModel.startEditingItem(it) },
            modifier = Modifier.weight(1f),
        )

        // Bottom Action Bar - Fixed height, always visible at bottom
        BottomActionBar(
            total = uiState.total,
            viewModel = viewModel,
            onScanPrice = {
                if (!isCameraActive) {
                    viewModel.activateCamera()
                } else {
                    onScanPrice()
                }
            },
            onManualEntry = {
                viewModel.openManualInputDialog()
            },
            isLoading = uiState.isLoading,
            isCameraActive = isCameraActive,
            cameraManager = cameraManager,
            modifier = Modifier.fillMaxWidth()
        )
    }



    // Show loading indicator
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF009688))
        }
    }
}

@Composable
fun CameraPreviewArea(
    cameraManager: CameraManager,
    onScanPrice: () -> Unit,
    scannerWidthPercent: Float = 0.98f,
    scannerHeightPercent: Float = 0.84f,
) {
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    Log.d("CameraPreviewArea", "🎥 CameraPreviewArea composing - setting up camera")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                Log.d("CameraPreviewArea", "📱 AndroidView factory called - creating PreviewView")
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    Log.d("CameraPreviewArea", "📱 Calling cameraManager.setUp()")
                    cameraManager.setUp(this, lifecycleOwner)
                    Log.d("CameraPreviewArea", "✅ cameraManager.setUp() completed")
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                Log.d("CameraPreviewArea", "📱 AndroidView update called")
            }
        )

        // Overlay ROI Frame with dynamic dimensions
        ROIOverlay(
            widthPercent = scannerWidthPercent,
            heightPercent = scannerHeightPercent,
        )
    }
}

@Composable
fun ROIOverlay(
    widthPercent: Float = 0.98f,
    heightPercent: Float = 0.84f,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 3.dp.toPx()
        val color = Color(0xFF009688)

        // Calculate margins to center the ROI
        val xMargin = (1.0f - widthPercent) / 2.0f
        val yMargin = (1.0f - heightPercent) / 2.0f

        val xStart = this.size.width * xMargin
        val xEnd = this.size.width * (1.0f - xMargin)
        val yStart = this.size.height * yMargin
        val yEnd = this.size.height * (1.0f - yMargin)

        // Draw corners
        val cornerSize = 20.dp.toPx()

        // Top-left
        drawLine(color, Offset(xStart, yStart), Offset(xStart + cornerSize, yStart), strokeWidth)
        drawLine(color, Offset(xStart, yStart), Offset(xStart, yStart + cornerSize), strokeWidth)

        // Top-right
        drawLine(color, Offset(xEnd, yStart), Offset(xEnd - cornerSize, yStart), strokeWidth)
        drawLine(color, Offset(xEnd, yStart), Offset(xEnd, yStart + cornerSize), strokeWidth)

        // Bottom-left
        drawLine(color, Offset(xStart, yEnd), Offset(xStart + cornerSize, yEnd), strokeWidth)
        drawLine(color, Offset(xStart, yEnd), Offset(xStart, yEnd - cornerSize), strokeWidth)

        // Bottom-right
        drawLine(color, Offset(xEnd, yEnd), Offset(xEnd - cornerSize, yEnd), strokeWidth)
        drawLine(color, Offset(xEnd, yEnd), Offset(xEnd, yEnd - cornerSize), strokeWidth)
    }
}

@Composable
fun CartListSection(
    items: List<CartItem>,
    onRemoveItem: (Long) -> Unit,
    onEditItem: (CartItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            CartItemRow(
                item = item,
                onEdit = { onEditItem(item) },
                onRemove = { onRemoveItem(item.id) },
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = String.format("%.2f EUR", item.price),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp),
            )

            // Edit Button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    tint = Color(0xFF1976D2),
                )
            }

            // Delete Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = Color(0xFFE53935),
                )
            }
        }
    }
}

@Composable
fun BottomActionBar(
    total: Float,
    viewModel: ShoppingViewModel,
    onScanPrice: () -> Unit,
    onManualEntry: () -> Unit,
    isLoading: Boolean,
    isCameraActive: Boolean,
    cameraManager: CameraManager? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        color = Color(0xFF009688),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                text = viewModel.formatEur(total),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onManualEntry() },
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    enabled = !isLoading,
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFF009688))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manuell", color = Color(0xFF009688), fontSize = 13.sp, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (isCameraActive && cameraManager != null) {
                            Log.d("BottomActionBar", "📷 Scan button clicked - calling takePhoto()")
                            cameraManager.takePhoto()
                        } else {
                            onScanPrice()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(Alignment.CenterVertically),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    enabled = !isLoading,
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null, tint = Color(0xFF009688))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isCameraActive) "Preis scannen" else "Kamera an",
                        color = Color(0xFF009688),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PriceSelectionDialog(
    candidates: List<Float>,
    onSelectPrice: (Float) -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mehrere Preise erkannt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Im Sucher wurden unterschiedliche Preise gefunden. Bitte wähle den richtigen:")
                candidates.forEach { price ->
                    Button(
                        onClick = { onSelectPrice(price) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(String.format("%.2f EUR", price))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onManual) {
                Text("Manuell")
            }
        },
    )
}
