package com.einkaufsscanner.presentation.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.einkaufsscanner.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val scannerWidth by viewModel.scannerWidth.collectAsState()
    val scannerHeight by viewModel.scannerHeight.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // Header
        TopAppBar(
            title = { Text("Scanner-Einstellungen") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF009688),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Width Slider
            ScannerSizeSlider(
                label = "Scan-Rechteck Breite",
                value = scannerWidth,
                onValueChange = { viewModel.updateScannerWidth(it) },
                percentage = (scannerWidth * 100).toInt(),
            )

            // Height Slider
            ScannerSizeSlider(
                label = "Scan-Rechteck Höhe",
                value = scannerHeight,
                onValueChange = { viewModel.updateScannerHeight(it) },
                percentage = (scannerHeight * 100).toInt(),
            )

            // Preview Box
            ScannerPreview(
                widthPercent = scannerWidth,
                heightPercent = scannerHeight,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Reset Button
            Button(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                ),
            ) {
                Text("Auf Standard zurücksetzen", color = Color.White)
            }
        }
    }
}

@Composable
fun ScannerSizeSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    percentage: Int,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0.10f..1.0f,  // ← UNLIMITED: 10% to 100% (FULL SCREEN!)
                steps = 89,  // ← More steps for finer control (0.1% precision)
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF009688),
                    activeTrackColor = Color(0xFF009688),
                ),
            )

            Text(
                text = "$percentage%",
                fontSize = 14.sp,
                modifier = Modifier.width(40.dp),
            )
        }
    }
}

@Composable
fun ScannerPreview(
    widthPercent: Float,
    heightPercent: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Vorschau",
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center,
        ) {
            // Preview rectangle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val color = androidx.compose.ui.graphics.Color(0xFF009688)

                val xStart = this.size.width * ((1 - widthPercent) / 2)
                val xEnd = this.size.width * (1 - (1 - widthPercent) / 2)
                val yStart = this.size.height * ((1 - heightPercent) / 2)
                val yEnd = this.size.height * (1 - (1 - heightPercent) / 2)

                val cornerSize = 12.dp.toPx()

                // Top-left
                drawLine(color, androidx.compose.ui.geometry.Offset(xStart, yStart), androidx.compose.ui.geometry.Offset(xStart + cornerSize, yStart), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(xStart, yStart), androidx.compose.ui.geometry.Offset(xStart, yStart + cornerSize), strokeWidth)

                // Top-right
                drawLine(color, androidx.compose.ui.geometry.Offset(xEnd, yStart), androidx.compose.ui.geometry.Offset(xEnd - cornerSize, yStart), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(xEnd, yStart), androidx.compose.ui.geometry.Offset(xEnd, yStart + cornerSize), strokeWidth)

                // Bottom-left
                drawLine(color, androidx.compose.ui.geometry.Offset(xStart, yEnd), androidx.compose.ui.geometry.Offset(xStart + cornerSize, yEnd), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(xStart, yEnd), androidx.compose.ui.geometry.Offset(xStart, yEnd - cornerSize), strokeWidth)

                // Bottom-right
                drawLine(color, androidx.compose.ui.geometry.Offset(xEnd, yEnd), androidx.compose.ui.geometry.Offset(xEnd - cornerSize, yEnd), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(xEnd, yEnd), androidx.compose.ui.geometry.Offset(xEnd, yEnd - cornerSize), strokeWidth)
            }
        }

        Text(
            text = "Breite: ${(widthPercent * 100).toInt()}% | Höhe: ${(heightPercent * 100).toInt()}%",
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
