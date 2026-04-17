package com.yourname.simplenotes.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Manages PIN authentication for devices without biometric hardware.
 *
 * Stores a bcrypt hash (cost 12) of the PIN inside [EncryptedSharedPreferences]
 * so the hash is encrypted at rest (AES-256-GCM) in addition to being salted by bcrypt.
 *
 * PIN constraints: 4–6 numeric digits.
 */
class PinAuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Returns true when a PIN hash has already been saved. */
    fun isPinSet(): Boolean = preferences.getString(KEY_PIN_HASH, null) != null

    /**
     * Hash and persist a new PIN.
     *
     * @param pin 4–6 digit numeric string supplied by the user.
     * @return true on success, false when the PIN format is invalid or an error occurs.
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        return try {
            // cost 12 — ~250 ms on modern hardware; fast enough for UX, slow for brute-force
            val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, pin.toCharArray())
            preferences.edit().putString(KEY_PIN_HASH, hash).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Compare a user-supplied plain-text PIN against the stored bcrypt hash.
     *
     * On success the failed-attempt counter is reset to zero.
     * On failure the counter is incremented and persisted so brute-force attempts
     * survive app restarts.
     *
     * @return true when the PIN matches, false otherwise (including when no PIN is set).
     */
    fun verifyPin(pin: String): Boolean {
        val hash = preferences.getString(KEY_PIN_HASH, null) ?: return false
        return try {
            val verified = BCrypt.verifyer().verify(pin.toCharArray(), hash).verified
            if (verified) resetFailedAttempts() else incrementFailedAttempts()
            verified
        } catch (e: Exception) {
            incrementFailedAttempts()
            false
        }
    }

    /** Returns the number of consecutive failed PIN attempts persisted across sessions. */
    fun getFailedAttempts(): Int = preferences.getInt(KEY_FAILED_ATTEMPTS, 0)

    /** Returns true when the failed-attempt count has reached [MAX_ATTEMPTS]. */
    fun isLockedOut(): Boolean = getFailedAttempts() >= MAX_ATTEMPTS

    /** Increments and persists the failed-attempt counter (no-op once cap is reached). */
    private fun incrementFailedAttempts() {
        val current = getFailedAttempts()
        if (current < MAX_ATTEMPTS) {
            preferences.edit().putInt(KEY_FAILED_ATTEMPTS, current + 1).apply()
        }
    }

    /** Clears the failed-attempt counter — call after a successful verification. */
    fun resetFailedAttempts() {
        preferences.edit().remove(KEY_FAILED_ATTEMPTS).apply()
    }

    /** Remove the stored PIN hash (used on sign-out or app reset). */
    fun clearPin() {
        preferences.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_FAILED_ATTEMPTS)
            .apply()
    }

    /** 4–6 digits, numeric only. */
    private fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }

    companion object {
        private const val PREFS_FILE_NAME = "pin_preferences"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val BCRYPT_COST = 12
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
        const val MAX_ATTEMPTS = 3
    }
}
