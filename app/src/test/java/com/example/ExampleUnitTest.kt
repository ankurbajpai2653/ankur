package com.example

import com.example.util.ImeiUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLuhnCheckDigitCalculation() {
        // TAC + Serial: 86012304123456
        val base14 = "86012304123456"
        val checkDigit = ImeiUtils.calculateLuhnCheckDigit(base14)
        val fullImei = "$base14$checkDigit"
        assertEquals(15, fullImei.length)
        assertTrue(ImeiUtils.isValidImei(fullImei))
    }

    @Test
    fun testRandomImeiGenerationIsValid() {
        for (i in 1..20) {
            val imei = ImeiUtils.generateRandomValidImei()
            assertEquals(15, imei.length)
            assertTrue("Generated IMEI $imei should satisfy Luhn check", ImeiUtils.isValidImei(imei))
        }
    }

    @Test
    fun testInvalidImeiDetection() {
        assertFalse(ImeiUtils.isValidImei("12345"))
        assertFalse(ImeiUtils.isValidImei("abcdefghijklmno"))
        // Valid 14 digits with wrong check digit
        val base14 = "86012304123456"
        val validCheck = ImeiUtils.calculateLuhnCheckDigit(base14)
        val wrongCheck = (validCheck + 1) % 10
        assertFalse(ImeiUtils.isValidImei("$base14$wrongCheck"))
    }
}
