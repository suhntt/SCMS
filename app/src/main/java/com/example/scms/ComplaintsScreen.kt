package com.example.scms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintsScreen(navController: NavController) {

    val scope = rememberCoroutineScope()

    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 🔐 SAFE NETWORK CALL
    LaunchedEffect(Unit) {
        try {
            complaints = RetrofitClient.api.getComplaints()
        } catch (e: Exception) {
            e.printStackTrace()
            error = "Unable to load complaints"
        } finally {
            isLoading = false
        }
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
                Text("+")
            }
        }
    ) { padding ->

        // 🔄 LOADING STATE (NO CRASH)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                Text(error!!)
            }
            return@Scaffold
        }

        // ✅ DATA STATE
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(complaints) { complaint ->
                ComplaintCard(
                    complaint = complaint,
                    onUpvote = {
                        scope.launch {
                            try {
                                RetrofitClient.api.upvote(complaint.id.toInt())
                                complaints = RetrofitClient.api.getComplaints()
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
