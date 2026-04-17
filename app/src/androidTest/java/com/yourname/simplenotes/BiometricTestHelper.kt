package com.yourname.simplenotes

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider

/**
 * Utility for instrumented tests: detects whether the test device has a
 * usable biometric sensor so tests can skip gracefully on emulators.
 */
object BiometricTestHelper {

    /** Returns true if the test device has a strong biometric enrolled and ready. */
    fun isTestDeviceBiometricCapable(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
