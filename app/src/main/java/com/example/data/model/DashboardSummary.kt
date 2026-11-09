package com.example.data.model

data class DashboardSummary(
    val totalCustomers: Int = 0,
    val registeredToday: Int = 0,
    val warrantyExpiringSoon: Int = 0,
    val totalSalesValue: Double = 0.0
)
