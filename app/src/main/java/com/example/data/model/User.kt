package com.example.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = ROLE_STAFF // "Admin" or "Staff"
) {
    val isAdmin: Boolean get() = role.equals(ROLE_ADMIN, ignoreCase = true)

    companion object {
        const val ROLE_ADMIN = "Admin"
        const val ROLE_STAFF = "Staff"
    }
}
