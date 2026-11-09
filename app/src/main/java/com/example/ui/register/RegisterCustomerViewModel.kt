package com.example.ui.register

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.Purchase
import com.example.data.model.User
import com.example.data.repository.CustomerRepository
import com.example.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class RegisterCustomerUiState(
    // Personal fields
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "Ahmedabad",
    val state: String = "Gujarat",
    val pincode: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,

    // Initial Purchase fields
    val hasPurchase: Boolean = true,
    val brand: String = "Samsung",
    val model: String = "",
    val imei: String = "",
    val price: String = "",
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val warrantyMonths: Int = 12,
    val billUri: Uri? = null,

    // Validation & State
    val isCheckingDuplicate: Boolean = false,
    val isDuplicateWarningVisible: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successCustomerId: String? = null
)

class RegisterCustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterCustomerUiState())
    val uiState = _uiState.asStateFlow()

    fun updateName(v: String) = _uiState.update { it.copy(name = v, errorMessage = null) }
    fun updateMobile(v: String) {
        val filtered = v.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(mobile = filtered, errorMessage = null, isDuplicateWarningVisible = false) }
        if (filtered.length == 10) {
            checkDuplicateMobile(filtered)
        }
    }
    fun updateEmail(v: String) = _uiState.update { it.copy(email = v, errorMessage = null) }
    fun updateAddress(v: String) = _uiState.update { it.copy(address = v) }
    fun updateCity(v: String) = _uiState.update { it.copy(city = v) }
    fun updateState(v: String) = _uiState.update { it.copy(state = v) }
    fun updatePincode(v: String) = _uiState.update { it.copy(pincode = v.filter { it.isDigit() }.take(6)) }
    fun updateNotes(v: String) = _uiState.update { it.copy(notes = v) }
    fun updatePhotoUri(uri: Uri?) = _uiState.update { it.copy(photoUri = uri) }

    fun updateHasPurchase(v: Boolean) = _uiState.update { it.copy(hasPurchase = v) }
    fun updateBrand(v: String) = _uiState.update { it.copy(brand = v) }
    fun updateModel(v: String) = _uiState.update { it.copy(model = v, errorMessage = null) }
    fun updateImei(v: String) = _uiState.update { it.copy(imei = v.filter { it.isDigit() }.take(15), errorMessage = null) }
    fun updatePrice(v: String) = _uiState.update { it.copy(price = v.filter { it.isDigit() || it == '.' }, errorMessage = null) }
    fun updatePurchaseDate(millis: Long) = _uiState.update { it.copy(purchaseDateMillis = millis) }
    fun updateWarrantyMonths(months: Int) = _uiState.update { it.copy(warrantyMonths = months) }
    fun updateBillUri(uri: Uri?) = _uiState.update { it.copy(billUri = uri) }

    private fun checkDuplicateMobile(mobile: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingDuplicate = true) }
            val exists = customerRepository.checkMobileExists(mobile)
            _uiState.update { it.copy(isCheckingDuplicate = false, isDuplicateWarningVisible = exists) }
        }
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(isDuplicateWarningVisible = false) }
    }

    fun submitCustomer(actor: User, forceSubmit: Boolean = false) {
        val state = _uiState.value

        // Validate personal
        if (state.name.trim().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter customer's full name") }
            return
        }
        if (state.mobile.length != 10) {
            _uiState.update { it.copy(errorMessage = "Mobile number must be exactly 10 digits") }
            return
        }
        if (state.email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        // Validate purchase if selected
        if (state.hasPurchase) {
            if (state.model.trim().isBlank()) {
                _uiState.update { it.copy(errorMessage = "Please enter phone model name") }
                return
            }
            if (state.imei.length != 15) {
                _uiState.update { it.copy(errorMessage = "IMEI number must be exactly 15 digits") }
                return
            }
            val priceVal = state.price.toDoubleOrNull()
            if (priceVal == null || priceVal <= 0.0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid purchase price in ₹") }
                return
            }
        }

        if (state.isDuplicateWarningVisible && !forceSubmit) {
            // Already flagged duplicate
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val customerId = "cust_${System.currentTimeMillis().toString().takeLast(8)}"
            
            // Upload photo if present
            var photoDownloadUrl = ""
            state.photoUri?.let { uri ->
                val uploadResult = storageRepository.uploadCustomerPhoto(uri, customerId)
                photoDownloadUrl = uploadResult.getOrDefault(uri.toString())
            }

            val newCustomer = Customer(
                customerId = customerId,
                name = state.name.trim(),
                mobile = state.mobile.trim(),
                email = state.email.trim(),
                address = state.address.trim(),
                city = state.city.trim(),
                state = state.state.trim(),
                pincode = state.pincode.trim(),
                notes = state.notes.trim(),
                photoUrl = photoDownloadUrl,
                createdAt = System.currentTimeMillis(),
                createdBy = actor.name.ifBlank { actor.email }
            )

            var initialPurchase: Purchase? = null
            if (state.hasPurchase) {
                val purchaseId = "pur_${System.currentTimeMillis().toString().takeLast(8)}"
                var billDownloadUrl = ""
                state.billUri?.let { uri ->
                    val uploadResult = storageRepository.uploadBillImage(uri, purchaseId)
                    billDownloadUrl = uploadResult.getOrDefault(uri.toString())
                }

                val expiryMillis = state.purchaseDateMillis + (state.warrantyMonths.toLong() * 30L * 24 * 60 * 60 * 1000)

                initialPurchase = Purchase(
                    purchaseId = purchaseId,
                    customerId = customerId,
                    brand = state.brand.trim(),
                    model = state.model.trim(),
                    imei = state.imei.trim(),
                    price = state.price.toDoubleOrNull() ?: 0.0,
                    purchaseDate = state.purchaseDateMillis,
                    warrantyExpiry = expiryMillis,
                    billUrl = billDownloadUrl
                )
            }

            val result = customerRepository.addCustomer(newCustomer, initialPurchase, actor)
            result.onSuccess { id ->
                _uiState.update { it.copy(isSaving = false, successCustomerId = id) }
            }.onFailure { err ->
                _uiState.update { it.copy(isSaving = false, errorMessage = err.localizedMessage ?: "Failed to save customer") }
            }
        }
    }

    fun resetForm() {
        _uiState.value = RegisterCustomerUiState()
    }
}
