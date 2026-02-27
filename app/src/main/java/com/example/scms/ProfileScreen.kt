package com.example.scms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val context = LocalContext.current
    val user = UserSession.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F3057),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 👤 AVATAR
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color(0xFF1E88E5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.name ?: "Guest User",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = user?.phone ?: "Phone not available",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileRow("👤 Name", user?.name ?: "N/A")
                    ProfileRow("📱 Phone", user?.phone ?: "N/A")
                    ProfileRow("🏙️ Role", "Citizen")
                    ProfileRow("⭐ Status", "Active User")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* leaderboard later */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🏆 View Leaderboard")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🚪 LOGOUT (FIXED)
            OutlinedButton(
                onClick = {
                    UserSession.currentUser = null
                    SessionManager(context).clear()

                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value)
    }
}
