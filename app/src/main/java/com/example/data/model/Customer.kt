package com.example.data.model

data class Customer(
    val customerId: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val notes: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "" // User UID or Name
)
