package com.einkaufsscanner.domain.model

import kotlin.math.roundToInt

data class PriceResult(
    val price: Float? = null,
    val candidates: List<Float> = emptyList(),
    val ambiguous: Boolean = false,
    val articleName: String? = null,
)

object PriceExtractor {
    /**
     * HANDWRITING OPTIMIZATION: Convert OCR garbage to readable text
     * Applies aggressive letter-to-digit conversion typical for handwriting OCR errors
     */
    fun cleanOcrTextForHandwriting(rawText: String): String {
        return rawText
            .uppercase()
            .replace(Regex("[Oo]"), "0")  // O → 0
            .replace(Regex("[IlL]"), "1")  // I/l/L → 1
            .replace(Regex("[Ss]"), "5")   // S → 5
            .replace(Regex("[Zz]"), "2")   // Z → 2
            .replace(Regex("[Gg]"), "9")   // G → 9
            .replace(Regex("[Bb]"), "8")   // B → 8
            .replace(Regex("[.]"), ",")    // . → ,
            .replace(Regex("[^0-9,]"), "") // Remove everything except digits and comma
    }

    // German price format: 2,49 / 2,50 / 12,99 (comma as decimal separator)
    private val PRICE_PATTERN_GERMAN = Regex("""(?<!\d)(\d{1,4}),(\d{2})(?!\d)""")

    // Handwriting-tolerant: more flexible separators for handwritten input
    // Accepts: comma, period, semicolon, colon, and even missing separator
    private val PRICE_PATTERN_HANDWRITING = Regex("""(?<!\d)(\d{1,4})[,;:.\s]*(\d{2})(?!\d)""")

    // Fallback: alternative separators (period, hyphen, space) and 1-digit cents
    private val PRICE_PATTERN_ALT_SEP = Regex("""(?<!\d)(\d{1,4})[ \t]*[.\-;:][ \t]*(\d{1,2}|-)(?!\d)""")

    // For "2 49" (space-separated with 2 digits)
    private val PRICE_PATTERN_SPACE = Regex("""(?<!\d)(\d{1,4})[ \t]+(\d{2})(?!\d)""")

    private val UNIT_TAIL_PATTERN = Regex("""^\s*/""")

    data class PriceCandidate(
        val value: Float,
        val isUnit: Boolean,
    )

    /**
     * Extract article name from text lines
     * Returns first non-price line that looks like actual text (not just numbers/symbols)
     * Used to extract product name from receipts/labels
     */
    fun extractArticleName(textLines: List<String>): String? {
        for (line in textLines) {
            val trimmed = line.trim()

            // Skip empty lines
            if (trimmed.isEmpty()) continue

            // Skip if it looks like a price (contains comma or period with digits)
            if (PRICE_PATTERN_GERMAN.containsMatchIn(trimmed) ||
                PRICE_PATTERN_HANDWRITING.containsMatchIn(trimmed)) {
                continue
            }

            // Skip if it's only digits
            if (trimmed.all { it.isDigit() }) continue

            // Skip if it's only special characters/symbols
            if (trimmed.all { !it.isLetterOrDigit() }) continue

            // Skip very short lines (likely noise)
            if (trimmed.length < 2) continue

            // Skip lines that are mostly uppercase single letters (OCR noise)
            val letterCount = trimmed.count { it.isLetter() }
            if (letterCount == 0) continue

            // Found a valid product name!
            return trimmed
        }

        return null
    }

    fun extractPrice(text: String?): PriceResult {
        if (text.isNullOrEmpty()) {
            return PriceResult()
        }

        // Normalize OCR artifacts systematically (two-pass strategy)
        var cleaned = normalizeOcrErrors(text)
        var candidates = findPriceCandidates(cleaned)

        // If no candidates found, try aggressive handwriting normalization
        if (candidates.isEmpty()) {
            cleaned = normalizeHandwriting(text)
            candidates = findPriceCandidates(cleaned)
        }

        // If still empty, try extracting digits from the garbage and converting to prices
        if (candidates.isEmpty()) {
            candidates = extractDigitsAsPrice(text)
        }

        if (candidates.isEmpty()) {
            return PriceResult()
        }

        // Prefer prices that are not units (e.g., "2,50/kg")
        val realPrices = candidates.filter { !it.isUnit }.map { it.value }
        val values = if (realPrices.isNotEmpty()) realPrices else candidates.map { it.value }

        val distinct = values.distinct()
        val ambiguous = distinct.size > 1

        return PriceResult(
            price = distinct.firstOrNull(),
            candidates = distinct,
            ambiguous = ambiguous,
        )
    }

