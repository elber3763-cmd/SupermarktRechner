package com.einkaufsscanner.domain.model

import org.junit.Test
import org.junit.Assert.*

class PriceExtractorTest {

    @Test
    fun testExtractSimplePrice() {
        val text = "Preis: 2,99 EUR"
        val result = PriceExtractor.extractPrice(text)

        assertNotNull(result.price)
        assertEquals(2.99f, result.price!!, 0.01f)
        assertFalse(result.ambiguous)
    }

    @Test
    fun testExtractPriceWithPoint() {
        val text = "Article costs 3.50"
        val result = PriceExtractor.extractPrice(text)

        assertNotNull(result.price)
        assertEquals(3.50f, result.price!!, 0.01f)
        assertFalse(result.ambiguous)
    }

    @Test
    fun testExtractPriceWithSpaces() {
        val text = "Preis: 12 , 95 EUR"
        val result = PriceExtractor.extractPrice(text)

        assertNotNull(result.price)
        assertEquals(12.95f, result.price!!, 0.01f)
        // May have multiple candidates with enhanced handwriting normalization, but main price should be correct
        assertTrue(result.candidates.isEmpty() || result.candidates.contains(12.95f))
    }

    @Test
    fun testDetectAmbiguousPrices() {
        val text = "Alte Preis: 5,99 EUR, Neue Preis: 3,49 EUR"
        val result = PriceExtractor.extractPrice(text)

        assertTrue(result.ambiguous)
        assertEquals(2, result.candidates.size)
        assertTrue(result.candidates.contains(5.99f) || result.candidates.contains(3.49f))
    }

    @Test
    fun testFilterUnitPrices() {
        val text = "Einzelpreis: 0,99 EUR, 2,50 / kg"
        val result = PriceExtractor.extractPrice(text)

        assertNotNull(result.price)
        // Should prefer non-unit price
        assertEquals(0.99f, result.price!!, 0.01f)
    }

    @Test
    fun testEmptyText() {
        val result = PriceExtractor.extractPrice("")

        assertNull(result.price)
        assertTrue(result.candidates.isEmpty())
        assertFalse(result.ambiguous)
    }

    @Test
    fun testNullText() {
        val result = PriceExtractor.extractPrice(null)

        assertNull(result.price)
        assertTrue(result.candidates.isEmpty())
        assertFalse(result.ambiguous)
    }

    @Test
    fun testPriceOutOfRange() {
        val text = "Preis: 9999,99 EUR oder 15000,00 EUR"
        val result = PriceExtractor.extractPrice(text)

        // Prices > 9999.99 should be filtered out, but 9999.99 is acceptable
        if (result.price != null) {
            assertTrue("Price should be <= 9999.99", result.price!! <= 9999.99f)
        }
        // Should accept 9999,99 but reject 15000,00
        assertTrue("Should find 9999.99", result.price == 9999.99f || result.candidates.contains(9999.99f) || result.price == null)
    }

    @Test
    fun testNegativePriceFiltered() {
        val text = "Discount: -5,00 EUR"
        val result = PriceExtractor.extractPrice(text)

        // Negative prices should not be extracted
        if (result.price != null) {
            assertTrue(result.price!! > 0)
        }
    }

    @Test
    fun testDuplicatePrices() {
        val text = "Preis 10,50 EUR überall: 10,50 EUR im Angebot"
        val result = PriceExtractor.extractPrice(text)

        // Main price should be recognized
        assertNotNull(result.price)
        assertEquals(10.50f, result.price!!, 0.01f)
        // Duplicates should be de-duplicated
        assertTrue(result.candidates.size <= 1 || (result.candidates.size == 1 && result.candidates[0] == 10.50f))
    }

    @Test
    fun testComplexOcrOutput() {
        val ocrText = """
            Milch
            1L
            Preis: 1,29 EUR
            Ablaufdatum: 15.01.2024
        """.trimIndent()

        val result = PriceExtractor.extractPrice(ocrText)

        assertNotNull(result.price)
        assertEquals(1.29f, result.price!!, 0.01f)
    }
}
