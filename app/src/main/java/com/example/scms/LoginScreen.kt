package com.example.scms

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    // 🔹 Fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 🔹 UI State
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf("") }
    var resetSuccess by remember { mutableStateOf(false) }
    var triggerShake by remember { mutableStateOf(false) }

    // 🔹 Firebase
    val auth = FirebaseAuth.getInstance()

    // 🔹 Animations
    val entryAnim = remember { Animatable(0f) }
    val shakeAnim = remember { Animatable(0f) }
    
    // Staggered alphas
    val headerAlpha = remember { Animatable(0f) }
    val formAlpha = remember { Animatable(0f) }
    val btnAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entryAnim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        headerAlpha.animateTo(1f, tween(600))
        delay(100)
        formAlpha.animateTo(1f, tween(600))
        delay(100)
        btnAlpha.animateTo(1f, tween(600))
    }

    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(1f, tween(400))
            triggerShake = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌌 High Quality Background
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
                .padding(horizontal = 24.dp)
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
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // -- Header --
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { alpha = headerAlpha.value }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))), CircleShape)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Welcome Back",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Sign in to continue to SCMS",
                            fontSize = 15.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(Modifier.height(36.dp))

                    // -- Form --
                    Column(modifier = Modifier.graphicsLayer { alpha = formAlpha.value }) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF64748B)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF64748B)) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, "Toggle password visibility", tint = Color(0xFF64748B))
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // 🔑 FORGOT PASSWORD
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            TextButton(
                                onClick = {
                                    resetMessage = ""
                                    resetSuccess = false

                                    if (email.isBlank()) {
                                        resetMessage = "Enter your email first"
                                        triggerShake = true
                                        return@TextButton
                                    }

                                    isLoading = true
                                    auth.sendPasswordResetEmail(email.trim())
                                        .addOnSuccessListener {
                                            isLoading = false
                                            resetSuccess = true
                                            resetMessage = "Password reset email sent"
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            resetMessage = e.message ?: "Reset failed"
                                            triggerShake = true
                                        }
                                }
                            ) {
                                Text("Forgot password?", color = Color(0xFF60A5FA), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // -- Submit Button --
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = btnAlpha.value },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                        }

                        if (resetMessage.isNotEmpty()) {
                            Text(
                                resetMessage,
                                color = if (resetSuccess) Color(0xFF22C55E) else Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                errorMessage = ""
                                resetMessage = ""

                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Email and password required"
                                    triggerShake = true
                                    return@Button
                                }

                                isLoading = true
                                auth.signInWithEmailAndPassword(email.trim(), password)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = e.message ?: "Login failed"
                                        triggerShake = true
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Sign In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Don't have an account? ", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            Text(
                                "Sign Up",
                                color = Color(0xFF60A5FA),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { navController.navigate("signup") }
                            )
                        }
                    }
                }
            }
        }
    }
}
