package com.yourname.simplenotes.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages the biometric authentication flow using [BiometricPrompt].
 *
 * All prompt interaction is wrapped in a suspend function so callers can
 * use structured concurrency without manual callbacks.
 *
 * NOTE: [authenticate] requires a [FragmentActivity]. MainActivity extends
 * ComponentActivity; Phase 4 must cast it (or bridge via a wrapper) before calling.
 */
class BiometricAuthManager(private val context: Context) {

    /**
     * Returns true when the device has at least one enrolled strong or weak
     * biometric sensor that is ready to use.
     */
    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Launches the system biometric prompt and suspends until:
     * - The user authenticates successfully  → [BiometricResult.Success]
     * - The user cancels / timeout / error   → [BiometricResult.Error]
     * - The biometric attempt fails (retry)  → [BiometricResult.Failed]
     *
     * Cancelling the coroutine scope does NOT dismiss the prompt automatically;
     * callers should handle the [BiometricResult.Error] with code
     * [BiometricPrompt.ERROR_USER_CANCELED] or [BiometricPrompt.ERROR_CANCELED].
     *
     * @param activity Must be a [FragmentActivity]. See phase note above.
     */
    suspend fun authenticate(activity: FragmentActivity): BiometricResult =
        suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricResult.Error(errorCode, errString.toString()))
                    }
                }

                override fun onAuthenticationFailed() {
                    // Finger not recognised — prompt stays open, do not resolve the coroutine yet.
                    // BiometricPrompt will call onAuthenticationError if retries are exhausted.
                    if (continuation.isActive) continuation.resume(BiometricResult.Failed)
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Notes")
                .setSubtitle("Authenticate to access your notes")
                // Negative button is required when NOT using device credential fallback
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(
                    Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }

    // ---------------------------------------------------------------------------
    // Result type
    // ---------------------------------------------------------------------------

    sealed class BiometricResult {
        /** Authentication completed successfully. */
        object Success : BiometricResult()

        /**
         * System-level error (lockout, hardware unavailable, user cancelled, etc.).
         * [code] maps to [BiometricPrompt.ERROR_*] constants.
         */
        data class Error(val code: Int, val message: String) : BiometricResult()

        /**
         * The biometric gesture was not recognised; the prompt is still open.
         * Coroutines resume here only when the subclass is used outside the live prompt.
         * Normal prompt retries are handled internally by BiometricPrompt.
         */
        object Failed : BiometricResult()
    }
}
