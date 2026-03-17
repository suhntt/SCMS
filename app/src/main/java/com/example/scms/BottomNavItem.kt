package com.example.scms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home        : BottomNavItem("home",        "Home",        Icons.Filled.Home)
    object Complaints  : BottomNavItem("complaints",  "Complaints",  Icons.Filled.List)
    object Alerts      : BottomNavItem("alerts",      "Alerts",      Icons.Filled.Notifications)
    object Leaderboard : BottomNavItem("leaderboard", "Leaderboard", Icons.Filled.EmojiEvents)
    object Profile     : BottomNavItem("profile",     "Profile",     Icons.Filled.Person)
}
