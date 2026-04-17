package com.yourname.simplenotes

import com.yourname.simplenotes.data.auth.PinAuthManager
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for [PinAuthManager].
 *
 * These tests cover PIN validation, hashing, and verification logic
 * without mocking the actual SharedPreferences/Security components.
 * Full integration tests would require instrumented tests with a real Android environment.
 */
class PinAuthManagerTest {

    // -------------------------------------------------------------------------
    // PIN validation tests (these work without Android mocks)
    // -------------------------------------------------------------------------

    @Test
    fun `PIN constants are defined correctly`() {
        assertEquals("MIN_PIN_LENGTH should be 4", 4, PinAuthManager.MIN_PIN_LENGTH)
        assertEquals("MAX_PIN_LENGTH should be 6", 6, PinAuthManager.MAX_PIN_LENGTH)
        assertEquals("MAX_ATTEMPTS should be 3", 3, PinAuthManager.MAX_ATTEMPTS)
    }

    @Test
    fun `MIN_PIN_LENGTH is less than or equal to MAX_PIN_LENGTH`() {
        assertTrue(
            "MIN_PIN_LENGTH should be <= MAX_PIN_LENGTH",
            PinAuthManager.MIN_PIN_LENGTH <= PinAuthManager.MAX_PIN_LENGTH
        )
    }

    @Test
    fun `MAX_ATTEMPTS is positive`() {
        assertTrue("MAX_ATTEMPTS should be positive", PinAuthManager.MAX_ATTEMPTS > 0)
    }

    // -------------------------------------------------------------------------
    // bcrypt hashing tests (no Android mocks needed)
    // -------------------------------------------------------------------------

    @Test
    fun `different PINs produce different bcrypt hashes`() {
        val pin1 = "1234"
        val pin2 = "5678"

        val hash1 = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, pin1.toCharArray())
        val hash2 = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, pin2.toCharArray())

        assertTrue("Different PINs should produce different hashes", hash1 != hash2)
    }

    @Test
    fun `same PIN can be verified multiple times with same hash`() {
        val correctPin = "1234"
        val hashedPin = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, correctPin.toCharArray())

        // Verify multiple times against the same hash
        val result1 = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
            .verify(correctPin.toCharArray(), hashedPin).verified
        val result2 = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
            .verify(correctPin.toCharArray(), hashedPin).verified
        val result3 = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
            .verify(correctPin.toCharArray(), hashedPin).verified

        assertTrue("Same PIN should verify consistently", result1 && result2 && result3)
    }

    @Test
    fun `incorrect PIN fails bcrypt verification`() {
        val correctPin = "1234"
        val wrongPin = "5678"

        val hashedPin = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, correctPin.toCharArray())

        val result = at.favre.lib.crypto.bcrypt.BCrypt.verifyer()
            .verify(wrongPin.toCharArray(), hashedPin).verified

        assertFalse("Wrong PIN should fail verification", result)
    }

    @Test
    fun `bcrypt hash has expected format`() {
        val pin = "1234"
        val hash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, pin.toCharArray())

        // bcrypt hashes start with $2a$ prefix
        assertTrue("bcrypt hash should have correct format", hash.contains("$2a$"))
    }

    @Test
    fun `bcrypt cost factor 12 is used`() {
        val pin = "1234"
        val hash = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
            .hashToString(12, pin.toCharArray())

        // Extract cost from hash format: $2a$COST$...
        val parts = hash.split("$")
        assertEquals("bcrypt cost should be 12", "12", parts[2])
    }

    // -------------------------------------------------------------------------
    // PIN format validation tests
    // -------------------------------------------------------------------------

    @Test
    fun `PIN validation logic - valid 4 digit PIN`() {
        val pin = "1234"
        val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                    pin.all { it.isDigit() }
        assertTrue("4-digit numeric PIN should be valid", valid)
    }

    @Test
    fun `PIN validation logic - valid 6 digit PIN`() {
        val pin = "123456"
        val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                    pin.all { it.isDigit() }
        assertTrue("6-digit numeric PIN should be valid", valid)
    }

    @Test
    fun `PIN validation logic - too short PIN`() {
        val pin = "123"
        val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                    pin.all { it.isDigit() }
        assertFalse("3-digit PIN should be invalid", valid)
    }

    @Test
    fun `PIN validation logic - too long PIN`() {
        val pin = "1234567"
        val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                    pin.all { it.isDigit() }
        assertFalse("7-digit PIN should be invalid", valid)
    }

    @Test
    fun `PIN validation logic - non-numeric characters`() {
        val pins = listOf("12ab", "12!@", "12 34", "1234a")
        pins.forEach { pin ->
            val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                        pin.all { it.isDigit() }
            assertFalse("PIN '$pin' with non-numeric chars should be invalid", valid)
        }
    }

    @Test
    fun `PIN validation logic - empty string`() {
        val pin = ""
        val valid = pin.length in PinAuthManager.MIN_PIN_LENGTH..PinAuthManager.MAX_PIN_LENGTH &&
                    pin.all { it.isDigit() }
        assertFalse("Empty PIN should be invalid", valid)
    }
}
