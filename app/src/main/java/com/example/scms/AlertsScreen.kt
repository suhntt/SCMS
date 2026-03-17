package com.example.scms

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Data class for an alert ─────────────────────────────────────
data class Alert(
    val alertId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info",          // "warning" | "danger" | "info"
    val area: String = "",
    val created_at: String? = null
)

private val DarkBg   = Color(0xFF0F1B2D)
private val CardBg   = Color(0xFF1A2E4A)
private val GoldCol  = Color(0xFFFFD700)
private val RedAlert = Color(0xFFEF4444)
private val OrangeW  = Color(0xFFF97316)
private val BlueInfo = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {

    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastRefreshed by remember { mutableStateOf("") }

    // Pulse animation for the live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    fun loadAlerts() {
        scope.launch {
            try {
                val result = RetrofitClient.api.getAlerts()
                alerts = result
                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
                lastRefreshed = "Updated at $now"
                error = null
            } catch (e: Exception) {
                error = "Could not load alerts. Check your connection."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAlerts()
        // Auto-refresh every 30 seconds
        while (true) {
            delay(30_000)
            loadAlerts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = GoldCol,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Alerts & Incidents", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    // Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color(0xFF22C55E).copy(alpha = pulseAlpha),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Live", fontSize = 11.sp, color = Color(0xFF22C55E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBg
    ) { padding ->

        when {
            isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldCol)
                        Spacer(Modifier.height(12.dp))
                        Text("Fetching live alerts...", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, null, tint = OrangeW, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(error!!, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isLoading = true; loadAlerts() },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueInfo)
                        ) { Text("Retry") }
                    }
                }
            }

            alerts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No active alerts in your area",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Stay safe! We'll notify you when something comes up.",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                        if (lastRefreshed.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(lastRefreshed, color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary banner
                    item {
                        AlertSummaryBanner(alerts = alerts, lastRefreshed = lastRefreshed)
                        Spacer(Modifier.height(4.dp))
                    }

                    items(alerts) { alert ->
                        AlertCard(alert = alert)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertSummaryBanner(alerts: List<Alert>, lastRefreshed: String) {
    val danger  = alerts.count { it.type == "danger" }
    val warning = alerts.count { it.type == "warning" }
    val info    = alerts.count { it.type == "info" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E3A5F), Color(0xFF162E4A))
                )
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                "${alerts.size} Active Alert${if (alerts.size != 1) "s" else ""}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (danger > 0) SummaryChip("🔴 $danger Danger", RedAlert)
                if (warning > 0) SummaryChip("🟠 $warning Warning", OrangeW)
                if (info > 0) SummaryChip("🔵 $info Info", BlueInfo)
            }
            if (lastRefreshed.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(lastRefreshed, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AlertCard(alert: Alert) {
    val (accentColor, iconVec, bgGrad) = when (alert.type) {
        "danger"  -> Triple(
            RedAlert,
            Icons.Filled.Warning,
            listOf(Color(0xFF3B0F0F), Color(0xFF1A0A0A))
        )
        "warning" -> Triple(
            OrangeW,
            Icons.Filled.Warning,
            listOf(Color(0xFF3B1F0A), Color(0xFF1A0F05))
        )
        else -> Triple(
            BlueInfo,
            Icons.Filled.Info,
            listOf(Color(0xFF0F2240), Color(0xFF0A1A30))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(bgGrad))
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(accentColor.copy(alpha = 0.8f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVec, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = alert.title.ifEmpty { "Alert" },
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Message
                if (alert.message.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = alert.message,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Area + time
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (alert.area.isNotEmpty()) {
                        Text(
                            "Area: ${alert.area}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    alert.created_at?.let { ts ->
                        val formatted = try {
                            formatTo12Hour(ts)
                        } catch (_: Exception) { ts }
                        Text(
                            "Time: $formatted",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Type badge
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = alert.type.replaceFirstChar { it.uppercase() },
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
