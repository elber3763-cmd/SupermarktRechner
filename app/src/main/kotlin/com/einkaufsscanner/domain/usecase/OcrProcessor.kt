package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "OcrProcessor"

object OcrProcessor {
    // DEBUG FLAG: Set to true to test full image without crop
    private const val DEBUG_FULL_IMAGE_MODE = true

    /**
     * Optimized local OCR using ML Kit with aggressive handwriting support
     * Uses binarization and local processing - no cloud dependency
     * Fast, offline, and 100% free
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        recognizeTextWithMlKit(bitmap, continuation)
    }

    /**
     * Recognize text with detailed structure (blocks, lines, etc.)
     * Returns: Pair<allText, textBlocks>
     */
    suspend fun recognizeTextWithDetails(bitmap: Bitmap): Pair<String, List<String>> = suspendCancellableCoroutine { continuation ->
        recognizeTextWithMlKitDetails(bitmap, continuation)
    }

    private fun recognizeTextWithMlKit(bitmap: Bitmap, continuation: kotlinx.coroutines.CancellableContinuation<String>) {
        try {
            // ========== EXTENSIVE LOGGING FOR DEBUGGING ==========
            Log.d("OCR_TEST", "========== OCR Processing Started ==========")
            Log.d("OCR_TEST", "Bitmap dimensions: ${bitmap.width}x${bitmap.height}")
            Log.d("OCR_TEST", "Bitmap config: ${bitmap.config}")
            Log.d("OCR_TEST", "DEBUG_FULL_IMAGE_MODE: $DEBUG_FULL_IMAGE_MODE")

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // TEST: Try rotation 0 degrees fixed
            Log.d("OCR_TEST", "Creating InputImage with rotation=0 (fixed for testing)")
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val rawText = result.text

                    // ========== DETAILED LOGGING ==========
                    Log.d("OCR_TEST", "========== OCR Result ==========")
                    Log.d("OCR_TEST", "Raw text length: ${rawText.length} characters")
                    Log.d("OCR_TEST", "Raw text (unfiltered): '$rawText'")
                    Log.d("OCR_TEST", "Text blocks found: ${result.textBlocks.size}")

                    // Log each text block
                    result.textBlocks.forEachIndexed { blockIndex, block ->
                        Log.d("OCR_TEST", "  Block $blockIndex: '${block.text.take(50)}'")
                        block.lines.forEachIndexed { lineIndex, line ->
                            Log.d("OCR_TEST", "    Line $lineIndex: '${line.text}'")
                        }
                    }

                    if (rawText.isEmpty()) {
                        Log.w("OCR_TEST", "⚠️  WARNING: No text recognized! Image might be:")
                        Log.w("OCR_TEST", "  - Too dark/bright")
                        Log.w("OCR_TEST", "  - Rotated incorrectly")
                        Log.w("OCR_TEST", "  - Cropped too small")
                        Log.w("OCR_TEST", "  - Blurry or low quality")
                    }

                    // Apply post-processing but preserve original colors/text quality
                    val cleanedText = com.einkaufsscanner.domain.model.PriceExtractor.cleanOcrTextForHandwriting(rawText)
                    Log.d("OCR_TEST", "After cleaning: '$cleanedText'")

                    val enhancedText = EnhancedOcrProcessor.postProcessHandwriting(cleanedText)
                    Log.d("OCR_TEST", "After enhancement: '$enhancedText'")
                    Log.d("OCR_TEST", "========== OCR Complete ==========")

                    continuation.resume(enhancedText)
                }
                .addOnFailureListener { exception ->
                    Log.e("OCR_TEST", "❌ ML Kit OCR FAILED", exception)
                    Log.e("OCR_TEST", "Exception type: ${exception.javaClass.simpleName}")
                    Log.e("OCR_TEST", "Exception message: ${exception.message}")
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            Log.e("OCR_TEST", "❌ ML Kit setup FAILED", e)
            Log.e("OCR_TEST", "Exception type: ${e.javaClass.simpleName}")
            Log.e("OCR_TEST", "Exception message: ${e.message}")
            continuation.resumeWithException(e)
        }
    }

    private fun recognizeTextWithMlKitDetails(bitmap: Bitmap, continuation: kotlinx.coroutines.CancellableContinuation<Pair<String, List<String>>>) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    // Extract all text lines separately for article name extraction
                    val textLines = result.textBlocks
                        .flatMap { block -> block.lines }
                        .map { line -> line.text.trim() }
                        .filter { it.isNotEmpty() }

                    val rawText = result.text
                    val cleanedText = com.einkaufsscanner.domain.model.PriceExtractor.cleanOcrTextForHandwriting(rawText)
                    val enhancedText = EnhancedOcrProcessor.postProcessHandwriting(cleanedText)

                    continuation.resume(Pair(enhancedText, textLines))
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Synchronous version with timeout and handwriting support
     * For testing or non-async contexts
     */
    fun recognizeTextSync(bitmap: Bitmap, timeoutMs: Long = 10000): String? {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            var result: String? = null

            val task = recognizer.process(image)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (task.isComplete) {
                    result = if (task.isSuccessful) {
                        val rawText = task.result.text
                        // Apply handwriting enhancements
                        EnhancedOcrProcessor.postProcessHandwriting(rawText)
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
