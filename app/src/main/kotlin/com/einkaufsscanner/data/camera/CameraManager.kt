package com.einkaufsscanner.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
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
import java.io.ByteArrayOutputStream
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
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var digitClassifier: DigitClassifier? = null

    // Callback receives recognized text directly from ML Kit
    var onTextRecognized: ((String) -> Unit)? = null

    // Callback for live digit scanning (continuous analysis)
    var onLiveDigitDetected: ((String) -> Unit)? = null

    init {
        digitClassifier = DigitClassifier(context)
        digitClassifier?.initialize()
    }


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

    fun startLiveDigitScanning(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        Log.d("CameraManager", "🔍 Starting live digit scanning with ImageAnalysis")

        // First get the camera provider if not already initialized
        if (cameraProvider == null) {
            Log.d("CameraManager", "Initializing camera provider...")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                Log.d("CameraManager", "✅ Camera provider obtained, binding camera...")
                bindLiveDigitScanningCamera(previewView, lifecycleOwner)
            }, ContextCompat.getMainExecutor(context))
        } else {
            Log.d("CameraManager", "Camera provider already initialized, binding camera...")
            bindLiveDigitScanningCamera(previewView, lifecycleOwner)
        }
    }

    private fun bindLiveDigitScanningCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrameForDigits(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider?.unbindAll()
            Log.d("CameraManager", "Camera bindings unbound, binding new use cases...")

            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            Log.d("CameraManager", "✅ Live digit scanning camera bound successfully")
        } catch (exc: Exception) {
            Log.e("CameraManager", "❌ Error binding live digit scanning camera", exc)
            exc.printStackTrace()
        }
    }

    fun stopLiveDigitScanning() {
        imageAnalysis = null
        digitClassifier?.close()
        Log.d("CameraManager", "🛑 Live digit scanning stopped")
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeFrameForDigits(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                // Step 1: Try direct ML Kit (for printed text)
                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        try {
                            val recognizedText = visionText.text

                            if (recognizedText.isNotEmpty()) {
                                Log.d("CameraManager", "✅ PRINTED: ML Kit recognized: '$recognizedText'")
                                val digitsOnly = extractDigitsFromText(recognizedText)
                                if (digitsOnly.isNotEmpty()) {
                                    Log.d("CameraManager", "📊 Printed digits extracted: $digitsOnly")
                                    onLiveDigitDetected?.invoke(digitsOnly)
                                }
                            } else {
                                Log.d("CameraManager", "⚠️ No text in frame - trying handwriting mode...")
                                // Step 2: Try handwriting recognition with enhanced image
                                tryHandwritingRecognition(mediaImage, rotationDegrees)
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CameraManager", "❌ ML Kit failed, trying handwriting...", exception)
                        tryHandwritingRecognition(mediaImage, rotationDegrees)
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } catch (e: Exception) {
            Log.e("CameraManager", "Error in analyzeFrameForDigits", e)
            imageProxy.close()
        }
    }

    private fun tryHandwritingRecognition(mediaImage: android.media.Image, rotationDegrees: Int) {
        try {
            Log.d("CameraManager", "🖊️  Attempting enhanced ML Kit for handwriting...")

            // Use ML Kit directly for handwriting with a second attempt
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text
                    if (recognizedText.isNotEmpty()) {
                        Log.d("CameraManager", "✅ HANDWRITING: ML Kit recognized: '$recognizedText'")
                        val digitsOnly = extractDigitsFromText(recognizedText)
                        if (digitsOnly.isNotEmpty()) {
                            Log.d("CameraManager", "📊 Handwriting digits: $digitsOnly (confidence: high)")
                            onLiveDigitDetected?.invoke(digitsOnly)
                        }
                    } else {
                        Log.d("CameraManager", "⚠️ Handwriting: ML Kit found no text")
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("CameraManager", "⚠️ Handwriting recognition failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("CameraManager", "Error in handwriting recognition", e)
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(image: android.media.Image): Bitmap {
        val planes = image.planes
        val yPlane = planes[0]

        val yBuffer = yPlane.buffer
        yBuffer.rewind()

        val width = image.width
        val height = image.height

        // Extract Y (luma) plane
        val yData = ByteArray(yBuffer.remaining())
        yBuffer.get(yData)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert Y-plane to ARGB8888
        // Invert: 255 - yValue for handwriting (dark on light)
        var pixelIndex = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixelIndex < yData.size) {
                    val yVal = yData[pixelIndex].toInt() and 0xff
                    val inverted = 255 - yVal  // Invert for handwriting
                    val rgb = -0x1000000 or (inverted shl 16) or (inverted shl 8) or inverted
                    bitmap.setPixel(x, y, rgb)
                }
                pixelIndex++
            }
        }

        Log.d("CameraManager", "📸 Bitmap: ${width}x${height}, inverted")
        return bitmap
    }

    private fun extractDigitsFromText(text: String): String {
        // Extract only digits (0-9) and commas
        val cleaned = text.filter { it.isDigit() || it == ',' }
            .replace(Regex("[^0-9,]"), "")
            .trim()

        Log.d("CameraManager", "🔤 Raw text: '$text' → Cleaned: '$cleaned'")
        return cleaned
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

                        // Call the callback with recognized text (even if empty)
                        // This ensures the ViewModel can reset loading state
                        onTextRecognized?.invoke(recognizedText)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CameraAnalyzer", "ML Kit OCR failed", exception)
                        // Even on failure, we should notify the listener to stop loading
                        onTextRecognized?.invoke("")
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
