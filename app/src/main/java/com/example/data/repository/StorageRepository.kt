package com.example.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository(
    private val storage: FirebaseStorage? = try { FirebaseStorage.getInstance() } catch (t: Throwable) { null }
) {
    suspend fun uploadCustomerPhoto(imageUri: Uri, customerId: String): Result<String> {
        val st = storage ?: return Result.success(imageUri.toString())
        return try {
            val fileName = "customer_${customerId}_${UUID.randomUUID().toString().take(6)}.jpg"
            val ref = st.reference.child("customers/$fileName")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Throwable) {
            Result.success(imageUri.toString())
        }
    }

    suspend fun uploadBillImage(imageUri: Uri, purchaseId: String): Result<String> {
        val st = storage ?: return Result.success(imageUri.toString())
        return try {
            val fileName = "bill_${purchaseId}_${UUID.randomUUID().toString().take(6)}.jpg"
            val ref = st.reference.child("bills/$fileName")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Throwable) {
            Result.success(imageUri.toString())
        }
    }
}
