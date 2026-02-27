package com.example.scms

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@Composable fun NavGraph(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        // 🔐 AUTH
        composable("login") {
            LoginScreen(navController)   // ✅ FIXED
        }

        composable("signup") {
            SignupScreen(navController)
        }

        // 🏠 HOME INFO
        composable("home") {
            HomeInfoScreen()
        }

        // 📋 COMPLAINTS
        composable("complaints") {
            ComplaintsScreen(navController)
        }

        // 📷 CAMERA
        composable("camera") {
            CameraScreen(navController)
        }

        // 👤 PROFILE
        composable("profile") {
            ProfileScreen(navController)
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
