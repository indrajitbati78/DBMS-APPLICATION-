package com.example.data.repository

import com.example.data.model.ActivityLog
import com.example.data.model.Customer
import com.example.data.model.DashboardSummary
import com.example.data.model.Purchase
import com.example.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

class CustomerRepository(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (t: Throwable) { null }
) {
    // In-memory fallback dataset for seamless offline operation & instant preview
    private val _fallbackCustomers = MutableStateFlow<List<Customer>>(createInitialDemoCustomers())
    private val _fallbackPurchases = MutableStateFlow<List<Purchase>>(createInitialDemoPurchases())
    private val _fallbackLogs = MutableStateFlow<List<ActivityLog>>(createInitialDemoLogs())

    // -------------------------------------------------------------
    // Customers Flow
    // -------------------------------------------------------------
    fun getCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(_fallbackCustomers.value)
            awaitClose { }
            return@callbackFlow
        }

        try {
            val listenerRegistration = fs.collection("customers")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_fallbackCustomers.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Customer::class.java)?.copy(customerId = doc.id)
                        }
                        if (list.isEmpty() && _fallbackCustomers.value.isNotEmpty()) {
                            trySend(_fallbackCustomers.value)
                        } else {
                            trySend(list)
                        }
                    }
                }
            awaitClose { listenerRegistration.remove() }
        } catch (e: Throwable) {
            trySend(_fallbackCustomers.value)
            awaitClose { }
        }
    }

    // -------------------------------------------------------------
    // Purchases Flow
    // -------------------------------------------------------------
    fun getPurchasesFlow(): Flow<List<Purchase>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(_fallbackPurchases.value)
            awaitClose { }
            return@callbackFlow
        }

        try {
            val listenerRegistration = fs.collection("purchases")
                .orderBy("purchaseDate", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_fallbackPurchases.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Purchase::class.java)?.copy(purchaseId = doc.id)
                        }
                        if (list.isEmpty() && _fallbackPurchases.value.isNotEmpty()) {
                            trySend(_fallbackPurchases.value)
                        } else {
                            trySend(list)
                        }
                    }
                }
            awaitClose { listenerRegistration.remove() }
        } catch (e: Throwable) {
            trySend(_fallbackPurchases.value)
            awaitClose { }
        }
    }

    // -------------------------------------------------------------
    // Activity Logs Flow (Admin only)
    // -------------------------------------------------------------
    fun getActivityLogsFlow(): Flow<List<ActivityLog>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(_fallbackLogs.value)
            awaitClose { }
            return@callbackFlow
        }

        try {
            val listener = fs.collection("activityLogs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(_fallbackLogs.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ActivityLog::class.java)?.copy(logId = doc.id)
                        }
                        if (list.isEmpty() && _fallbackLogs.value.isNotEmpty()) {
                            trySend(_fallbackLogs.value)
                        } else {
                            trySend(list)
                        }
                    }
                }
            awaitClose { listener.remove() }
        } catch (e: Throwable) {
            trySend(_fallbackLogs.value)
            awaitClose { }
        }
    }

    // -------------------------------------------------------------
    // Check Mobile Number Existence
    // -------------------------------------------------------------
    suspend fun checkMobileExists(mobile: String, currentCustomerId: String? = null): Boolean {
        val cleanMobile = mobile.trim()
        if (cleanMobile.length < 10) return false

        val existsInFallback = _fallbackCustomers.value.any {
            it.mobile.trim() == cleanMobile && it.customerId != currentCustomerId
        }
        if (existsInFallback) return true

        val fs = firestore ?: return false
        return try {
            val query = fs.collection("customers")
                .whereEqualTo("mobile", cleanMobile)
                .get()
                .await()

            query.documents.any { it.id != currentCustomerId }
        } catch (e: Throwable) {
            existsInFallback
        }
    }

    // -------------------------------------------------------------
    // Add Customer & Initial Purchase
    // -------------------------------------------------------------
    suspend fun addCustomer(
        customer: Customer,
        initialPurchase: Purchase?,
        actor: User
    ): Result<String> {
        return try {
            val customerId = if (customer.customerId.isBlank()) "cust_${UUID.randomUUID().toString().take(8)}" else customer.customerId
            val finalCustomer = customer.copy(
                customerId = customerId,
                createdAt = System.currentTimeMillis(),
                createdBy = actor.name.ifBlank { actor.email }
            )

            try {
                firestore?.collection("customers")?.document(customerId)?.set(finalCustomer)?.await()
            } catch (e: Throwable) {
                // Offline fallback
            }

            val updatedList = listOf(finalCustomer) + _fallbackCustomers.value.filter { it.customerId != customerId }
            _fallbackCustomers.value = updatedList

            if (initialPurchase != null && initialPurchase.brand.isNotBlank()) {
                val purchaseId = "pur_${UUID.randomUUID().toString().take(8)}"
                val finalPurchase = initialPurchase.copy(
                    purchaseId = purchaseId,
                    customerId = customerId
                )
                try {
                    firestore?.collection("purchases")?.document(purchaseId)?.set(finalPurchase)?.await()
                } catch (e: Throwable) {
                    // Local fallback
                }
                _fallbackPurchases.value = listOf(finalPurchase) + _fallbackPurchases.value
            }

            logActivity(
                action = "Registered customer ${finalCustomer.name} (${finalCustomer.mobile})",
                targetId = customerId,
                actor = actor
            )

            Result.success(customerId)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Update Customer
    // -------------------------------------------------------------
    suspend fun updateCustomer(customer: Customer, actor: User): Result<Unit> {
        return try {
            try {
                firestore?.collection("customers")?.document(customer.customerId)?.set(customer)?.await()
            } catch (e: Throwable) {
                // Fallback
            }

            _fallbackCustomers.value = _fallbackCustomers.value.map {
                if (it.customerId == customer.customerId) customer else it
            }

            logActivity(
                action = "Updated customer ${customer.name}",
                targetId = customer.customerId,
                actor = actor
            )

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Delete Customer (Admin Only)
    // -------------------------------------------------------------
    suspend fun deleteCustomer(customerId: String, actor: User): Result<Unit> {
        if (!actor.isAdmin) {
            return Result.failure(SecurityException("Only Admin can delete customer records"))
        }

        return try {
            val customerName = _fallbackCustomers.value.find { it.customerId == customerId }?.name ?: customerId

            try {
                firestore?.let { fs ->
                    fs.collection("customers").document(customerId).delete().await()
                    val purchasesSnapshot = fs.collection("purchases")
                        .whereEqualTo("customerId", customerId)
                        .get()
                        .await()
                    for (doc in purchasesSnapshot.documents) {
                        doc.reference.delete()
                    }
                }
            } catch (e: Throwable) {
                // Fallback
            }

            _fallbackCustomers.value = _fallbackCustomers.value.filter { it.customerId != customerId }
            _fallbackPurchases.value = _fallbackPurchases.value.filter { it.customerId != customerId }

            logActivity(
                action = "Deleted customer $customerName",
                targetId = customerId,
                actor = actor
            )

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Add Purchase for Existing Customer
    // -------------------------------------------------------------
    suspend fun addPurchase(purchase: Purchase, actor: User): Result<String> {
        return try {
            val purchaseId = if (purchase.purchaseId.isBlank()) "pur_${UUID.randomUUID().toString().take(8)}" else purchase.purchaseId
            val finalPurchase = purchase.copy(purchaseId = purchaseId)

            try {
                firestore?.collection("purchases")?.document(purchaseId)?.set(finalPurchase)?.await()
            } catch (e: Throwable) {
                // Fallback
            }

            _fallbackPurchases.value = listOf(finalPurchase) + _fallbackPurchases.value

            logActivity(
                action = "Added purchase: ${finalPurchase.brand} ${finalPurchase.model} (₹${finalPurchase.price.toInt()})",
                targetId = finalPurchase.customerId,
                actor = actor
            )

            Result.success(purchaseId)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Delete Purchase (Admin Only)
    // -------------------------------------------------------------
    suspend fun deletePurchase(purchaseId: String, actor: User): Result<Unit> {
        if (!actor.isAdmin) {
            return Result.failure(SecurityException("Only Admin can delete purchase records"))
        }

        return try {
            val target = _fallbackPurchases.value.find { it.purchaseId == purchaseId }
            try {
                firestore?.collection("purchases")?.document(purchaseId)?.delete()?.await()
            } catch (e: Throwable) {
                // Fallback
            }

            _fallbackPurchases.value = _fallbackPurchases.value.filter { it.purchaseId != purchaseId }

            logActivity(
                action = "Deleted purchase ${target?.brand ?: ""} ${target?.model ?: ""}",
                targetId = target?.customerId ?: purchaseId,
                actor = actor
            )

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Log Activity
    // -------------------------------------------------------------
    suspend fun logActivity(action: String, targetId: String, actor: User) {
        val logId = "log_${UUID.randomUUID().toString().take(8)}"
        val log = ActivityLog(
            logId = logId,
            userId = actor.uid,
            userName = actor.name.ifBlank { actor.email },
            action = action,
            targetId = targetId,
            timestamp = System.currentTimeMillis()
        )

        try {
            firestore?.collection("activityLogs")?.document(logId)?.set(log)?.await()
        } catch (e: Throwable) {
            // Ignore
        }

        _fallbackLogs.value = listOf(log) + _fallbackLogs.value
    }

    // -------------------------------------------------------------
    // Calculate Dashboard Summary
    // -------------------------------------------------------------
    fun calculateSummary(customers: List<Customer>, purchases: List<Purchase>): DashboardSummary {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        val registeredToday = customers.count { it.createdAt >= startOfToday }
        val warrantyExpiringSoon = purchases.count { it.isExpiringIn30Days }
        val totalSales = purchases.sumOf { it.price }

        return DashboardSummary(
            totalCustomers = customers.size,
            registeredToday = registeredToday,
            warrantyExpiringSoon = warrantyExpiringSoon,
            totalSalesValue = totalSales
        )
    }

    // -------------------------------------------------------------
    // CSV Export Generator
    // -------------------------------------------------------------
    fun generateCustomerCsv(customers: List<Customer>, purchases: List<Purchase>): String {
        val purchasesByCustomer = purchases.groupBy { it.customerId }
        val sb = StringBuilder()
        sb.append("Customer ID,Name,Mobile,Email,City,State,Pincode,Total Purchases,Total Spent (INR),Registration Date,Registered By,Notes\n")

        for (c in customers) {
            val custPurchases = purchasesByCustomer[c.customerId] ?: emptyList()
            val totalSpent = custPurchases.sumOf { it.price }
            val totalPurchases = custPurchases.size

            val escape = { str: String ->
                if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
                    "\"${str.replace("\"", "\"\"")}\""
                } else {
                    str
                }
            }

            val regDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(c.createdAt))

            sb.append("${escape(c.customerId)},")
            sb.append("${escape(c.name)},")
            sb.append("${escape(c.mobile)},")
            sb.append("${escape(c.email)},")
            sb.append("${escape(c.city)},")
            sb.append("${escape(c.state)},")
            sb.append("${escape(c.pincode)},")
            sb.append("$totalPurchases,")
            sb.append("$totalSpent,")
            sb.append("$regDate,")
            sb.append("${escape(c.createdBy)},")
            sb.append("${escape(c.notes)}\n")
        }
        return sb.toString()
    }

    // -------------------------------------------------------------
    // Initial Realistic Seed Data
    // -------------------------------------------------------------
    private fun createInitialDemoCustomers(): List<Customer> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        return listOf(
            Customer(
                customerId = "cust_001",
                name = "Indrajit Bati",
                mobile = "9876543210",
                email = "indrajitbati@gmail.com",
                address = "Shop 12, Gota Cross Road",
                city = "Ahmedabad",
                state = "Gujarat",
                pincode = "382481",
                notes = "Prefers flagship Samsung phones with extended warranty",
                photoUrl = "",
                createdAt = now - (2 * 60 * 60 * 1000), // Today
                createdBy = "Admin"
            ),
            Customer(
                customerId = "cust_002",
                name = "Rahul Patel",
                mobile = "9825012345",
                email = "rahul.patel@gmail.com",
                address = "24, Shivalik Plaza, CG Road",
                city = "Ahmedabad",
                state = "Gujarat",
                pincode = "380009",
                notes = "Regular customer, bought OnePlus 12R",
                photoUrl = "",
                createdAt = now - (1 * oneDay), // Yesterday
                createdBy = "Staff 1"
            ),
            Customer(
                customerId = "cust_003",
                name = "Priya Shah",
                mobile = "9909087654",
                email = "priya.shah@outlook.com",
                address = "502, Orchid Heights, Satellite",
                city = "Ahmedabad",
                state = "Gujarat",
                pincode = "380015",
                notes = "Apple ecosystem buyer; warranty expires in 12 days",
                photoUrl = "",
                createdAt = now - (280 * oneDay),
                createdBy = "Admin"
            ),
            Customer(
                customerId = "cust_004",
                name = "Amit Sharma",
                mobile = "9426033445",
                email = "amit.sharma@yahoo.com",
                address = "14, Galaxy Arcade, Ring Road",
                city = "Surat",
                state = "Gujarat",
                pincode = "395002",
                notes = "Redmi Note series customer, cash payment",
                photoUrl = "",
                createdAt = now - (15 * oneDay),
                createdBy = "Staff 2"
            ),
            Customer(
                customerId = "cust_005",
                name = "Sneha Mehta",
                mobile = "9712345678",
                email = "sneha.mehta@gmail.com",
                address = "88, Alkapuri",
                city = "Vadodara",
                state = "Gujarat",
                pincode = "390007",
                notes = "Vivo V30 camera phone buyer",
                photoUrl = "",
                createdAt = now - (35 * oneDay),
                createdBy = "Staff 1"
            )
        )
    }

    private fun createInitialDemoPurchases(): List<Purchase> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        return listOf(
            Purchase(
                purchaseId = "pur_001",
                customerId = "cust_001",
                brand = "Samsung",
                model = "Galaxy S24 Ultra 512GB",
                imei = "354892018472910",
                price = 129999.0,
                purchaseDate = now - (2 * 60 * 60 * 1000),
                warrantyExpiry = now + (365L * oneDay),
                billUrl = ""
            ),
            Purchase(
                purchaseId = "pur_002",
                customerId = "cust_002",
                brand = "OnePlus",
                model = "12R 256GB Cool Blue",
                imei = "864201948271054",
                price = 45999.0,
                purchaseDate = now - (1 * oneDay),
                warrantyExpiry = now + (364L * oneDay),
                billUrl = ""
            ),
            Purchase(
                purchaseId = "pur_003",
                customerId = "cust_003",
                brand = "Apple",
                model = "iPhone 15 128GB Black",
                imei = "359182746201948",
                price = 69900.0,
                purchaseDate = now - (350 * oneDay),
                warrantyExpiry = now + (15 * oneDay), // Expiring in 15 days!
                billUrl = ""
            ),
            Purchase(
                purchaseId = "pur_004",
                customerId = "cust_004",
                brand = "Xiaomi",
                model = "Redmi Note 13 Pro+ 5G",
                imei = "865019284756201",
                price = 31999.0,
                purchaseDate = now - (15 * oneDay),
                warrantyExpiry = now + (350L * oneDay),
                billUrl = ""
            ),
            Purchase(
                purchaseId = "pur_005",
                customerId = "cust_005",
                brand = "Vivo",
                model = "V30 Pro 5G Andaman Blue",
                imei = "861948203948572",
                price = 41999.0,
                purchaseDate = now - (35 * oneDay),
                warrantyExpiry = now + (20 * oneDay), // Expiring in 20 days!
                billUrl = ""
            )
        )
    }

    private fun createInitialDemoLogs(): List<ActivityLog> {
        val now = System.currentTimeMillis()
        val oneHour = 60L * 60 * 1000
        return listOf(
            ActivityLog(
                logId = "log_001",
                userId = "admin_01",
                userName = "Indrajit Bati (Admin)",
                action = "Registered customer Indrajit Bati (9876543210)",
                targetId = "cust_001",
                timestamp = now - (2 * oneHour)
            ),
            ActivityLog(
                logId = "log_002",
                userId = "admin_01",
                userName = "Indrajit Bati (Admin)",
                action = "Added purchase: Samsung Galaxy S24 Ultra (₹129,999)",
                targetId = "cust_001",
                timestamp = now - (2 * oneHour)
            ),
            ActivityLog(
                logId = "log_003",
                userId = "staff_01",
                userName = "Shop Staff 1",
                action = "Registered customer Rahul Patel (9825012345)",
                targetId = "cust_002",
                timestamp = now - (24 * oneHour)
            )
        )
    }
}
