package com.example.scms

data class ProfileUpdateResponse(
    val success: Boolean,
    val profile_picture: String?,
    val message: String?
)
