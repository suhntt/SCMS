package com.example.scms

data class ComplaintResponse(
    val id: Int,
    val category: String,
    val address: String,
    val latitude: String,
    val longitude: String,
    val description: String,
    val photo_url: String?,
    val date_time: String,
    val status: String
)
