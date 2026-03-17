package com.example.scms

import android.Manifest
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SosScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf("IDLE") } // IDLE -> LOCATING -> SENDING -> SUCCESS -> ERROR
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if(granted) { state = "LOCATING" } else { state = "ERROR" }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Start fetching location instantly if permission is granted
    LaunchedEffect(state) {
        if (state == "IDLE") {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (state == "LOCATING") {
            try {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val loc = result.lastLocation ?: return
                        fusedLocationClient.removeLocationUpdates(this) // Stop updates once grabbed
                        
                        state = "SENDING"
                        scope.launch {
                            try {
                                val body = mapOf(
                                    "latitude" to loc.latitude.toString(),
                                    "longitude" to loc.longitude.toString()
                                )
                                val res = RetrofitClient.api.postAccident(body)
                                if (res.isSuccessful) {
                                    state = "SUCCESS"
                                    delay(3000)
                                    navController.popBackStack() // Auto return to home
                                } else {
                                    state = "ERROR"
                                }
                            } catch (e: Exception) {
                                state = "ERROR"
                            }
                        }
                    }
                }
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                state = "ERROR"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFB71C1C)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "SOS",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            when (state) {
                "LOCATING" -> {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Acquiring exact GPS coordinates...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                "SENDING" -> {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Broadcasting Emergency SOS...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                "SUCCESS" -> {
                    Text("SOS DELIVERED", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Emergency responders & nearby users alerted.", color = Color.White, fontSize = 16.sp)
                }
                "ERROR" -> {
                    Text("FAILED TO SEND SOS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please check location permissions or network connection.", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text("Go Back", color = Color.Red)
                    }
                }
            }
        }
    }
}
