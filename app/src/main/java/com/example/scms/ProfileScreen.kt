package com.example.scms

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

private val Gold  = Color(0xFFFFD700)
private val Navy  = Color(0xFF0F1B2D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ─── Live points state (refreshed from API) ───────────────
    var user   by remember { mutableStateOf(UserSession.currentUser) }
    var points by remember { mutableIntStateOf(user?.points ?: 0) }
    var myRank by remember { mutableIntStateOf(0) }
    var myStats by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Animate points number
    val animatedPoints by animateIntAsState(
        targetValue = points,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "points_anim"
    )

    // Load points and rank on first composition
    LaunchedEffect(Unit) {
        scope.launch {
            val uid = user?.id ?: return@launch
            isRefreshing = true
            try {
                // Refresh points
                val resp = RetrofitClient.api.getUserPoints(uid)
                if (resp.isSuccessful) {
                    val newPts = resp.body()?.points ?: points
                    points = newPts
                    // Update session
                    val sessionMgr = SessionManager(context)
                    sessionMgr.updatePoints(newPts)
                    UserSession.currentUser = user?.copy(points = newPts)
                    user = UserSession.currentUser
                }

                // Get rank from leaderboard
                val board = RetrofitClient.api.getLeaderboard()
                val myEntry = board.indexOfFirst { it.id == uid }
                if (myEntry >= 0) {
                    myRank = myEntry + 1
                    myStats = board[myEntry]
                }
            } catch (_: Exception) { /* offline – use cached points */ }
            finally { isRefreshing = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── HERO HEADER ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A5F), Color(0xFF0F3057))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF2563EB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.name?.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: "Citizen",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = user?.phone ?: "",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── POINTS BADGE ──────────────────────────────
                    Box(
                        modifier = Modifier
                            .background(Gold.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            if (isRefreshing) {
                                Text("Loading...", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            } else {
                                Text(
                                    "$animatedPoints pts",
                                    color = Gold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }

                    if (myRank > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🏅 Rank #$myRank on the Leaderboard",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── STATS CARDS (if available) ────────────────────────
            myStats?.let { stats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Reported",
                        value = "${stats.total_complaints}",
                        icon = Icons.Filled.Star,
                        color = Color(0xFF3B82F6)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Resolved",
                        value = "${stats.resolved_complaints}",
                        icon = Icons.Filled.CheckCircle,
                        color = Color(0xFF10B981)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Upvotes",
                        value = "${stats.total_upvotes}",
                        icon = Icons.Filled.ThumbUp,
                        color = Color(0xFFF59E0B)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── HOW POINTS WORK ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("How to earn points", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    PointsRuleRow("Submit a complaint", Icons.Default.Edit, "+10 pts")
                    PointsRuleRow("Your complaint gets a vote", Icons.Default.ThumbUp, "+2 pts")
                    PointsRuleRow("Your complaint is resolved", Icons.Default.CheckCircle, "+20 pts")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── ACCOUNT DETAILS ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileRow("Name", Icons.Default.Person, user?.name ?: "N/A")
                    ProfileRow("Phone", Icons.Default.Phone, user?.phone ?: "N/A")
                    ProfileRow("Role", Icons.Default.Work, "Citizen")
                    ProfileRow("Status", Icons.Default.Star, "Active Civic Reporter")
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── LEADERBOARD BUTTON ────────────────────────────────
            Button(
                onClick = { navController.navigate("leaderboard") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D4ED8)
                )
            ) {
                Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Full Leaderboard", fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            // ── LOGOUT ────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    UserSession.currentUser = null
                    SessionManager(context).clear()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Logout")
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun PointsRuleRow(action: String, icon: androidx.compose.ui.graphics.vector.ImageVector, reward: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(action, fontSize = 13.sp)
        }
        Text(reward, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 13.sp)
    }
}

@Composable
private fun ProfileRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
        Text(value)
    }
}
