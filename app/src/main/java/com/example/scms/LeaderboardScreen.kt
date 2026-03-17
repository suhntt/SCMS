package com.example.scms

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// Colour palette
// ─────────────────────────────────────────────────────────────
private val Gold   = Color(0xFFFFD700)
private val Silver = Color(0xFFC0C0C0)
private val Bronze = Color(0xFFCD7F32)
private val Navy   = Color(0xFF0F1B2D)
private val DeepBlue = Color(0xFF1A2E4A)

// ─────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavController) {

    val scope = rememberCoroutineScope()
    var entries   by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }

    val currentUserId = UserSession.currentUser?.id ?: -1

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                entries = RetrofitClient.api.getLeaderboard()
            } catch (e: Exception) {
                error = "Could not load leaderboard"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Civic Heroes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Navy
    ) { padding ->

        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            }

            error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error!!, color = Color.White)
                }
            }

            entries.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No entries yet. Be the first hero! 🦸", color = Color.White)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

                    // ── PODIUM (top-3) ──────────────────────────────────
                    if (entries.size >= 3) {
                        item {
                            PodiumCard(
                                first  = entries[0],
                                second = entries[1],
                                third  = entries[2],
                                currentUserId = currentUserId
                            )
                        }
                    }

                    // ── REST OF LIST ────────────────────────────────────
                    val restStart = if (entries.size >= 3) 3 else 0
                    itemsIndexed(entries.drop(restStart)) { idx, entry ->
                        LeaderboardRow(
                            rank          = restStart + idx + 1,
                            entry         = entry,
                            isCurrentUser = entry.id == currentUserId
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Podium (top-3 visual)
// ─────────────────────────────────────────────────────────────
@Composable
private fun PodiumCard(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry,
    currentUserId: Int
) {
    // Subtle shimmer animation on the gold medal
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2E4A), Color(0xFF0F1B2D))
                )
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 🥈 Silver – 2nd place
            PodiumPillar(
                entry = second,
                rank = 2,
                medalColor = Silver,
                pillarHeight = 90.dp,
                shimmerAlpha = 1f,
                isCurrentUser = second.id == currentUserId
            )
            // 🥇 Gold – 1st place (tallest)
            PodiumPillar(
                entry = first,
                rank = 1,
                medalColor = Gold,
                pillarHeight = 130.dp,
                shimmerAlpha = shimmerAlpha,
                isCurrentUser = first.id == currentUserId
            )
            // 🥉 Bronze – 3rd place
            PodiumPillar(
                entry = third,
                rank = 3,
                medalColor = Bronze,
                pillarHeight = 65.dp,
                shimmerAlpha = 1f,
                isCurrentUser = third.id == currentUserId
            )
        }
    }
}

@Composable
private fun PodiumPillar(
    entry: LeaderboardEntry,
    rank: Int,
    medalColor: Color,
    pillarHeight: androidx.compose.ui.unit.Dp,
    shimmerAlpha: Float,
    isCurrentUser: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {

        // Name + points above pillar
        Text(
            entry.name.split(" ").first(),
            color = if (isCurrentUser) Gold else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
        Text(
            "${entry.points} pts",
            color = medalColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))

        // Avatar circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, CircleShape)
                .background(medalColor.copy(alpha = shimmerAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy
            )
        }

        Spacer(Modifier.height(6.dp))

        // Pillar block
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            medalColor.copy(alpha = 0.35f),
                            medalColor.copy(alpha = 0.12f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                color = medalColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Regular rank row (4th place and beyond)
// ─────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val cardBg = if (isCurrentUser)
        Brush.horizontalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF16324A)))
    else
        Brush.horizontalGradient(listOf(DeepBlue, DeepBlue))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Rank badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$rank",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entry.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name + stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    color = if (isCurrentUser) Gold else Color.White,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStat(
                        Icons.Filled.Star,
                        "${entry.total_complaints}",
                        Color(0xFF60A5FA)
                    )
                    MiniStat(
                        Icons.Filled.CheckCircle,
                        "${entry.resolved_complaints}",
                        Color(0xFF34D399)
                    )
                    MiniStat(
                        Icons.Filled.ThumbUp,
                        "${entry.total_upvotes}",
                        Color(0xFFFBBF24)
                    )
                }
            }

            // Points
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${entry.points}",
                    color = if (isCurrentUser) Gold else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    "pts",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }

        // "YOU" badge for current user
        if (isCurrentUser) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Gold, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("YOU", color = Navy, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun MiniStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(value, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}
