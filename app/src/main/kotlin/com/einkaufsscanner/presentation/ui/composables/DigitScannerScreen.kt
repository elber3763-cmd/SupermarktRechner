package com.einkaufsscanner.presentation.ui.composables

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.einkaufsscanner.data.camera.CameraManager
import com.einkaufsscanner.util.PermissionRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitScannerScreen(
    cameraManager: CameraManager,
    onDigitDetected: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
        ?: throw IllegalStateException("Context must be a LifecycleOwner")

    var detectedDigits by remember { mutableStateOf("") }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var recognitionMode by remember { mutableStateOf("") }
    val UPDATE_THROTTLE_MS = 50L

    LaunchedEffect(Unit) {
        if (!PermissionRequester.hasCameraPermission(context)) {
            Log.w("DigitScanner", "Camera permission not granted")
            onClose()
            return@LaunchedEffect
        }

        cameraManager.onLiveDigitDetected = { digits ->
            val currentTime = System.currentTimeMillis()
            val timeSinceLastUpdate = currentTime - lastUpdateTime

            if (digits.isNotEmpty()) {
                if (timeSinceLastUpdate >= UPDATE_THROTTLE_MS) {
                    detectedDigits = digits
                    lastUpdateTime = currentTime
                    Log.d("DigitScanner", "✅ UI updated with digits: $digits")
                } else {
                    Log.d("DigitScanner", "⏱️  Throttled (${timeSinceLastUpdate}ms < ${UPDATE_THROTTLE_MS}ms): $digits")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopLiveDigitScanning()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview - Full screen background
        LiveCameraPreview(cameraManager, lifecycleOwner)

        // Overlay UI on top of camera
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopAppBar(
                title = { Text("Zahlen scannen") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Halten Sie Zahlen vor die Kamera",
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (detectedDigits.isNotEmpty()) {
                    DigitDisplayBox(detectedDigits)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abbrechen")
                }

                Button(
                    onClick = {
                        if (detectedDigits.isNotEmpty()) {
                            onDigitDetected(detectedDigits)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = detectedDigits.isNotEmpty()
                ) {
                    Text("Übernehmen")
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreview(
    cameraManager: CameraManager,
    lifecycleOwner: LifecycleOwner,
) {
    var previewView: PreviewView? = null

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                previewView = this
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                Log.d("DigitScannerScreen", "📱 PreviewView created, starting camera setup")
                cameraManager.startLiveDigitScanning(this, lifecycleOwner)
                Log.d("DigitScannerScreen", "✅ Camera setup completed")
            }
        },
        update = { view ->
            // Update callback if needed
            Log.d("DigitScannerScreen", "📱 PreviewView update called")
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            Log.d("DigitScannerScreen", "🛑 Disposing PreviewView")
            cameraManager.stopLiveDigitScanning()
        }
    }
}

@Composable
private fun DigitDisplayBox(digits: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Erkannte Zahlen:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            digits,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
