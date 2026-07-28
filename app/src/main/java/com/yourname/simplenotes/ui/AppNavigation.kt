package com.yourname.simplenotes.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourname.simplenotes.ui.auth.AuthNavigation
import com.yourname.simplenotes.ui.auth.AuthScreen
import com.yourname.simplenotes.ui.auth.AuthViewModel
import com.yourname.simplenotes.ui.auth.PinEntryScreen
import com.yourname.simplenotes.ui.editor.NoteEditorScreen
import com.yourname.simplenotes.ui.home.HomeScreen
import com.yourname.simplenotes.ui.nav.NotezBottomNavBar
import com.yourname.simplenotes.ui.notes.NoteListScreen
import com.yourname.simplenotes.ui.notes.NoteListViewModel
import com.yourname.simplenotes.ui.search.SearchScreen
import com.yourname.simplenotes.ui.settings.SettingsScreen
import com.yourname.simplenotes.ui.theme.appBackground
import org.koin.androidx.compose.koinViewModel

// Route constants for the main graph. Not private — NotezBottomNavBar reads the 4 tab routes.
const val ROUTE_HOME = "home"
const val ROUTE_NOTES = "notes"
const val ROUTE_FOLDERS = "folders"
const val ROUTE_PERSONAL = "personal"
private const val ROUTE_EDITOR = "editor/{noteId}?categoryId={categoryId}&sharedText={sharedText}&initialMode={initialMode}"
private const val ROUTE_EDITOR_PREFIX = "editor"
private const val ROUTE_SEARCH = "search"

private val BOTTOM_NAV_ROUTES = setOf(ROUTE_HOME, ROUTE_NOTES, ROUTE_FOLDERS, ROUTE_PERSONAL)

/**
 * Root navigation host for the app.
 *
 * When [requiresAuth] is true the graph starts at [AuthNavigation.ROUTE_AUTH]
 * and navigates to the Home tab only after successful authentication.
 * When false the Home tab is the immediate start destination.
 *
 * A persistent bottom [NotezBottomNavBar] wraps the 4 main tab routes (Trang chủ / Ghi chú /
 * Thư mục / Cá nhân); full-screen routes (editor/search/auth/pin entry) get no bottom bar.
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
    onDynamicColorChange: (Boolean) -> Unit = {},
    onAppThemeChange: (String) -> Unit = {},
    /** Text received via another app's share sheet (e.g. Samsung Notes, Easy Note), if any. */
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (requiresAuth) AuthNavigation.ROUTE_AUTH else ROUTE_HOME

    // Single AuthViewModel instance scoped to the nav host lifetime
    val context = LocalContext.current
    val authViewModel = remember { AuthViewModel(context) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != null && currentRoute in BOTTOM_NAV_ROUTES) {
                NotezBottomNavBar(currentRoute = currentRoute, onNavigate = ::navigateToTab)
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(scaffoldPadding)
        ) {

            // --- Auth route (shown first when biometric lock is enabled) ---
            composable(AuthNavigation.ROUTE_AUTH) {
                AuthScreen(
                    activity = activity,
                    onAuthSuccess = {
                        navController.navigate(ROUTE_HOME) {
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
                        navController.navigate(ROUTE_HOME) {
                            // Remove PIN screen from back stack so Back exits the app
                            popUpTo(AuthNavigation.ROUTE_PIN_ENTRY) { inclusive = true }
                        }
                    }
                )
            }

            // --- Trang chủ (Home dashboard) ---
            composable(ROUTE_HOME) {
                // Reached on first launch (no auth) or right after unlocking, so this is the one
                // place both cold- and warm-start shares land — jump straight into a prefilled
                // new note, then clear the pending text so rotation/back-nav doesn't refire it.
                LaunchedEffect(sharedText) {
                    if (!sharedText.isNullOrBlank()) {
                        navController.navigate("$ROUTE_EDITOR_PREFIX/new?sharedText=${Uri.encode(sharedText)}")
                        onSharedTextConsumed()
                    }
                }
                HomeScreen(
                    onNoteClick = { id -> navController.navigate("$ROUTE_EDITOR_PREFIX/$id") },
                    onNewBlankNote = { navController.navigate("$ROUTE_EDITOR_PREFIX/new") },
                    onNewImageNote = { navController.navigate("$ROUTE_EDITOR_PREFIX/new?initialMode=image") },
                    onNewChecklistNote = { navController.navigate("$ROUTE_EDITOR_PREFIX/new?initialMode=checklist") },
                    onSearchClick = { navController.navigate(ROUTE_SEARCH) },
                    onSeeAllPinned = { navigateToTab(ROUTE_NOTES) },
                    onSeeAllRecent = { navigateToTab(ROUTE_NOTES) }
                )
            }

            // --- Ghi chú (full notes list) ---
            composable(ROUTE_NOTES) {
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
                    onThemeChange = onThemeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onAppThemeChange = onAppThemeChange
                )
            }

            // --- Thư mục & Nhãn (placeholder — full redesign is a later phase) ---
            composable(ROUTE_FOLDERS) {
                Column(
                    modifier = Modifier.fillMaxSize().appBackground(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Thư mục & Nhãn — sắp ra mắt",
                        modifier = Modifier.padding(top = 120.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Cá nhân (routes straight at Settings for now — TODO: full redesign +
            // restore Recycle-bin access here in a later phase) ---
            composable(ROUTE_PERSONAL) {
                val noteListViewModel: NoteListViewModel = koinViewModel()
                Column(Modifier.fillMaxSize().appBackground()) {
                    SettingsScreen(
                        onThemeChange = onThemeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onAppThemeChange = onAppThemeChange,
                        onImportNotes = { noteListViewModel.importNotes(it) },
                        onImportArchive = { noteListViewModel.importArchive(it) }
                    )
                }
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
                    navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("sharedText") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("initialMode") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                val sharedTextArg = backStackEntry.arguments?.getString("sharedText")
                val initialModeArg = backStackEntry.arguments?.getString("initialMode")
                NoteEditorScreen(
                    noteId = noteId,
                    initialCategoryId = categoryId,
                    sharedText = sharedTextArg,
                    initialMode = initialModeArg,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