    private fun normalizeOcrErrors(text: String): String {
        var result = text

        // Context-aware corrections (safe, only when adjacent to digits)
        result = result.replace(Regex("""[Oo](?=\d)"""), "0")  // O before digit -> 0
        result = result.replace(Regex("""(?<=\d)[Oo]"""), "0")  // O after digit -> 0
        result = result.replace(Regex("""(?<=\d)[Il](?=\d)"""), "1")  // I/l between digits

        // Normalize price separators: "12 . 95" or "12 , 95" -> "12,95"
        result = result.replace(Regex("""(\d{1,4})\s*[.,;:]\s*(\d{1,2})(?!\d)"""), "$1,$2")

        // Fix cases like "2 99" (space-separated, common in handwriting)
        result = result.replace(Regex("""(\d)\s+(\d{2})(?!\d)"""), "$1,$2")

        // Remove only problematic stray characters
        result = result.replace(Regex("""[`´'ʹ]"""), "")

        return result
    }

    /**
     * Aggressive handwriting-specific normalization
     * Only used if standard normalization fails to find prices
     * Assumes all input is related to price detection
     */
    private fun normalizeHandwriting(text: String): String {
        var result = text

        // AGGRESSIVE character substitution (for handwriting only)
        result = result.replace(Regex("""[Oo]"""), "0")  // O/o -> 0
        result = result.replace(Regex("""[IlL]"""), "1")  // I/l/L -> 1
        result = result.replace(Regex("""[Ss]"""), "5")  // S/s -> 5
        result = result.replace(Regex("""[Gg]"""), "9")  // G/g -> 9
        result = result.replace(Regex("""[Zz]"""), "2")  // Z/z -> 2
        result = result.replace(Regex("""[Bb]"""), "8")  // B/b -> 8

        // Normalize separators
        result = result.replace(Regex("""(\d{1,4})\s*[.,;:]\s*(\d{1,2})(?!\d)"""), "$1,$2")
        result = result.replace(Regex("""(\d)\s+(\d{2})(?!\d)"""), "$1,$2")

        // Clean stray characters
        result = result.replace(Regex("""[`´'ʹ]"""), "")

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

            // Validate price range (0.05 to 9999.99)
            if (value < 0.05f || value > 9999.99f) {
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

        // Try handwriting-tolerant pattern (more flexible)
        if (candidates.isEmpty()) {
            PRICE_PATTERN_HANDWRITING.findAll(text).forEach { processMatch(it) }
        }

        // Fallback: alternative separators and space-separated
        if (candidates.isEmpty()) {
            PRICE_PATTERN_ALT_SEP.findAll(text).forEach { processMatch(it) }
            PRICE_PATTERN_SPACE.findAll(text).forEach { processMatch(it) }
        }

        // Last resort: extract any digit sequences that look like prices
        if (candidates.isEmpty()) {
            extractFuzzyPrices(text, seenCents).forEach { candidates.add(it) }
        }

        return candidates
    }

    /**
     * EXTREME FALLBACK: Convert any digit sequences in garbage text to prices
     * Last resort when handwriting OCR is completely unreliable
     * e.g., "JNASO 0IA1N0" -> extracts digits -> "0,0,1,0" -> tries to make price
     */
    private fun extractDigitsAsPrice(text: String): List<PriceCandidate> {
        val results = mutableListOf<PriceCandidate>()
        val seenCents = mutableSetOf<Int>()

        // Extract ONLY digit characters and character-to-digit mappings
        var digitStr = text
            .replace(Regex("""[Oo]"""), "0")
            .replace(Regex("""[IlL]"""), "1")
            .replace(Regex("""[Ss]"""), "5")
            .replace(Regex("""[Gg]"""), "9")
            .replace(Regex("""[Zz]"""), "2")
            .replace(Regex("""[Bb]"""), "8")
            .replace(Regex("""[^0-9]"""), "")  // Remove everything that's not a digit

        // If we have at least 3-4 digits, try to parse as price
        if (digitStr.length >= 3) {
            // Try different interpretations
            val attempts = listOf(
                // 3 digits: "199" -> 1.99
                if (digitStr.length >= 3) {
                    val euros = digitStr.substring(0, digitStr.length - 2).toIntOrNull() ?: 0
                    val cents = digitStr.substring(digitStr.length - 2).toIntOrNull() ?: 0
                    if (euros in 1..9999 && cents in 0..99) {
                        euros + (cents / 100f)
                    } else null
                } else null,
                // 4 digits: "1299" -> 12.99
                if (digitStr.length >= 4) {
                    val euros = digitStr.substring(0, digitStr.length - 2).toIntOrNull() ?: 0
                    val cents = digitStr.substring(digitStr.length - 2).toIntOrNull() ?: 0
                    if (euros in 1..9999 && cents in 0..99) {
                        euros + (cents / 100f)
                    } else null
                } else null,
                // Take first digit as euros, rest as cents: "299" -> 2.99
                if (digitStr.length >= 3) {
                    val euros = digitStr.first().toString().toIntOrNull() ?: 0
                    val centsStr = digitStr.substring(1)
                    if (centsStr.length == 2) {
                        val cents = centsStr.toIntOrNull() ?: 0
                        euros + (cents / 100f)
                    } else null
                } else null
            ).filterNotNull()

            // Add valid price attempts
            attempts.forEach { price ->
                if (price in 0.05f..9999.99f) {
                    val key = (price * 100).roundToInt()
                    if (key !in seenCents) {
                        seenCents.add(key)
                        results.add(PriceCandidate(price, false))
                    }
                }
            }
        }

        return results
    }

    /**
     * Aggressive fallback: extract prices from very noisy OCR output
     * Looks for any digit patterns that could be prices
     */
    private fun extractFuzzyPrices(text: String, seenCents: MutableSet<Int>): List<PriceCandidate> {
        val results = mutableListOf<PriceCandidate>()

        // Extract all digit sequences as strings first
        val numbers = mutableListOf<Int>()
        Regex("""(\d+)""").findAll(text).forEach { match ->
            val num = match.groupValues[1].toIntOrNull()
            if (num != null) {
                numbers.add(num)
            }
        }

        // Try to find price-like patterns: look for 2-4 digit numbers that could be euros and cents
        for (i in numbers.indices) {
            val num = numbers[i]

            // Single/double digit: could be cents (0-99)
            if (num in 5..99) {
                // Try to find a preceding 1-4 digit number
                if (i > 0) {
                    val prevNum = numbers.getOrNull(i - 1)
                    if (prevNum != null && prevNum in 1..9999) {
                        val price = prevNum + (num / 100f)
                        if (price in 0.05f..9999.99f) {
                            val key = (price * 100).roundToInt()
                            if (key !in seenCents) {
                                seenCents.add(key)
                                results.add(PriceCandidate(price, false))
                            }
                        }
                    }
                }
            }

            // 3-4 digit number: could be price like 199 (1,99) or 1299 (12,99)
            if (num in 100..9999) {
                // Try to parse as price (last 2 digits are cents)
                val euros = num / 100
                val cents = num % 100

                if (euros in 1..9999 && cents in 0..99) {
                    val price = euros + (cents / 100f)
                    if (price in 0.05f..9999.99f) {
                        val key = (price * 100).roundToInt()
                        if (key !in seenCents) {
                            seenCents.add(key)
                            results.add(PriceCandidate(price, false))
                        }
                    }
                }
            }
        }

        return results
    }

    fun extractDigitsOnly(text: String): String {
        return text.filter { it.isDigit() }
    }

    fun formatLiveDigitsAsPrice(digits: String): String? {
        if (digits.isEmpty()) return null

        val normalized = digits.replace(Regex("[^0-9]"), "")
        if (normalized.isEmpty()) return null

        // Always assume last 2 digits are cents
        val digitsString = normalized.padStart(3, '0')  // "5" -> "005", "99" -> "099", "1299" -> "1299"

        val euros = digitsString.substring(0, digitsString.length - 2).toIntOrNull() ?: 0
        val cents = digitsString.substring(digitsString.length - 2).toIntOrNull() ?: 0

        if (cents > 99) return null

        val price = euros + (cents / 100f)

        return if (price < 0.05f || price > 9999.99f) {
            null
        } else {
            String.format("%.2f", price).replace(".", ",")
        }
    }
}
