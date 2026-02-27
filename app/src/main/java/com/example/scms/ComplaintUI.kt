package com.example.scms

data class ComplaintUI(
    val category: String,
    val address: String,
    val lat: String,
    val lon: String,
    val dateTime: String,
    val description: String,
    val status: String,
    var upvotes: Int = 0
)
