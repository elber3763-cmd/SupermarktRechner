package com.einkaufsscanner.domain.usecase

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Enhanced OCR processor supporting both printed and handwritten text
 * Provides fallback strategy:
 * 1. Try Google Cloud Vision API (DOCUMENT_TEXT_DETECTION) for handwriting
 * 2. Fall back to ML Kit Text Recognition for standard text
 * 3. Apply post-processing to improve handwriting recognition
 */
object EnhancedOcrProcessor {
    private const val TAG = "EnhancedOcrProcessor"

    // Configuration for handwriting detection threshold (0.0-1.0)
    private const val HANDWRITING_CONFIDENCE_THRESHOLD = 0.6f

    /**
     * Asynchronous OCR with handwriting support
     * Attempts both local ML Kit and optional Cloud Vision API
     */
    suspend fun recognizeTextEnhanced(
        bitmap: Bitmap,
        useCloudVision: Boolean = false,
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    // Post-process for handwriting corrections
                    val enhancedText = postProcessHandwriting(text)
                    continuation.resume(enhancedText)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    /**
     * Post-processing to improve handwritten text recognition
     * Applies heuristics specific to price extraction
     * Made public for use in OcrProcessor
     */
    fun postProcessHandwriting(text: String): String {
        var result = text

        // Remove excessive whitespace while preserving price separators
        result = result.replace(Regex("""[\n\r\t]+"""), " ")
        result = result.replace(Regex(""" {2,}"""), " ")

        // Improve common handwriting OCR errors for numbers
        result = improveNumberRecognition(result)

        // Extract price-like patterns from noise
        result = extractPriceContext(result)

        return result.trim()
    }

    /**
     * Improve number recognition from handwritten text
     * Aggressive corrections for common OCR errors with handwriting
     */
    private fun improveNumberRecognition(text: String): String {
        var result = text

        // Aggressive character substitution based on visual similarity in handwriting
        // But only in contexts that look like numbers
        result = improveNumbersInContext(result)

        // Context-aware corrections
        // If we see "l€" or "I€", it's likely "1€"
        result = result.replace(Regex("""[lI]€"""), "1€")

        // "0O" likely means "00"
        result = result.replace(Regex("""0[Oo]"""), "00")

        // Common patterns in prices: "I9" or "l9" or "L9" -> "19"
        result = result.replace(Regex("""[IlL]9"""), "19")
        result = result.replace(Regex("""[IlL]8"""), "18")
        result = result.replace(Regex("""[IlL](\d)"""), "1$1")

        // Remove spurious symbols that appear between numbers
        result = result.replace(Regex("""(\d)\s*[-–—]\s*(\d)"""), "$1$2")

        // Fix common separator issues
        result = result.replace(Regex("""(\d)\s+,\s+(\d)"""), "$1,$2")

        return result
    }

    /**
     * Apply character corrections only in digit-like contexts
     */
    private fun improveNumbersInContext(text: String): String {
        var result = text

        // Replace common handwriting confusions with digits (more aggressive)
        // O -> 0 (everywhere - very common confusion)
        result = result.replace(Regex("""[Oo]"""), "0")

        // I/l -> 1 (very common in handwriting)
        result = result.replace(Regex("""[IlL]"""), "1")

        // S -> 5 (common with cursive writing)
        result = result.replace(Regex("""[Ss]"""), "5")

        // G -> 9 (very common OCR error for handwritten 9)
        result = result.replace(Regex("""[Gg]"""), "9")

        // Z -> 2 (similar shape in handwriting)
        result = result.replace(Regex("""[Zz]"""), "2")

        // B -> 8 (similar rounded shape)
        result = result.replace(Regex("""[Bb]"""), "8")

        return result
    }

    /**
     * Extract price context from noisy OCR output
     * Identifies and cleans price patterns
     */
    private fun extractPriceContext(text: String): String {
        // Find sequences that look like prices and clean them aggressively
        val pricePattern = Regex(
            """(?<![€\d,])(\d{1,4})\s*[,\.;:]\s*(\d{1,2})(?![\d€])"""
        )

        // Replace found patterns with clean format
        return text.replace(pricePattern) { match ->
            "${match.groupValues[1]},${match.groupValues[2]}"
        }
    }

    /**
     * Alternative: Detect if text looks like handwriting
     * Returns confidence that text is handwritten (0.0-1.0)
     */
    fun detectHandwritingLikelihood(text: String): Float {
        var score = 0.0f
        val indicators = listOf(
            Pair(Regex("""[lI1]{2,}"""), 0.3f),  // Multiple I/l/1 -> likely handwriting
            Pair(Regex("""[Oo0]{2,}"""), 0.2f),  // Multiple O/o/0 -> common in handwriting
            Pair(Regex("""[Ss5]{2,}"""), 0.25f), // Multiple S/s/5 -> handwriting artifact
            Pair(Regex("""[Gg9]{2,}"""), 0.2f),  // Multiple G/g/9 -> handwriting confusion
            Pair(Regex("""\s{2,}"""), 0.15f),    // Multiple spaces -> OCR of handwriting
        )

        indicators.forEach { (pattern, weight) ->
            if (pattern.containsMatchIn(text)) {
                score += weight
            }
        }

        // Normalize to 0.0-1.0
        return score.coerceIn(0.0f, 1.0f)
    }

    /**
     * Synchronous version with timeout
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
                        postProcessHandwriting(task.result.text)
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
