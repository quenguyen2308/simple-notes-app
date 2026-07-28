package com.yourname.simplenotes.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.yourname.simplenotes.ui.ROUTE_FOLDERS
import com.yourname.simplenotes.ui.ROUTE_HOME
import com.yourname.simplenotes.ui.ROUTE_NOTES
import com.yourname.simplenotes.ui.ROUTE_PERSONAL

private enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(ROUTE_HOME, "Trang chủ", Icons.Filled.Home),
    NOTES(ROUTE_NOTES, "Ghi chú", Icons.AutoMirrored.Filled.Notes),
    FOLDERS(ROUTE_FOLDERS, "Thư mục", Icons.Filled.Folder),
    PERSONAL(ROUTE_PERSONAL, "Cá nhân", Icons.Filled.Person)
}

/** Bottom tab bar for the 4 main app sections. Uses [MaterialTheme.colorScheme] tokens only
 *  (never literal Notez colors) since this shell is shared by every [com.yourname.simplenotes.ui.theme.AppTheme],
 *  not just the NOTEZ one. */
@Composable
fun NotezBottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick  = { onNavigate(tab.route) },
                icon     = { Icon(tab.icon, contentDescription = tab.label) },
                label    = { Text(tab.label) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
