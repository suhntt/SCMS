package com.example.scms

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {

    // 🔹 Fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

    LaunchedEffect(Unit) {
        entryAnim.animateTo(1f, tween(800))
    }

    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(1f, tween(400))
            triggerShake = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.auth_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .graphicsLayer {
                    alpha = entryAnim.value
                    translationY = (1 - entryAnim.value) * 120 +
                            if (shakeAnim.value > 0f) (-12..12).random() else 0
                },
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.10f)
            ),
            elevation = CardDefaults.cardElevation(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Welcome Back",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    "Login to continue",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 🔑 FORGOT PASSWORD
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
                    Text("Forgot password?", color = Color(0xFF38BDF8))
                }

                Spacer(Modifier.height(16.dp))

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

                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        )
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
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text("Login", fontSize = 16.sp)
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(errorMessage, color = Color.Red, fontSize = 13.sp)
                }

                if (resetMessage.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        resetMessage,
                        color = if (resetSuccess) Color(0xFF22C55E) else Color.Red,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(18.dp))

                TextButton(onClick = { navController.navigate("signup") }) {
                    Text("Create new account", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}
