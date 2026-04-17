package com.yourname.simplenotes.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

/**
 * Composable that drives the biometric prompt and renders feedback UI.
 *
 * Triggers [AuthViewModel.authenticateWithBiometric] on first composition.
 * Delegates success/failure events to the caller via [onAuthSuccess] /
 * [onAuthFailed] so navigation logic stays in the parent.
 */
@Composable
fun BiometricAuthPrompt(
    activity: FragmentActivity,
    onAuthSuccess: () -> Unit,
    onAuthFailed: (String) -> Unit,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()

    // Reset any stale state from a previous auth attempt before triggering a new one.
    // This prevents the composable from immediately forwarding a leftover success/error
    // state that was emitted during the last session.
    LaunchedEffect(Unit) {
        viewModel.resetAuthState()
        viewModel.authenticateWithBiometric(activity)
    }

    // Forward terminal states to caller
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthViewModel.AuthState.AuthSuccess -> onAuthSuccess()
            is AuthViewModel.AuthState.AuthError -> onAuthFailed(state.message)
            is AuthViewModel.AuthState.AuthFailed -> onAuthFailed(state.message)
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            when (val state = authState) {
                is AuthViewModel.AuthState.Authenticating -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verifying identity...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                is AuthViewModel.AuthState.AuthError -> {
                    Text(
                        text = "Authentication Error",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetAuthState() }) {
                        Text("Retry")
                    }
                }

                is AuthViewModel.AuthState.AuthFailed -> {
                    Text(
                        text = "Authentication Failed",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetAuthState() }) {
                        Text("Try Again")
                    }
                    TextButton(onClick = { onAuthFailed("User chose PIN fallback") }) {
                        Text("Use PIN Instead")
                    }
                }

                else -> Unit // Idle / ReadyForBiometric / AuthSuccess — no extra UI
            }
        }
    }
}
