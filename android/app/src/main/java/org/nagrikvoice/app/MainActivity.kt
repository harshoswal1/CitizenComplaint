package org.nagrikvoice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.ui.navigation.BottomNavBar
import org.nagrikvoice.app.ui.navigation.Screen
import org.nagrikvoice.app.ui.screens.auth.LoginScreen
import org.nagrikvoice.app.ui.screens.auth.RegisterScreen
import org.nagrikvoice.app.ui.screens.detail.ComplaintDetailScreen
import org.nagrikvoice.app.ui.screens.explore.ExploreScreen
import org.nagrikvoice.app.ui.screens.home.HomeScreen
import org.nagrikvoice.app.ui.screens.onboarding.OnboardingScreen
import org.nagrikvoice.app.ui.screens.onboarding.PostRegisterOnboardingScreen
import org.nagrikvoice.app.ui.screens.profile.ProfileScreen
import org.nagrikvoice.app.ui.screens.rankings.RankingsScreen
import org.nagrikvoice.app.ui.screens.report.ReportScreen
import org.nagrikvoice.app.ui.theme.NagrikVoiceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NagrikVoiceTheme {
                MainAppNavigation()
            }
        }
    }
}

@Composable
fun MainAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Decide the start destination based on persisted token
    val authRepo = AuthRepository.instance
    val startDestination = when {
        authRepo.savedToken == null -> Screen.Login.route
        !authRepo.isOnboardingDone() -> Screen.PostRegisterOnboarding.route
        else -> Screen.Home.route
    }

    // Show bottom navigation bar only on primary dashboard screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Report.route,
        Screen.Rankings.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        if (!authRepo.isOnboardingDone()) {
                            navController.navigate(Screen.PostRegisterOnboarding.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.PostRegisterOnboarding.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }

            composable(Screen.PostRegisterOnboarding.route) {
                PostRegisterOnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PostRegisterOnboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { complaintId ->
                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                    },
                    onNavigateToReport = {
                        navController.navigate(Screen.Report.route)
                    },
                    onNavigateToExplore = {
                        navController.navigate(Screen.Explore.route)
                    },
                    onNavigateToRankings = {
                        navController.navigate(Screen.Rankings.route)
                    }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    onNavigateToDetail = { complaintId ->
                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                    }
                )
            }

            composable(Screen.Report.route) {
                ReportScreen(
                    onNavigateToFeed = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToDetail = { complaintId ->
                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                    }
                )
            }

            composable(Screen.Rankings.route) {
                RankingsScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToDetail = { complaintId ->
                        navController.navigate(Screen.ComplaintDetail.createRoute(complaintId))
                    }
                )
            }

            composable(
                route = Screen.ComplaintDetail.route,
                arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
            ) { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                ComplaintDetailScreen(
                    complaintId = complaintId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
