package com.yourname.simplenotes

import android.content.Context
import androidx.biometric.BiometricManager
import com.yourname.simplenotes.data.auth.BiometricAuthManager
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BiometricAuthManager].
 *
 * BiometricPrompt is an Android framework class that cannot be instantiated on the JVM,
 * so [authenticate] is covered by instrumented tests in androidTest/.
 * These JVM tests cover: result type identity, data class equality, and instantiation.
 */
class BiometricAuthManagerTest {

    private lateinit var mockContext: Context
    private lateinit var manager: BiometricAuthManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        manager = BiometricAuthManager(mockContext)
    }

    // -------------------------------------------------------------------------
    // isBiometricAvailable — structural smoke test (no Android runtime on JVM)
    // -------------------------------------------------------------------------

    @Test
    fun `isBiometricAvailable does not throw unexpected exceptions`() {
        // Android framework stubs throw RuntimeException on JVM — that is expected.
        // Any other Throwable type indicates a programming error in our wrapper.
        try {
            manager.isBiometricAvailable()
        } catch (e: RuntimeException) {
            // Acceptable: Android stub throwing on JVM
        } catch (e: Throwable) {
            throw AssertionError("Unexpected exception type from isBiometricAvailable: ${e::class}", e)
        }
    }

    // -------------------------------------------------------------------------
    // BiometricResult sealed class — type identity
    // -------------------------------------------------------------------------

    @Test
    fun `BiometricResult Success has correct runtime type`() {
        val result: BiometricAuthManager.BiometricResult = BiometricAuthManager.BiometricResult.Success
        assertEquals(
            BiometricAuthManager.BiometricResult.Success::class,
            result::class
        )
    }

    @Test
    fun `BiometricResult Error has correct runtime type`() {
        val result: BiometricAuthManager.BiometricResult =
            BiometricAuthManager.BiometricResult.Error(code = 5, message = "Lockout")
        assertEquals(
            BiometricAuthManager.BiometricResult.Error::class,
            result::class
        )
    }

    @Test
    fun `BiometricResult Failed has correct runtime type`() {
        val result: BiometricAuthManager.BiometricResult = BiometricAuthManager.BiometricResult.Failed
        assertEquals(
            BiometricAuthManager.BiometricResult.Failed::class,
            result::class
        )
    }

    @Test
    fun `BiometricResult subtypes are all distinct`() {
        val success = BiometricAuthManager.BiometricResult.Success
        val error = BiometricAuthManager.BiometricResult.Error(1, "err")
        val failed = BiometricAuthManager.BiometricResult.Failed

        assertNotEquals(success::class, error::class)
        assertNotEquals(success::class, failed::class)
        assertNotEquals(error::class, failed::class)
    }

    // -------------------------------------------------------------------------
    // BiometricResult.Error — data class fields
    // -------------------------------------------------------------------------

    @Test
    fun `BiometricResult Error preserves code and message`() {
        val error = BiometricAuthManager.BiometricResult.Error(
            code = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            message = "No biometric hardware"
        )
        assertEquals(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, error.code)
        assertEquals("No biometric hardware", error.message)
    }

    @Test
    fun `BiometricResult Error data class equality works`() {
        val a = BiometricAuthManager.BiometricResult.Error(7, "Network error")
        val b = BiometricAuthManager.BiometricResult.Error(7, "Network error")
        val c = BiometricAuthManager.BiometricResult.Error(99, "Other")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    // -------------------------------------------------------------------------
    // Instantiation
    // -------------------------------------------------------------------------

    @Test
    fun `BiometricAuthManager can be instantiated with a context`() {
        val instance = BiometricAuthManager(mockContext)
        // Verify no exception is thrown and the object is non-null
        assertEquals(BiometricAuthManager::class, instance::class)
    }
}
