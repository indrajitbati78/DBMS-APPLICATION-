package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Register : Screen("register")
    object Customers : Screen("customers")
    object CustomerProfile : Screen("customer_profile/{customerId}") {
        fun createRoute(customerId: String) = "customer_profile/$customerId"
    }
    object ActivityLog : Screen("activity_log")
}
