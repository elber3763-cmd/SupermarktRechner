package com.einkaufsscanner.data.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX operations for capturing photos and real-time analysis.
 * Uses native ImageProxy + ML Kit directly (NO manual bitmap conversion)
 */
class CameraManager(
    private val context: Context,
) : LifecycleObserver {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Callback receives recognized text directly from ML Kit
    var onTextRecognized: ((String) -> Unit)? = null


    fun setUp(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        Log.d("CameraManager", "🎥 setUp() called - requesting camera provider")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            Log.d("CameraManager", "✅ Camera provider obtained - binding use cases")
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(previewView, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll()
            Log.d("CameraManager", "All previous camera bindings unbound")

            val boundCamera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            if (boundCamera != null) {
                Log.d("CameraManager", "✅ Camera successfully bound to lifecycle with preview + imageCapture ONLY")
            } else {
                Log.e("CameraManager", "❌ Failed to bind camera - returned null")
            }
        } catch (exc: Exception) {
            Log.e("CameraManager", "❌ Error binding camera to lifecycle", exc)
            exc.printStackTrace()
        }
    }

    /**
     * MANUAL PHOTO CAPTURE: Trigger ImageCapture callback to grab frame directly
     * This is the robust standard pattern recommended by Google
     */
    fun takePhoto() {
        Log.d("CameraManager", "📷 takePhoto() called - initiating image capture")
        val imageCapture = imageCapture ?: run {
            Log.e("CameraManager", "❌ ImageCapture is null!")
            return
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    Log.d("CameraManager", "✅ Image captured successfully: ${imageProxy.width}x${imageProxy.height}")
                    analyzeImageWithMlKit(imageProxy)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraManager", "❌ ImageCapture failed", exception)
                }
            }
        )
    }

    /**
     * NATIVE ML KIT INTEGRATION - Process ImageProxy directly without bitmap conversion
     * This is the Google-recommended approach for best OCR accuracy
     * CRITICAL: imageProxy.close() MUST be called in finally block to prevent stream blocking
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeImageWithMlKit(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                // CRITICAL: Use native MediaImage + rotation metadata directly
                // ML Kit reads the perfect resolution and correct rotation automatically!
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                Log.d("CameraAnalyzer", "Analyzing image: ${mediaImage.width}x${mediaImage.height}, rotation=$rotationDegrees°")

                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        Log.d("CameraAnalyzer", "OCR recognized: '$recognizedText'")

                        // Call the NEW callback with recognized text
                        val listener = onTextRecognized
                        if (listener != null && recognizedText.isNotEmpty()) {
                            Log.d("CameraAnalyzer", "Text callback invoked with: '$recognizedText'")
                            listener(recognizedText)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CameraAnalyzer", "ML Kit OCR failed", exception)
                    }
            } else {
                Log.w("CameraAnalyzer", "MediaImage is null!")
            }
        } catch (e: Exception) {
            Log.e("CameraAnalyzer", "Error in analyzeImageWithMlKit", e)
        } finally {
            // ABSOLUTELY CRITICAL: Close in finally block to guarantee it always happens
            // If not closed, camera stream blocks and no more frames are processed!
            try {
                imageProxy.close()
                Log.d("CameraAnalyzer", "ImageProxy closed successfully")
            } catch (e: Exception) {
                Log.e("CameraAnalyzer", "Error closing ImageProxy", e)
            }
        }
    }


    /**
     * Completely unbind camera to shut down sensor and free resources
     * Call this when camera is no longer needed to save battery
     */
    fun unbindCamera() {
        try {
            cameraProvider?.unbindAll()
            imageCapture = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        unbindCamera()
        cameraExecutor.shutdown()
    }
}
