package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrProcessor {
    private const val TAG = "OcrProcessor"

    /**
     * Run ML Kit text recognition on bitmap
     * Returns recognized text asynchronously using Coroutines
     * Replaces Python's pytesseract.image_to_string
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    Log.d(TAG, "Recognized text: '$text'")
                    continuation.resume(text)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "OCR failed", exception)
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "OCR error", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Alternative: synchronous version with timeout
     * For testing or non-async contexts
     */
    fun recognizeTextSync(bitmap: Bitmap, timeoutMs: Long = 10000): String? {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            var result: String? = null

            val task = recognizer.process(image)
            val startTime = System.currentTimeMillis()

            // Simple polling until completion or timeout
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (task.isComplete) {
                    result = if (task.isSuccessful) {
                        task.result.text
                    } else {
                        null
                    }
                    break
                }
                Thread.sleep(100)
            }

            result
        } catch (e: Exception) {
            null
        }
    }
}
