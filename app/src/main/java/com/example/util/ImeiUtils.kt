package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import kotlin.random.Random

object ImeiUtils {

    /**
     * Common realistic Type Allocation Codes (TAC) for testing
     * 86...: China/Asia devices, 35...: Global devices
     */
    private val POPULAR_TACS = listOf(
        "86012304",
        "35209900",
        "86423403",
        "35920508",
        "86734502"
    )

    /**
     * Checks if a 15-digit IMEI is structurally valid using Luhn's algorithm
     */
    fun isValidImei(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) {
            return false
        }
        val expectedCheckDigit = calculateLuhnCheckDigit(imei.substring(0, 14))
        return (imei[14] - '0') == expectedCheckDigit
    }

    /**
     * Calculates the Luhn check digit for the first 14 digits of an IMEI
     */
    fun calculateLuhnCheckDigit(digits14: String): Int {
        var sum = 0
        for (i in digits14.indices) {
            var digit = digits14[i] - '0'
            // For odd index positions (1-based index 2, 4, 6... i.e. 0-based odd index 1, 3, 5...)
            if (i % 2 != 0) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10)
                }
            }
            sum += digit
        }
        val remainder = sum % 10
        return if (remainder == 0) 0 else 10 - remainder
    }

    /**
     * Generates a completely valid 15-digit IMEI using a random TAC and random serial
     */
    fun generateRandomValidImei(): String {
        val tac = POPULAR_TACS.random()
        val serial = (100000..999999).random().toString()
        val base14 = "$tac$serial"
        val checkDigit = calculateLuhnCheckDigit(base14)
        return "$base14$checkDigit"
    }

    /**
     * Derives a deterministic, valid 15-digit IMEI from the device's unique Android ID.
     * This ensures the app generates a consistent hardware-bound IMEI for the physical or virtual device.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceDerivedImei(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_default_id"
        val hashBytes = MessageDigest.getInstance("SHA-256").digest((androidId + Build.SERIAL).toByteArray())
        
        // Take positive number from hash to generate 6-digit serial
        val hashNum = kotlin.math.abs(
            ((hashBytes[0].toInt() and 0xFF) shl 24) or
            ((hashBytes[1].toInt() and 0xFF) shl 16) or
            ((hashBytes[2].toInt() and 0xFF) shl 8) or
            (hashBytes[3].toInt() and 0xFF)
        )
        val tac = "86012304"
        val serialPart = String.format("%06d", (hashNum % 1000000))
        val base14 = "$tac$serialPart"
        val checkDigit = calculateLuhnCheckDigit(base14)
        return "$base14$checkDigit"
    }
}
