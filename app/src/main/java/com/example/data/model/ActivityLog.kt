package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityLog(
    val logId: String = "",
    val userId: String = "",
    val userName: String = "",
    val action: String = "",
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
