package com.yourname.simplenotes.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages cryptographic keys for biometric authentication using AndroidKeyStore.
 * Keys are bound to biometric authentication — the cipher can only be initialised
 * after the user successfully authenticates.
 *
 * minSdk = 28 so no @RequiresApi annotation needed; all APIs used are available from API 23+.
 */
class BiometricKeyStore {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    /**
     * Generates (or replaces) the AES-CBC key in AndroidKeyStore.
     * The key is flagged as requiring user authentication, so every cipher
     * initialisation will trigger a biometric challenge.
     */
    fun generateBiometricKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            // Invalidate the key if a new biometric is enrolled — security hardening
            setInvalidatedByBiometricEnrollment(true)
            setUserAuthenticationRequired(true)
            // Do not require confirmation dialog on top of biometric prompt
            setUserConfirmationRequired(false)
        }.build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    /**
     * Returns true if the biometric key already exists in the key store.
     */
    fun hasBiometricKey(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    /**
     * Deletes the stored key, forcing re-generation on next use.
     * Useful when biometric enrollment changes.
     */
    fun deleteBiometricKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /**
     * Returns a Cipher initialised in ENCRYPT_MODE with the stored biometric key.
     * If no key exists yet one is generated automatically.
     *
     * When a new biometric is enrolled the existing key is permanently invalidated.
     * In that case the stale key is deleted, a fresh key is generated, and the
     * cipher initialisation is retried — preventing a crash on subsequent launches.
     *
     * @throws Exception for any keystore or cipher failure that is not recoverable.
     */
    fun getBiometricCipher(): Cipher {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateBiometricKey()
        }

        return try {
            initCipher()
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Biometric re-enrollment invalidated the key — delete and regenerate
            deleteBiometricKey()
            generateBiometricKey()
            initCipher()
        }
    }

    /**
     * Creates and initialises the cipher with the key currently in the store.
     * Separated from [getBiometricCipher] so the retry path stays clean.
     */
    private fun initCipher(): Cipher {
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("Biometric key not found in AndroidKeyStore after generation")

        return Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "biometric_auth_key"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }
}
