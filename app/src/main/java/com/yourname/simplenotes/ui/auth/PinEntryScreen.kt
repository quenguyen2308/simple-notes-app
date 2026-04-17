package com.yourname.simplenotes.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full-screen PIN entry / PIN setup composable.
 *
 * Handles three logical phases driven by [PinEntryViewModel.state]:
 *   - [PinEntryViewModel.PinEntryState.SetPin]     — create a new PIN
 *   - [PinEntryViewModel.PinEntryState.ConfirmPin] — confirm the new PIN
 *   - [PinEntryViewModel.PinEntryState.VerifyPin]  — unlock with existing PIN
 *
 * Calls [onPinSuccess] once the state reaches [PinEntryViewModel.PinEntryState.PinSuccess].
 */
@Composable
fun PinEntryScreen(
    onPinSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { PinEntryViewModel(context) }

    val state by viewModel.state.collectAsState()
    val pinInput by viewModel.pinInput.collectAsState()
    val pinConfirm by viewModel.pinConfirm.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val failedAttempts by viewModel.failedAttempts.collectAsState()

    // Navigate away as soon as success is signalled
    LaunchedEffect(state) {
        if (state == PinEntryViewModel.PinEntryState.PinSuccess) onPinSuccess()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---- Header ----
            PinHeader(state = state, failedAttempts = failedAttempts)

            // ---- PIN dot indicators ----
            val displayPin = if (state == PinEntryViewModel.PinEntryState.ConfirmPin) pinConfirm else pinInput
            PinDots(filled = displayPin.length)

            // ---- Error message ----
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ---- Numeric keypad ----
            if (state != PinEntryViewModel.PinEntryState.TooManyAttempts) {
                PinKeypad(
                    onDigit = viewModel::onDigit,
                    onBackspace = viewModel::onBackspace,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Too many incorrect attempts.\nPlease try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header — title + subtitle change per state
// ---------------------------------------------------------------------------

@Composable
private fun PinHeader(
    state: PinEntryViewModel.PinEntryState,
    failedAttempts: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when (state) {
                PinEntryViewModel.PinEntryState.SetPin -> "Create PIN"
                PinEntryViewModel.PinEntryState.ConfirmPin -> "Confirm PIN"
                PinEntryViewModel.PinEntryState.TooManyAttempts -> "Locked Out"
                else -> "Enter PIN"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state) {
                PinEntryViewModel.PinEntryState.SetPin -> "Choose a 4–6 digit PIN"
                PinEntryViewModel.PinEntryState.ConfirmPin -> "Re-enter your PIN to confirm"
                PinEntryViewModel.PinEntryState.TooManyAttempts -> "Account locked after $failedAttempts failed attempts"
                else -> "Enter your PIN to unlock"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// PIN dot row — 6 slots; filled circles for entered digits
// ---------------------------------------------------------------------------

@Composable
private fun PinDots(filled: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(6) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Numeric keypad — 3×3 grid + bottom row (0 + backspace)
// ---------------------------------------------------------------------------

@Composable
private fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rows 1-3: digits 1-9
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (col in 0..2) {
                    val digit = (row * 3 + col + 1).toString()
                    PinDigitButton(
                        label = digit,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bottom row: [empty spacer] [0] [backspace]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left placeholder to keep "0" centred
            Spacer(modifier = Modifier.weight(1f))

            PinDigitButton(
                label = "0",
                onClick = { onDigit("0") },
                modifier = Modifier.weight(1f)
            )

            // Backspace button
            FilledTonalButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete last digit"
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Individual digit key
// ---------------------------------------------------------------------------

@Composable
private fun PinDigitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
