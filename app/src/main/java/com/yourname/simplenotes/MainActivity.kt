package com.yourname.simplenotes

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
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
import com.yourname.simplenotes.ui.settings.SettingsPrefs
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

    private var lastBackPressTime = 0L

    private var isSignedIn by mutableStateOf(false)
    private var signInError by mutableStateOf<String?>(null)
    /** "system" | "light" | "dark" — triggers recompose on change */
    private var themeMode by mutableStateOf("system")

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            task.getResult(ApiException::class.java)
            isSignedIn = true
            signInError = null
            // Trigger immediate pull from Drive so device 2 sees device 1's data right away
            syncScheduler.triggerImmediateSync()
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

        // Double-back-to-exit: registered first (lowest priority in LIFO chain).
        // NavHost and editor callbacks take precedence when they are active.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(
                        this@MainActivity,
                        "Nhấn Back lần nữa để thoát",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        isSignedIn = authManager.getSignedInAccount() != null
        themeMode = SettingsPrefs(this).themeMode
        val requiresAuth = AuthPreferencesManager(this).isBiometricEnabled

        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "dark"  -> true
                "light" -> false
                else    -> systemDark
            }
            SimpleNotesTheme(darkTheme = darkTheme) {
                if (isSignedIn) {
                    LaunchedEffect(Unit) { syncScheduler.schedulePeriodicSync() }
                    AppNavigation(
                        activity = this@MainActivity,
                        requiresAuth = requiresAuth,
                        onThemeChange = { themeMode = it }
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
