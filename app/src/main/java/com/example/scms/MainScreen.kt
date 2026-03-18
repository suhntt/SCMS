package com.example.scms

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.compose.*

@Composable
fun MainScreen() {

    val navController = rememberNavController()

    // ✅ Bottom bar visible ONLY after login
    val bottomBarScreens = listOf(
        "home",
        "complaints?id={id}",
        "alerts",
        "leaderboard",
        "profile"
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomBarScreens.contains(currentRoute)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController)
            }
        }
    ) { paddingValues ->

        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {

            // 🔐 LOGIN
            composable("login") {
                LoginScreen(navController)
            }

            // 🆕 SIGNUP
            composable("signup") {
                SignupScreen(navController)
            }

            // 🏠 HOME INFO
            composable("home") {
                HomeInfoScreen(navController)
            }

            // 🚨 SOS
            composable("sos") {
                SosScreen(navController)
            }

            // 📋 COMPLAINTS
            composable("complaints?id={id}") { backStackEntry ->
                val filteredId = backStackEntry.arguments?.getString("id")
                ComplaintsScreen(navController, filteredId)
            }

            // 🔔 ALERTS
            composable("alerts") {
                AlertsScreen(navController)
            }

            // 📷 CAMERA
            composable("camera") {
                CameraScreen(navController)
            }

            // 👤 PROFILE
            composable("profile") {
                ProfileScreen(navController)
            }

            // ➕ REPORT
            composable("report") {
                ReportComplaintScreen(navController)
            }

            // 🏆 LEADERBOARD
            composable("leaderboard") {
                LeaderboardScreen(navController)
            }

            // 👮 ADMIN
            composable("admin_login") {
                AdminLoginScreen(navController)
            }

            composable("admin_dashboard") {
                AdminDashboardScreen(navController)
            }
        }
    }
}
