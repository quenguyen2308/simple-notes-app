package com.yourname.simplenotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourname.simplenotes.ui.auth.AuthNavigation
import com.yourname.simplenotes.ui.auth.AuthScreen
import com.yourname.simplenotes.ui.auth.AuthViewModel
import com.yourname.simplenotes.ui.auth.PinEntryScreen
import com.yourname.simplenotes.ui.editor.NoteEditorScreen
import com.yourname.simplenotes.ui.notes.NoteListScreen
import com.yourname.simplenotes.ui.search.SearchScreen

// Route constants for the main graph
private const val ROUTE_LIST = "list"
private const val ROUTE_EDITOR = "editor/{noteId}?categoryId={categoryId}"
private const val ROUTE_EDITOR_PREFIX = "editor"
private const val ROUTE_SEARCH = "search"

/**
 * Root navigation host for the app.
 *
 * When [requiresAuth] is true the graph starts at [AuthNavigation.ROUTE_AUTH]
 * and navigates to the note list only after successful authentication.
 * When false the note list is the immediate start destination.
 *
 * [activity] must be supplied so [AuthScreen] can pass a [FragmentActivity]
 * reference to [BiometricPrompt]. MainActivity extends [AppCompatActivity]
 * which is a [FragmentActivity], satisfying the biometric library requirement.
 */
@Composable
fun AppNavigation(
    activity: FragmentActivity,
    requiresAuth: Boolean = false,
    onThemeChange: (String) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (requiresAuth) AuthNavigation.ROUTE_AUTH else ROUTE_LIST

    // Single AuthViewModel instance scoped to the nav host lifetime
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(context) }

    NavHost(navController = navController, startDestination = startDestination) {

        // --- Auth route (shown first when biometric lock is enabled) ---
        composable(AuthNavigation.ROUTE_AUTH) {
            AuthScreen(
                activity = activity,
                onAuthSuccess = {
                    navController.navigate(ROUTE_LIST) {
                        // Remove auth screen from back stack so Back exits the app
                        popUpTo(AuthNavigation.ROUTE_AUTH) { inclusive = true }
                    }
                },
                showPinFallback = {
                    // Navigate to the PIN entry screen when biometric fails or is unavailable
                    navController.navigate(AuthNavigation.ROUTE_PIN_ENTRY) {
                        popUpTo(AuthNavigation.ROUTE_AUTH) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        // --- PIN fallback route (shown when biometric is unavailable or fails) ---
        composable(AuthNavigation.ROUTE_PIN_ENTRY) {
            PinEntryScreen(
                onPinSuccess = {
                    navController.navigate(ROUTE_LIST) {
                        // Remove PIN screen from back stack so Back exits the app
                        popUpTo(AuthNavigation.ROUTE_PIN_ENTRY) { inclusive = true }
                    }
                }
            )
        }

        // --- Main app routes ---
        composable(ROUTE_LIST) {
            NoteListScreen(
                onNoteClick = { id -> navController.navigate("$ROUTE_EDITOR_PREFIX/$id") },
                onNewNote = { categoryId ->
                    val route = if (categoryId != null)
                        "$ROUTE_EDITOR_PREFIX/new?categoryId=$categoryId"
                    else
                        "$ROUTE_EDITOR_PREFIX/new"
                    navController.navigate(route)
                },
                onSearchClick = { navController.navigate(ROUTE_SEARCH) },
                onThemeChange = onThemeChange
            )
        }

        composable(ROUTE_SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNoteClick = { id ->
                    navController.navigate("$ROUTE_EDITOR_PREFIX/$id") {
                        popUpTo(ROUTE_SEARCH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = ROUTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            NoteEditorScreen(
                noteId = noteId,
                initialCategoryId = categoryId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
