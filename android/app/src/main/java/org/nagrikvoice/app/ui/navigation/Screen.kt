package org.nagrikvoice.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String? = null,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")

    object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Explore : Screen(
        route = "explore",
        title = "Explore",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    object Report : Screen(
        route = "report",
        title = "Report",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircle
    )

    object Rankings : Screen(
        route = "rankings",
        title = "Rankings",
        selectedIcon = Icons.Filled.Leaderboard,
        unselectedIcon = Icons.Outlined.Leaderboard
    )

    object Profile : Screen(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object ComplaintDetail : Screen("complaint_detail/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_detail/$complaintId"
    }

    companion object {
        val bottomNavScreens: List<Screen>
            get() = listOf(Home, Explore, Report, Rankings, Profile)
    }
}
