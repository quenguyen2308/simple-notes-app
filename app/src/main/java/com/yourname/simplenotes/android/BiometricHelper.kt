package com.yourname.simplenotes.android

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators

/**
 * Device-level biometric capability detection utility.
 * Wraps BiometricManager checks for easy testability and reuse across the app.
 */
object BiometricHelper {

    /**
     * Returns true if the device has at least one enrolled biometric sensor
     * (strong or weak) that is currently ready to use.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Convenience alias — returns true when at least one biometric is enrolled
     * and the hardware is ready.
     */
    fun hasBiometricEnrolled(context: Context): Boolean = isBiometricAvailable(context)

    /**
     * Returns a human-readable name for the available biometric type.
     * Currently returns "Fingerprint" when any biometric is available; this can
     * be extended with PackageManager feature checks for face/iris if needed.
     */
    fun getBiometricFeatureName(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        val authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Fingerprint"
            else -> "Unknown"
        }
    }
}
