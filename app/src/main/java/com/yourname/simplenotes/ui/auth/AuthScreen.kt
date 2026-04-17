package com.yourname.simplenotes.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity

/**
 * Top-level auth screen composable.
 *
 * Delegates all rendering and state management to [BiometricAuthPrompt].
 * Navigation callbacks ([onAuthSuccess], [showPinFallback]) are handled by
 * [AppNavigation] so this screen stays stateless.
 */
@Composable
fun AuthScreen(
    activity: FragmentActivity,
    onAuthSuccess: () -> Unit,
    showPinFallback: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel
) {
    BiometricAuthPrompt(
        activity = activity,
        onAuthSuccess = onAuthSuccess,
        onAuthFailed = showPinFallback,
        viewModel = viewModel,
        modifier = modifier
    )
}
