package com.example

import android.app.Application
import com.example.data.repository.AuthRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.StorageRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class MobileShopApplication : Application() {

    lateinit var authRepository: AuthRepository
        private set
    lateinit var customerRepository: CustomerRepository
        private set
    lateinit var storageRepository: StorageRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase if available
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            // Handled gracefully in tests or offline
        }

        // 2. Resolve provisioned Firestore database
        val customDatabaseId = try {
            val resId = resources.getIdentifier("firestore_database_id", "string", packageName)
            if (resId != 0) getString(resId) else ""
        } catch (e: Throwable) {
            ""
        }

        val firestore: FirebaseFirestore? = try {
            val fs = if (customDatabaseId.isNotBlank()) {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), customDatabaseId)
            } else {
                FirebaseFirestore.getInstance()
            }
            val settings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings { })
            }
            fs.firestoreSettings = settings
            fs
        } catch (e: Throwable) {
            null
        }

        // 3. Initialize singletons with the provisioned Firestore instance
        authRepository = AuthRepository(firestore = firestore)
        customerRepository = CustomerRepository(firestore = firestore)
        storageRepository = StorageRepository()
    }
}
