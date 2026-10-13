package org.nagrikvoice.app.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.nagrikvoice.app.ui.theme.CivicNavy
import org.nagrikvoice.app.ui.theme.CivicSaffron

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navScreens = listOf(
        Screen.Home,
        Screen.Explore,
        Screen.Report,
        Screen.Rankings,
        Screen.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        navScreens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val isReport = screen == Screen.Report

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    val icon = if (isSelected) screen.selectedIcon else screen.unselectedIcon
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = screen.title,
                            modifier = if (isReport) Modifier.size(28.dp) else Modifier.size(24.dp),
                            tint = if (isReport) CivicSaffron else if (isSelected) CivicNavy else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.title ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) CivicNavy else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (isReport) CivicSaffron.copy(alpha = 0.2f) else CivicNavy.copy(alpha = 0.1f)
                )
            )
        }
    }
}
