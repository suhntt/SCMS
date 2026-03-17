package com.example.scms

data class LeaderboardEntry(
    val id: Int,
    val name: String,
    val points: Int,
    val total_complaints: Int,
    val resolved_complaints: Int,
    val total_upvotes: Int
)
