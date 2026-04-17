package com.yourname.simplenotes.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists biometric authentication flags in [EncryptedSharedPreferences].
 *
 * All values are encrypted at rest using AES-256-GCM (values) and AES-256-SIV (keys)
 * backed by a MasterKey stored in AndroidKeyStore.
 *
 * Typical usage:
 *   val prefs = AuthPreferencesManager(context)
 *   prefs.isBiometricEnabled = true
 */
class AuthPreferencesManager(context: Context) {

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

    /**
     * Whether the user has opted in to biometric unlock.
     * Defaults to false (disabled) on first install.
     */
    var isBiometricEnabled: Boolean
        get() = preferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    /**
     * Whether authentication is required before accessing notes.
     * Set to true once the user enables biometric or PIN lock.
     */
    var isAuthRequired: Boolean
        get() = preferences.getBoolean(KEY_AUTH_REQUIRED, false)
        set(value) = preferences.edit().putBoolean(KEY_AUTH_REQUIRED, value).apply()

    /**
     * Clears all stored auth flags (e.g. on sign-out or factory reset within app).
     */
    fun clearAll() = preferences.edit().clear().apply()

    companion object {
        private const val PREFS_FILE_NAME = "auth_preferences"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTH_REQUIRED = "auth_required"
    }
}
