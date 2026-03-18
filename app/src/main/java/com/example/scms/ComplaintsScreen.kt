package com.example.scms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(navController: NavController, filteredId: String? = null) {

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ FIX: Reload complaints every time user RETURNS to this screen
    suspend fun loadComplaints() {
        isLoading = true
        error = null
        try {
            val fetched = RetrofitClient.api.getComplaints()
            complaints = if (filteredId != null) {
                fetched.filter { it.id.toString() == filteredId }
            } else {
                fetched
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = "Unable to load complaints. Is the server running?"
        } finally {
            isLoading = false
        }
    }

    // First load
    LaunchedEffect(Unit) {
        loadComplaints()
    }

    // Reload every time screen is RESUMED (e.g. navigating back from Report screen)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadComplaints() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaints") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("report") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Report Complaint")
            }
        }
    ) { padding ->

        // 🔄 LOADING STATE
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading complaints…", style = MaterialTheme.typography.bodySmall)
                }
            }
            return@Scaffold
        }

        // ❌ ERROR STATE
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { loadComplaints() } }) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        // ✅ EMPTY STATE
        if (complaints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No complaints yet. Be the first to report!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        // ✅ DATA STATE — list with stale key to avoid recomposition glitches
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(complaints, key = { it.id }) { complaint ->
                ComplaintCard(
                    complaint = complaint,
                    onUpvote = {
                        scope.launch {
                            try {
                                val userId = UserSession.currentUser?.id ?: return@launch
                                RetrofitClient.api.upvote(complaint.id.toInt(), mapOf("user_id" to userId))
                                loadComplaints() // Refresh after upvote too
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            }
        }
    }
}
