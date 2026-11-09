package com.example.data.model

data class CustomerWithPurchases(
    val customer: Customer,
    val purchases: List<Purchase> = emptyList()
) {
    val totalPurchases: Int get() = purchases.size
    val totalSpent: Double get() = purchases.sumOf { it.price }
}
