package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Purchase(
    val purchaseId: String = "",
    val customerId: String = "",
    val brand: String = "",
    val model: String = "",
    val imei: String = "",
    val price: Double = 0.0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val warrantyExpiry: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
    val billUrl: String = ""
) {
    val isWarrantyActive: Boolean
        get() = warrantyExpiry > System.currentTimeMillis()

    val daysUntilWarrantyExpires: Long
        get() {
            val diff = warrantyExpiry - System.currentTimeMillis()
            return if (diff > 0) diff / (24 * 60 * 60 * 1000) else 0L
        }

    val isExpiringIn30Days: Boolean
        get() = isWarrantyActive && daysUntilWarrantyExpires <= 30

    fun formattedPurchaseDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(purchaseDate))
    }

    fun formattedWarrantyExpiry(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(warrantyExpiry))
    }
}
