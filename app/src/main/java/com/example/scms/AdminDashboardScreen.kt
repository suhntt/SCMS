package com.example.scms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {

    val scope = rememberCoroutineScope()
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }

    val departments = listOf(
        "Roads Department",
        "Water Department",
        "Electricity Department",
        "Sanitation Department",
        "Municipality Office"
    )

    LaunchedEffect(Unit) {
        complaints = RetrofitClient.api.getComplaints()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Admin Dashboard") })
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {

            // 🗺️ MAP — SCROLLABLE + BORDERED (NEW, SAFE)
            if (complaints.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        AdminMapView(
                            complaints = complaints,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 📄 COMPLAINT LIST (UNCHANGED)
            items(complaints) { c ->

                var expanded by remember { mutableStateOf(false) }
                var selectedDepartment by remember {
                    mutableStateOf(c.department ?: "Assign Department")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {

                    Column(modifier = Modifier.padding(12.dp)) {

                        c.photo_url?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Complaint Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Address: ${c.address}")
                        Text("Reported: ${formatTo12Hour(c.created_at)}")
                        Text("Category: ${c.category}")
                        Text("Latitude: ${c.latitude}")
                        Text("Longitude: ${c.longitude}")
                        Text("Details: ${c.description}")
                        Text("Upvotes: ${c.upvotes}")
                        Text("Status: ${c.status}")
                        Text("Department: ${c.department ?: "Not Assigned"}")

                        Spacer(modifier = Modifier.height(10.dp))

                        if (c.status == "Pending") {

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedDepartment,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    label = { Text("Assign Department") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    departments.forEach { dept ->
                                        DropdownMenuItem(
                                            text = { Text(dept) },
                                            onClick = {
                                                selectedDepartment = dept
                                                expanded = false

                                                scope.launch {
                                                    RetrofitClient.api.assignDepartment(
                                                        c.id,
                                                        mapOf("department" to dept)
                                                    )
                                                    complaints =
                                                        RetrofitClient.api.getComplaints()
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        RetrofitClient.api.markResolved(c.id)
                                        complaints =
                                            RetrofitClient.api.getComplaints()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}
