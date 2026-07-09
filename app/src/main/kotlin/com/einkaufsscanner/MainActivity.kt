package com.einkaufsscanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.einkaufsscanner.data.camera.CameraManager
import com.einkaufsscanner.presentation.ui.composables.IntroScreenSpectacular
import com.einkaufsscanner.presentation.ui.composables.ManualPriceEntryDialog
import com.einkaufsscanner.presentation.ui.composables.ShoppingCartScreen
import com.einkaufsscanner.presentation.ui.composables.SettingsScreen
import com.einkaufsscanner.presentation.viewmodel.ShoppingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var cameraManager: CameraManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Kamera-Berechtigung wird benötigt", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel: ShoppingViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsState()
                    var showSettings by remember { mutableStateOf(false) }
                    var showIntro by remember { mutableStateOf(true) }

                    // Show Toast when error message appears
                    val context = LocalContext.current
                    LaunchedEffect(uiState.errorMessage) {
                        uiState.errorMessage?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }

                    // NEW: Connect camera's real-time text recognition to viewModel
                    cameraManager.onTextRecognized = { recognizedText ->
                        viewModel.processRecognizedText(recognizedText)
                    }

                    if (showIntro) {
                        IntroScreenSpectacular(
                            onIntroComplete = { showIntro = false }
                        )
                    } else if (showSettings) {
                        SettingsScreen(
                            onNavigateBack = { showSettings = false }
                        )
                    } else {
                        ShoppingCartScreen(
                            viewModel = viewModel,
                            cameraManager = cameraManager,
                            onScanPrice = {
                                viewModel.activateCamera()
                            },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}
