package com.yourname.simplenotes.ui.auth

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.simplenotes.data.auth.AuthPreferencesManager
import com.yourname.simplenotes.data.auth.BiometricAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for biometric authentication flow.
 * Manages [AuthState] transitions and delegates to [BiometricAuthManager].
 */
class AuthViewModel(private val context: Context) : ViewModel() {

    private val biometricManager = BiometricAuthManager(context)
    private val preferencesManager = AuthPreferencesManager(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        if (preferencesManager.isBiometricEnabled && biometricManager.isBiometricAvailable()) {
            _authState.value = AuthState.ReadyForBiometric
        }
    }

    /** Launches the biometric prompt and updates state based on result. */
    fun authenticateWithBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating

            val result = biometricManager.authenticate(activity)
            _authState.value = when (result) {
                BiometricAuthManager.BiometricResult.Success -> AuthState.AuthSuccess
                is BiometricAuthManager.BiometricResult.Error -> {
                    AuthState.AuthError(result.message)
                }
                BiometricAuthManager.BiometricResult.Failed -> {
                    AuthState.AuthFailed("Authentication failed, please try again")
                }
            }
        }
    }

    /** Resets state back to [AuthState.Idle] so the user can retry. */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    sealed class AuthState {
        object Idle : AuthState()
        object ReadyForBiometric : AuthState()
        object Authenticating : AuthState()
        object AuthSuccess : AuthState()
        data class AuthError(val message: String) : AuthState()
        data class AuthFailed(val message: String) : AuthState()
    }
}
