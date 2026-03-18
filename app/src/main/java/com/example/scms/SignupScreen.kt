package com.example.scms

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {

    // 🔹 Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 🔹 UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var triggerShake by remember { mutableStateOf(false) }

    // 🔹 Firebase
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // 🔹 Fast & Smooth Iterative Animation
    val entryAnim = remember { Animatable(0f) }
    val shakeAnim = remember { Animatable(0f) }
    
    val formAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entryAnim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(50)
        formAlpha.animateTo(1f, tween(500))
    }

    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(1f, tween(400))
            triggerShake = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌌 Gradient Deep Background
        Image(
            painter = painterResource(R.drawable.auth_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🌑 Deep Modern Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color(0xFF0F172A).copy(alpha = 0.85f),
                            Color(0xFF020617).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = entryAnim.value
                    translationY = (1 - entryAnim.value) * 150 +
                            if (shakeAnim.value > 0f) (-15..15).random() else 0
                },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // -- Header --
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))), CircleShape)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Create Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Join the SCMS community today",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(Modifier.height(28.dp))

                    // -- Fields --
                    Column(modifier = Modifier.graphicsLayer { alpha = formAlpha.value }) {

                        val fieldShape = RoundedCornerShape(16.dp)
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )

                        // 👤 Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors
                        )
                        Spacer(Modifier.height(16.dp))

                        // 📱 Phone
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Number", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(Modifier.height(16.dp))

                        // 📧 Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(16.dp))

                        // 🔒 Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF64748B)) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, "Toggle visibility", tint = Color(0xFF64748B))
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors
                        )

                        Spacer(Modifier.height(32.dp))

                        // ✅ Sign Up Button
                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                errorMessage = ""

                                // 🔴 Validation
                                if (name.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank()) {
                                    errorMessage = "All fields are required"
                                    triggerShake = true
                                    return@Button
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters"
                                    triggerShake = true
                                    return@Button
                                }

                                isLoading = true
                                auth.createUserWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user!!.uid
                                        val generatedId = (System.currentTimeMillis() and 0x7FFFFFFFL).toInt()
                                        val userData = mapOf(
                                            "id" to generatedId,
                                            "name" to name,
                                            "phone" to phone,
                                            "email" to email,
                                            "points" to 0,
                                            "badgeLevel" to "Citizen",
                                            "profile_picture" to null
                                        )

                                        firestore.collection("users").document(uid).set(userData)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                navController.navigate("login") {
                                                    popUpTo("signup") { inclusive = true }
                                                }
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                errorMessage = it.message ?: "Failed to save data"
                                                triggerShake = true
                                            }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        errorMessage = it.message ?: "Signup failed"
                                        triggerShake = true
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981)))),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Already have an account? ", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            Text(
                                "Sign In",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
