package com.yourname.simplenotes.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.simplenotes.data.auth.PinAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages state for the PIN entry/setup flow.
 *
 * State machine transitions:
 *   First launch (no PIN stored):  SetPin → ConfirmPin → PinSuccess
 *   Subsequent launches:           VerifyPin → PinSuccess | TooManyAttempts
 *
 * PIN input is capped at [PinAuthManager.MAX_PIN_LENGTH] digits.
 * Auto-submits once the display length reaches [AUTO_SUBMIT_LENGTH].
 */
class PinEntryViewModel(context: Context) : ViewModel() {

    private val pinManager = PinAuthManager(context)

    private val _state = MutableStateFlow<PinEntryState>(
        if (pinManager.isPinSet()) PinEntryState.VerifyPin else PinEntryState.SetPin
    )
    val state: StateFlow<PinEntryState> = _state

    // Active digit buffer — used for SetPin / VerifyPin
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput

    // Second buffer used only during ConfirmPin step
    private val _pinConfirm = MutableStateFlow("")
    val pinConfirm: StateFlow<String> = _pinConfirm

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Seed from persisted storage so attempt count survives app restarts
    private val _failedAttempts = MutableStateFlow(pinManager.getFailedAttempts())
    val failedAttempts: StateFlow<Int> = _failedAttempts

    // -------------------------------------------------------------------------
    // Keypad actions
    // -------------------------------------------------------------------------

    /** Append a single digit to the active buffer; auto-submits at max length. */
    fun onDigit(digit: String) {
        val current = activeBuffer()
        if (current.length >= AUTO_SUBMIT_LENGTH) return

        val updated = current + digit
        setActiveBuffer(updated)

        if (updated.length == AUTO_SUBMIT_LENGTH) {
            submit()
        }
    }

    /** Remove the last character from the active buffer. */
    fun onBackspace() {
        val current = activeBuffer()
        if (current.isNotEmpty()) setActiveBuffer(current.dropLast(1))
    }

    /** Explicit submit (e.g. a "Done" button for 4-digit PINs that want to confirm early). */
    fun submit() {
        viewModelScope.launch {
            when (_state.value) {
                PinEntryState.SetPin -> handleSetPin()
                PinEntryState.ConfirmPin -> handleConfirmPin()
                PinEntryState.VerifyPin -> handleVerifyPin()
                else -> Unit
            }
        }
    }

    // -------------------------------------------------------------------------
    // State transitions
    // -------------------------------------------------------------------------

    private suspend fun handleSetPin() {
        val pin = _pinInput.value
        if (pin.length < PinAuthManager.MIN_PIN_LENGTH) {
            _errorMessage.value = "PIN must be at least ${PinAuthManager.MIN_PIN_LENGTH} digits"
            return
        }
        // Move to confirm step — keep original PIN in _pinInput for comparison
        _errorMessage.value = null
        _pinConfirm.value = ""
        _state.value = PinEntryState.ConfirmPin
    }

    private suspend fun handleConfirmPin() {
        val original = _pinInput.value
        val confirm = _pinConfirm.value

        if (original != confirm) {
            _errorMessage.value = "PINs do not match — please try again"
            // Reset both buffers; return user to SetPin step
            _pinInput.value = ""
            _pinConfirm.value = ""
            _state.value = PinEntryState.SetPin
            return
        }

        // bcrypt is CPU-intensive — run off the main thread
        val saved = withContext(Dispatchers.Default) { pinManager.setPin(original) }
        if (saved) {
            _state.value = PinEntryState.PinSuccess
        } else {
            _errorMessage.value = "Failed to save PIN — please try again"
            _pinInput.value = ""
            _pinConfirm.value = ""
            _state.value = PinEntryState.SetPin
        }
    }

    private suspend fun handleVerifyPin() {
        val pin = _pinInput.value

        // Guard: reject immediately if already locked out (persisted across restarts)
        if (pinManager.isLockedOut()) {
            _state.value = PinEntryState.TooManyAttempts
            return
        }

        // bcrypt verification is intentionally slow — run off the main thread.
        // verifyPin() persists the attempt counter internally (increment on failure,
        // reset on success), so no manual counter management is needed here.
        val verified = withContext(Dispatchers.Default) { pinManager.verifyPin(pin) }

        // Sync the in-memory flow with the value now stored in EncryptedSharedPreferences
        _failedAttempts.value = pinManager.getFailedAttempts()
        _pinInput.value = ""

        if (verified) {
            _state.value = PinEntryState.PinSuccess
            return
        }

        val attempts = _failedAttempts.value
        if (attempts >= PinAuthManager.MAX_ATTEMPTS) {
            _state.value = PinEntryState.TooManyAttempts
        } else {
            val remaining = PinAuthManager.MAX_ATTEMPTS - attempts
            _errorMessage.value = "Incorrect PIN — $remaining attempt${if (remaining == 1) "" else "s"} remaining"
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun activeBuffer(): String = when (_state.value) {
        PinEntryState.ConfirmPin -> _pinConfirm.value
        else -> _pinInput.value
    }

    private fun setActiveBuffer(value: String) {
        when (_state.value) {
            PinEntryState.ConfirmPin -> _pinConfirm.value = value
            else -> _pinInput.value = value
        }
    }

    // -------------------------------------------------------------------------
    // State sealed class
    // -------------------------------------------------------------------------

    sealed class PinEntryState {
        /** User is creating a new PIN (first entry). */
        object SetPin : PinEntryState()

        /** User is re-entering PIN to confirm it matches. */
        object ConfirmPin : PinEntryState()

        /** User is unlocking — PIN already stored. */
        object VerifyPin : PinEntryState()

        /** PIN accepted. */
        object PinSuccess : PinEntryState()

        /** Exceeded [PinAuthManager.MAX_ATTEMPTS] wrong guesses. */
        object TooManyAttempts : PinEntryState()
    }

    companion object {
        // Auto-submit when the buffer reaches max length
        private const val AUTO_SUBMIT_LENGTH = PinAuthManager.MAX_PIN_LENGTH
    }
}
