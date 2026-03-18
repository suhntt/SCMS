package com.example.scms

data class PointsResponse(
    val points: Int,
    val email: String?,
    val phone: String?,
    val name: String?,
    val profile_picture: String?,
    val badgeLevel: String?
)
