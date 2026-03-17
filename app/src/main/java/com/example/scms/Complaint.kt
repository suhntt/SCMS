package com.example.scms

data class Complaint(
    val id: Int,
    val category: String? = null,
    val address: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val description: String? = null,
    val photo_url: String? = null,
    val status: String? = null,
    val upvotes: Int = 0,
    val created_at: String? = null,
    val department: String? = null,
    val user_id: Int? = null,
    val reporter_name: String? = null   // 🏆 Gamification: who filed this complaint
)
