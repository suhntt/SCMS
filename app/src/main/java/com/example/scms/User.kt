package com.example.scms

data class User(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String? = null,
    val profile_picture: String? = null,
    val points: Int = 0,
    val badgeLevel: String = "Citizen"
)
