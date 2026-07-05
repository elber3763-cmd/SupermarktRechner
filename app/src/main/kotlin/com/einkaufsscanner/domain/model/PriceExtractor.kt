package com.einkaufsscanner.domain.model

import android.util.Log
import kotlin.math.roundToInt

data class PriceResult(
    val price: Float? = null,
    val candidates: List<Float> = emptyList(),
    val ambiguous: Boolean = false,
)

object PriceExtractor {
    private const val TAG = "PriceExtractor"

    // German price format: 2,49 / 2,50 / 12,99 (comma as decimal separator)
    private val PRICE_PATTERN_GERMAN = Regex("""(?<!\d)(\d{1,4}),(\d{2})(?!\d)""")

    // Fallback: alternative separators (period, hyphen, space) and 1-digit cents
    private val PRICE_PATTERN_ALT_SEP = Regex("""(?<!\d)(\d{1,4})[ \t]*[.\-][ \t]*(\d{1,2}|-)(?!\d)""")

    // For "2 49" (space-separated with 2 digits)
    private val PRICE_PATTERN_SPACE = Regex("""(?<!\d)(\d{1,4})[ \t]+(\d{2})(?!\d)""")

    private val UNIT_TAIL_PATTERN = Regex("""^\s*/""")

    data class PriceCandidate(
        val value: Float,
        val isUnit: Boolean,
    )

    fun extractPrice(text: String?): PriceResult {
        Log.d(TAG, "Extracting price from text: '$text'")
        if (text.isNullOrEmpty()) {
            return PriceResult()
        }

        // Normalize OCR artifacts systematically
        val cleaned = normalizeOcrErrors(text)

        val candidates = findPriceCandidates(cleaned)
        Log.d(TAG, "Found ${candidates.size} candidates: $candidates")
        
        if (candidates.isEmpty()) {
            return PriceResult()
        }

        // Prefer prices that are not units (e.g., "2,50/kg")
        val realPrices = candidates.filter { !it.isUnit }.map { it.value }
        val values = if (realPrices.isNotEmpty()) realPrices else candidates.map { it.value }

        val distinct = values.distinct()
        val ambiguous = distinct.size > 1

        Log.d(TAG, "Final result: price=${distinct.firstOrNull()}, ambiguous=$ambiguous")

        return PriceResult(
            price = distinct.firstOrNull(),
            candidates = distinct,
            ambiguous = ambiguous,
        )
    }

    private fun normalizeOcrErrors(text: String): String {
        var result = text
        // Replace common OCR confusions: Letters that look like numbers
        result = result.replace(Regex("[Oo]"), "0")  // O/o -> 0
        result = result.replace(Regex("[Il]"), "1")  // I/l -> 1
        result = result.replace(Regex("[Ss]"), "5")  // S/s -> 5
        result = result.replace(Regex("B"), "8")     // B -> 8
        result = result.replace(Regex("G"), "6")     // G -> 6
        result = result.replace(Regex("[Zz]"), "2")  // Z/z -> 2

        // Normalize price separators: "12 . 95" or "12 , 95" -> "12,95"
        // Only normalize separators with 1-2 digits after (not dates)
        result = result.replace(Regex("""(\d{1,4})\s*[.,]\s*(\d{1,2})(?!\d)"""), "$1,$2")

        return result
    }

    private fun findPriceCandidates(text: String): List<PriceCandidate> {
        val candidates = mutableListOf<PriceCandidate>()
        val seenCents = mutableSetOf<Int>()

        fun processMatch(match: MatchResult) {
            val euros = match.groupValues[1].toIntOrNull() ?: return
            val centsPart = match.groupValues[2]

            val cents = when {
                centsPart == "-" -> 0
                centsPart.length == 1 -> centsPart.toInt() * 10
                else -> centsPart.toIntOrNull() ?: 0
            }

            val value = euros + (cents / 100f)

            // Validate price range (0.05 to 999.00)
            if (value < 0.05f || value > 999f) {
                return
            }

            val key = (value * 100).roundToInt()
            val tail = text.substring(match.range.last + 1, minOf(text.length, match.range.last + 9))
            val isUnit = UNIT_TAIL_PATTERN.containsMatchIn(tail)

            if (key !in seenCents) {
                seenCents.add(key)
                candidates.add(PriceCandidate(value, isUnit))
            }
        }

        // Try German format first (comma-separated)
        PRICE_PATTERN_GERMAN.findAll(text).forEach { processMatch(it) }

        // Fallback: alternative separators
        if (candidates.isEmpty()) {
            PRICE_PATTERN_ALT_SEP.findAll(text).forEach { processMatch(it) }
            PRICE_PATTERN_SPACE.findAll(text).forEach { processMatch(it) }
        }

        return candidates
    }
}
