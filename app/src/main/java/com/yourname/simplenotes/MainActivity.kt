package com.yourname.simplenotes

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.yourname.simplenotes.data.auth.AuthPreferencesManager
import com.yourname.simplenotes.data.remote.DriveAuthManager
import com.yourname.simplenotes.sync.SyncScheduler
import com.yourname.simplenotes.ui.AppNavigation
import com.yourname.simplenotes.ui.auth.SignInScreen
import com.yourname.simplenotes.ui.theme.SimpleNotesTheme
import org.koin.android.ext.android.inject

/**
 * Single-activity host for the app.
 *
 * Extends [AppCompatActivity] (a [FragmentActivity] subclass) so that
 * [BiometricPrompt] can attach to this activity — a requirement of the
 * androidx.biometric library.
 */
class MainActivity : AppCompatActivity() {

    private val authManager: DriveAuthManager by inject()
    private val syncScheduler: SyncScheduler by inject()

    // Drive sign-in; recompose when sign-in state changes
    private var isSignedIn by mutableStateOf(false)
    private var signInError by mutableStateOf<String?>(null)

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(ApiException::class.java)
            isSignedIn = true
            signInError = null
        } catch (e: ApiException) {
            // Status code 10 = DEVELOPER_ERROR: SHA-1 fingerprint not registered in Google Cloud Console
            Log.e("GoogleSignIn", "Sign-in failed. Status code: ${e.statusCode}", e)
            signInError = when (e.statusCode) {
                10 -> "Sign-in config error (code 10). The app's signing certificate SHA-1 is not registered in Google Cloud Console."
                7 -> "Network error. Check your internet connection."
                else -> "Sign-in failed (code ${e.statusCode}). Please try again."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For testing/debug: Skip Google Sign-in and go directly to app
        isSignedIn = true // authManager.getSignedInAccount() != null

        // Check whether the user has enabled biometric lock before entering composition
        val requiresAuth = AuthPreferencesManager(this).isBiometricEnabled

        setContent {
            SimpleNotesTheme {
                if (isSignedIn) {
                    syncScheduler.schedulePeriodicSync()
                    // `this` is AppCompatActivity → FragmentActivity; safe to pass directly
                    AppNavigation(
                        activity = this@MainActivity,
                        requiresAuth = requiresAuth
                    )
                } else {
                    SignInScreen(
                        onSignIn = { signInLauncher.launch(authManager.client.signInIntent) },
                        errorMessage = signInError
                    )
                }
            }
        }
    }
}
