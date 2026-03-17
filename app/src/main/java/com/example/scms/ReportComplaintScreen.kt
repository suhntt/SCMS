package com.example.scms

import android.Manifest
import android.location.Geocoder
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportComplaintScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()   // ✅ FIX 1

    /* ---------------- STATE ---------------- */

    var photoUri by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf<String?>(null) }
    var longitude by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("Fetching location…") }

    var description by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("Select Category") }

    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf(
        "Road Damage",
        "Illegal Dumping",
        "Water Leakage",
        "Streetlight Issue",
        "Park Problem",
        "Other"
    )

    /* ---------------- PERMISSION ---------------- */

    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasLocationPermission = granted
        }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /* ---------------- LOCATION ---------------- */

    val fusedLocationClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                latitude = loc.latitude.toString()
                longitude = loc.longitude.toString()

                try {
                    val geo = Geocoder(context, Locale.getDefault())
                    val list = geo.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!list.isNullOrEmpty()) {
                        address = list[0].getAddressLine(0)
                    }
                } catch (_: Exception) {
                    address = "Address unavailable"
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        onDispose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /* ---------------- CAMERA RESULT ---------------- */

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("photoUri")
            ?.observeForever {
                photoUri = it
            }
    }

    /* 🔽 AUTO-SCROLL AFTER PHOTO */
    LaunchedEffect(photoUri) {
        if (photoUri != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val currentTime = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date())
    }

    /* ---------------- UI ---------------- */

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Report New Complaint", fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)   // ✅ FIX 2
                    .padding(padding)
                    .padding(16.dp)
            ) {

                Text("SCMS – Citizen Complaint Portal", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))

                AssistChip(
                    onClick = {},
                    label = { Text("Live Location Verified") },
                    leadingIcon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF2E7D32),
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    )
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(currentTime, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(address, fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))

                if (latitude != null && longitude != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Location Coordinates", fontWeight = FontWeight.Medium)
                            }
                            Text("Latitude: $latitude")
                            Text("Longitude: $longitude")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Complaint Category", fontWeight = FontWeight.Medium)

                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        }
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        categories.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    selectedCategory = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Complaint Description")
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Describe the issue clearly (min 10 characters)") }
                )

                Text("${description.length} / 300",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(Modifier.height(16.dp))

                photoUri?.let {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("camera") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (photoUri == null) "Live Photo" else "Retake")
                    }
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (photoUri == null) "Gallery" else "Replace")
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSubmitting = true

                                val photoPart = photoUri?.let {
                                    val uri = Uri.parse(it)
                                    val bytes = context.contentResolver
                                        .openInputStream(uri)?.readBytes() ?: return@launch

                                    MultipartBody.Part.createFormData(
                                        "photo",
                                        "complaint.jpg",
                                        bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                    )
                                }

                                val uid = UserSession.currentUser?.id?.toString() ?: ""

                                if (NetworkUtils.isInternetAvailable(context)) {
                                    // 🟢 ONLINE: Send Immediately
                                    RetrofitClient.api.submitComplaint(
                                        photo = photoPart,
                                        category = selectedCategory.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        address = address.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        latitude = (latitude ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                                        longitude = (longitude ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                                        description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                                        userId = uid.toRequestBody("text/plain".toMediaTypeOrNull())
                                    )
                                } else {
                                    // 🔴 OFFLINE: Queue Locally
                                    val offlineComplaint = OfflineComplaint(
                                        photoUri = photoUri,
                                        category = selectedCategory,
                                        address = address,
                                        latitude = latitude ?: "",
                                        longitude = longitude ?: "",
                                        description = description,
                                        userId = uid
                                    )
                                    OfflineComplaintManager.saveComplaintOffline(context, offlineComplaint)
                                    // Toast.makeText(context, "Offline detected. Complaint saved and will auto-sync.", Toast.LENGTH_LONG).show()
                                }

                                navController.popBackStack()

                            } catch (e: Exception) {
                                // If Retrofit fails (e.g., timeout/flaky network), queue it
                                val uid = UserSession.currentUser?.id?.toString() ?: ""
                                val offlineComplaint = OfflineComplaint(
                                    photoUri = photoUri,
                                    category = selectedCategory,
                                    address = address,
                                    latitude = latitude ?: "",
                                    longitude = longitude ?: "",
                                    description = description,
                                    userId = uid
                                )
                                OfflineComplaintManager.saveComplaintOffline(context, offlineComplaint)
                                navController.popBackStack()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = description.length >= 10 &&
                            selectedCategory != "Select Category" &&
                            photoUri != null &&
                            latitude != null &&
                            longitude != null &&
                            !isSubmitting
                ) {
                    Text("🚀 Submit Complaint")
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "All complaints are verified using live GPS & photo",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Submitting complaint…")
                        }
                    }
                }
            }
        }
    }
}
