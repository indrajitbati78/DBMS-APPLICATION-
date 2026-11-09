package com.example.data.repository

import com.example.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (t: Throwable) { null },
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (t: Throwable) { null }
) {
    private val _currentUserState = MutableStateFlow<User?>(null)
    val currentUserState = _currentUserState.asStateFlow()

    init {
        try {
            auth?.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    fetchUserProfile(fbUser.uid, fbUser.email ?: "", fbUser.displayName ?: "User")
                } else if (_currentUserState.value != null && !_isDemoUser(_currentUserState.value)) {
                    _currentUserState.value = null
                }
            }
        } catch (e: Throwable) {
            // Handled gracefully
        }
    }

    private fun _isDemoUser(user: User?): Boolean {
        return user?.uid?.startsWith("demo_") == true
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized on this device"))
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = fbAuth.signInWithCredential(credential).await()
            val fbUser = authResult.user ?: throw Exception("User is null after Google sign in")
            
            val user = try {
                val doc = firestore?.collection("users")?.document(fbUser.uid)?.get()?.await()
                if (doc != null && doc.exists()) {
                    doc.toObject(User::class.java)?.copy(uid = fbUser.uid) ?: User(
                        uid = fbUser.uid,
                        name = fbUser.displayName ?: "Google User",
                        email = fbUser.email ?: "",
                        role = User.ROLE_STAFF
                    )
                } else {
                    val newUser = User(
                        uid = fbUser.uid,
                        name = fbUser.displayName ?: "Google User",
                        email = fbUser.email ?: "",
                        role = User.ROLE_STAFF
                    )
                    firestore?.collection("users")?.document(fbUser.uid)?.set(newUser)?.await()
                    newUser
                }
            } catch (e: Throwable) {
                User(
                    uid = fbUser.uid,
                    name = fbUser.displayName ?: "Google User",
                    email = fbUser.email ?: "",
                    role = User.ROLE_STAFF
                )
            }
            _currentUserState.value = user
            Result.success(user)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<User> {
        // 1. Owner & Staff credentials
        if (email.equals("indrajit3232@gmail.com", ignoreCase = true) && pass == "mobileshop4578") {
            val adminUser = User(
                uid = "owner_indrajit_01",
                name = "Indrajit Bati (Owner)",
                email = "indrajit3232@gmail.com",
                role = User.ROLE_ADMIN
            )
            _currentUserState.value = adminUser
            return Result.success(adminUser)
        }
        if (email.equals("staff3232@gmail.com", ignoreCase = true) && pass == "mobileshop4578") {
            val staffUser = User(
                uid = "staff_user_01",
                name = "Shop Staff",
                email = "staff3232@gmail.com",
                role = User.ROLE_STAFF
            )
            _currentUserState.value = staffUser
            return Result.success(staffUser)
        }

        val fbAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized on this device"))

        // 2. Real Firebase Authentication
        return try {
            val authResult = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val fbUser = authResult.user ?: throw Exception("User is null after sign in")
            
            val user = try {
                val doc = firestore?.collection("users")?.document(fbUser.uid)?.get()?.await()
                if (doc != null && doc.exists()) {
                    doc.toObject(User::class.java)?.copy(uid = fbUser.uid) ?: User(
                        uid = fbUser.uid,
                        name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                        email = fbUser.email ?: email,
                        role = User.ROLE_STAFF
                    )
                } else {
                    val newUser = User(
                        uid = fbUser.uid,
                        name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                        email = fbUser.email ?: email,
                        role = User.ROLE_STAFF
                    )
                    firestore?.collection("users")?.document(fbUser.uid)?.set(newUser)?.await()
                    newUser
                }
            } catch (e: Throwable) {
                User(
                    uid = fbUser.uid,
                    name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "User",
                    email = fbUser.email ?: email,
                    role = User.ROLE_STAFF
                )
            }
            _currentUserState.value = user
            Result.success(user)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, name: String, role: String): Result<User> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized on this device"))
        return try {
            val authResult = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val fbUser = authResult.user ?: throw Exception("User is null after registration")
            
            val newUser = User(
                uid = fbUser.uid,
                name = name.trim(),
                email = email.trim(),
                role = role
            )
            try {
                firestore?.collection("users")?.document(fbUser.uid)?.set(newUser)?.await()
            } catch (e: Throwable) {
                // Ignore offline
            }
            _currentUserState.value = newUser
            Result.success(newUser)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Throwable) {
            // Ignore
        }
        _currentUserState.value = null
    }

    private fun fetchUserProfile(uid: String, email: String, name: String) {
        val fs = firestore
        if (fs == null) {
            _currentUserState.value = User(uid = uid, name = name, email = email, role = User.ROLE_STAFF)
            return
        }

        fs.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)?.copy(uid = uid)
                    _currentUserState.value = user
                } else {
                    val defaultUser = User(uid = uid, name = name, email = email, role = User.ROLE_STAFF)
                    _currentUserState.value = defaultUser
                }
            }
            .addOnFailureListener {
                val fallbackUser = User(uid = uid, name = name, email = email, role = User.ROLE_STAFF)
                _currentUserState.value = fallbackUser
            }
    }

    fun switchDemoRole(role: String) {
        val curr = _currentUserState.value
        if (curr != null) {
            _currentUserState.value = curr.copy(role = role)
        } else {
            _currentUserState.value = User(
                uid = "demo_user",
                name = if (role == User.ROLE_ADMIN) "Indrajit Bati (Admin)" else "Staff Member",
                email = if (role == User.ROLE_ADMIN) "indrajit3232@gmail.com" else "staff3232@gmail.com",
                role = role
            )
        }
    }
}
