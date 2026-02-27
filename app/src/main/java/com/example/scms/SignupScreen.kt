package com.example.scms

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignupScreen(navController: NavController) {

    // 🔹 Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // 🔹 UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 🔹 Firebase
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // 🔹 Animation
    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryAnim.animateTo(1f, tween(800))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🌌 Background image
        Image(
            painter = painterResource(R.drawable.auth_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🌑 Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 💎 Glass card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .graphicsLayer {
                    alpha = entryAnim.value
                    translationY = (1 - entryAnim.value) * 120
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    "Join SCMS platform",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(Modifier.height(28.dp))

                // 👤 Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                // 📱 Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                // 📧 Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                // 🔒 Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(14.dp))

                // 🔒 Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(26.dp))

                // ✅ Sign Up Button
                Button(
                    onClick = {
                        errorMessage = ""

                        // 🔴 Validation
                        if (
                            name.isBlank() ||
                            phone.isBlank() ||
                            email.isBlank() ||
                            password.isBlank()
                        ) {
                            errorMessage = "All fields are required"
                            return@Button
                        }

                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters"
                            return@Button
                        }

                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }

                        isLoading = true

                        // 🔐 Firebase Auth
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val uid = result.user!!.uid

                                // 🗄️ Save extra data to Firestore
                                val userData = mapOf(
                                    "name" to name,
                                    "phone" to phone,
                                    "email" to email
                                )

                                firestore.collection("users")
                                    .document(uid)
                                    .set(userData)
                                    .addOnSuccessListener {

                                        isLoading = false

                                        // ✅ Go back to LOGIN after successful signup
                                        navController.navigate("login") {
                                            popUpTo("signup") { inclusive = true }
                                        }
                                    }

                                    .addOnFailureListener {
                                        isLoading = false
                                        errorMessage = it.message ?: "Failed to save user data"
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = it.message ?: "Signup failed"
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    )
                ) {
                    Text(if (isLoading) "Creating account..." else "Sign Up")
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMessage, color = Color.Red, fontSize = 13.sp)
                }

                Spacer(Modifier.height(18.dp))

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Back to Login", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}
