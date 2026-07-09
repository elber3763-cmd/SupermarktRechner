package com.einkaufsscanner.presentation.ui.composables

import android.util.Log
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scannerWidth by viewModel.scannerWidthPercent.collectAsState()
    val scannerHeight by viewModel.scannerHeightPercent.collectAsState()
    val logoSize by viewModel.logoSizePercent.collectAsState()
    val labelSize by viewModel.labelSizePercent.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val selectedManualItemId by viewModel.selectedManualItemId.collectAsState()
    val isCameraActive = uiState.isCameraActive
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.einkaufsscanner.data.database.entities.ShoppingItemEntity?>(null) }

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
        val scannerQuantity by viewModel.scannerDialogQuantity.collectAsState()

        ScannerResultDialog(
            detectedPrice = uiState.detectedPrice,
            detectedName = uiState.editingItemName,
            isEditMode = uiState.editingItemId != null,
            hasSelectedManualItem = selectedManualItemId != null,
            quantityText = scannerQuantity,
            onQuantityChange = { viewModel.setScannerDialogQuantity(it) },
            onConfirm = { price, name, quantity ->
                viewModel.addItemFromScannerResult(price, name, quantity)
                viewModel.resetScannerDialogQuantity()
            },
            onDismiss = {
                viewModel.resetScannerDialogQuantity()
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
        // Animated Logo Animations - Premium Edition
        val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")

        val scannerRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scanner_rotation"
        )

        val pulseGlow by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_glow"
        )

        val shimmerValue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        // Header with Settings button and Clear Cart button
        val ctx = context  // Use context from Composable scope
        TopAppBar(
            modifier = Modifier
                .offset(y = (-8).dp)
                .height(68.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp)
                ) {
                    // SPECTACULAR Logo - WOW Edition
                    val dynamicLogoSize = 52.dp * logoSize
                    Box(
                        modifier = Modifier
                            .size(dynamicLogoSize)
                            .padding(end = 4.dp)
                            .graphicsLayer {
                                shadowElevation = 12.dp.toPx()
                            }
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f)
                                    ),
                                    center = Offset(0.5f, 0.5f),
                                    radius = 1f
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 2.dp,
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.2f),
                                        Color(0xFF80DEEA).copy(alpha = 0.4f)
                                    )
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .graphicsLayer {
                                scaleX = 1f + (pulseGlow - 0.5f) * 0.08f
                                scaleY = 1f + (pulseGlow - 0.5f) * 0.08f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val dynamicCanvasSize = 44.dp * logoSize
                        Canvas(modifier = Modifier.size(dynamicCanvasSize)) {
                            val size = (44.dp * logoSize).toPx()
                            val cx = size / 2
                            val cy = size / 2

                            // Multi-layer glow effect
                            val glowRadius1 = (size / 3.2f) * pulseGlow
                            val glowRadius2 = (size / 2.8f) * pulseGlow

                            // Outer pulsing glow
                            drawCircle(
                                color = Color(0xFF80DEEA).copy(alpha = 0.15f * pulseGlow),
                                radius = glowRadius2 * 1.3f,
                                center = Offset(cx, cy)
                            )

                            // Inner glow
                            drawCircle(
                                color = Color(0xFF00BCD4).copy(alpha = 0.2f * pulseGlow),
                                radius = glowRadius2,
                                center = Offset(cx, cy)
                            )

                            // Main ring - gradient effect
                            drawCircle(
                                color = Color.White,
                                radius = glowRadius1,
                                center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(3f)
                            )

                            // Premium center jewel
                            drawCircle(
                                color = Color(0xFFFFD700),
                                radius = 5.5f,
                                center = Offset(cx, cy)
                            )

                            drawCircle(
                                color = Color(0xFFFFF59D),
                                radius = 3f,
                                center = Offset(cx, cy)
                            )

                            // Rotating scan beams with shimmer
                            val angle = (scannerRotation % 360f) * kotlin.math.PI.toFloat() / 180f
                            val beamLength = glowRadius1 * 0.85f

                            // Primary beam - bright and bold
                            for (i in 0..2) {
                                val rotation = angle + (i * 120f * kotlin.math.PI.toFloat() / 180f)
                                val alpha = 1f - (i * 0.2f)
                                drawLine(
                                    color = Color.White.copy(alpha = alpha),
                                    start = Offset(
                                        cx + kotlin.math.cos(rotation) * beamLength,
                                        cy + kotlin.math.sin(rotation) * beamLength
                                    ),
                                    end = Offset(
                                        cx - kotlin.math.cos(rotation) * beamLength,
                                        cy - kotlin.math.sin(rotation) * beamLength
                                    ),
                                    strokeWidth = 2.8f
                                )
                            }

                            // Shimmer effect - moving highlight
                            val shimmerX = cx + kotlin.math.cos(shimmerValue * 2f * kotlin.math.PI.toFloat()) * (glowRadius1 * 0.6f)
                            val shimmerY = cy + kotlin.math.sin(shimmerValue * 2f * kotlin.math.PI.toFloat()) * (glowRadius1 * 0.6f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = 2f,
                                center = Offset(shimmerX, shimmerY)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Premium branding text
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Main brand name - ultra bold and striking
                        Text(
                            text = "SCAN SMART",
                            fontSize = (17.sp * labelSize),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (2.sp * labelSize),
                            lineHeight = (18.sp * labelSize)
                        )

                        // Premium tagline with accent color
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height((12.dp * labelSize))) {
                            Text(
                                text = "◆ ",
                                fontSize = (6.sp * labelSize),
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "PREMIUM",
                                fontSize = (7.sp * labelSize),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF80DEEA),
                                letterSpacing = (1.2.sp * labelSize)
                            )
                        }
                    }
                }
            },
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

                // Neustart Button
                IconButton(onClick = {
                    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Neustart",
                        tint = Color.White,
                    )
                }

                // Beenden Button
                IconButton(onClick = {
                    android.os.Process.killProcess(android.os.Process.myPid())
                }) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Beenden",
                        tint = Color(0xFFFF6B6B),
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

        // Add Item Dialog for manual shopping list
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

        // Unified Screen Layout: Camera on top (only if active), Shopping List below
        Column(modifier = Modifier.weight(1f)) {
            // Camera Preview Area - Only visible if camera is active
            if (isCameraActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .background(Color.Black),
                ) {
                    CameraPreviewArea(
                        cameraManager = cameraManager,
                        onScanPrice = onScanPrice,
                        scannerWidthPercent = scannerWidth,
                        scannerHeightPercent = scannerHeight,
                    )

                    // Close Button (Top Right)
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

            // Integrated Shopping List - takes remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (allItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Keine Artikel in der Einkaufsliste.\nKlicke auf + um einen neuen Artikel hinzuzufügen.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(allItems) { item ->
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

                // FloatingActionButton - Green + button bottom right
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

        // Bottom Action Bar - Fixed height, always visible at bottom
        BottomActionBar(
            total = uiState.total,
            viewModel = viewModel,
            onScanPrice = onScanPrice, // This will call viewModel.activateCamera() as set in MainActivity
            onManualEntry = {
                viewModel.openManualInputDialog()
            },
            isLoading = uiState.isLoading,
            isCameraActive = isCameraActive,
            cameraManager = cameraManager,
            modifier = Modifier.fillMaxWidth()
        )

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
}

@Composable
fun CameraPreviewArea(
    cameraManager: CameraManager,
    onScanPrice: () -> Unit,
    scannerWidthPercent: Float = 0.98f,
    scannerHeightPercent: Float = 0.84f,
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
        ?: throw IllegalStateException("Context must be a LifecycleOwner")

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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text("Manuell", color = Color(0xFF009688), fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (isCameraActive && cameraManager != null) {
                            Log.d("BottomActionBar", "📷 Scan button clicked - calling takePhoto()")
                            viewModel.setLoading(true) // Trigger loading state for feedback
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
                        if (isCameraActive) "Preis" else "Kamera",
                        color = Color(0xFF009688),
                        fontSize = 12.sp,
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


