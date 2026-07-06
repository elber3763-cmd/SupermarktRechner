package com.einkaufsscanner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DigitScannerTest {

    @Test
    fun testExtractDigitsOnly_RemovesLettersAndSymbols() {
        val input = "PREIS: 12,99€"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("1299", result)
    }

    @Test
    fun testExtractDigitsOnly_RemovesCommas() {
        val input = "12,99"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("1299", result)
    }

    @Test
    fun testExtractDigitsOnly_OnlyDigits() {
        val input = "299"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("299", result)
    }

    @Test
    fun testExtractDigitsOnly_Empty() {
        val input = ""
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("", result)
    }

    @Test
    fun testExtractDigitsOnly_OnlyLetters() {
        val input = "ABC"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_StandardPrice() {
        val digits = "1299"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("12,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_SingleDigit() {
        val digits = "5"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("0,05", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_TwoDigits() {
        val digits = "99"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("0,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_ThreeDigits() {
        val digits = "199"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("1,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_BelowMinimum() {
        // 0,01€ is below minimum of 0,05€
        val digits = "1"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertNull("Price below 0,05€ should be null", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_ExactlyMinimum() {
        // 0,05€ is exactly the minimum
        val digits = "5"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("0,05", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_LargePrice() {
        val digits = "999999"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("9999,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_ExactlyMaximum() {
        val digits = "999999"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("9999,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_EmptyString() {
        val digits = ""
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertNull("Empty string should be null", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_InvalidFormat() {
        val digits = "ABC"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertNull("Non-digit string should be null", result)
    }

    @Test
    fun testExtractDigitsOnly_WithMultipleCommas() {
        val input = "1,2,9,9"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("1299", result)
    }

    @Test
    fun testExtractDigitsOnly_WithSpecialCharacters() {
        val input = "12.99€ / Stück"
        val result = PriceExtractor.extractDigitsOnly(input)
        assertEquals("1299", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_CommonHandwritingError() {
        // Handwritten "2" might look like others, but once recognized
        val digits = "249"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("2,49", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_CommonPrice() {
        val digits = "1999"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("19,99", result)
    }

    @Test
    fun testFormatLiveDigitsAsPrice_CommonPrice2() {
        val digits = "999"
        val result = PriceExtractor.formatLiveDigitsAsPrice(digits)
        assertEquals("9,99", result)
    }
}
