package com.example.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.Purchase
import com.example.data.model.User
import com.example.data.repository.CustomerRepository
import com.example.data.repository.StorageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerProfileUiState(
    val isLoading: Boolean = true,
    val customer: Customer? = null,
    val purchases: List<Purchase> = emptyList(),
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CustomerProfileViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCustomerData()
    }

    private fun loadCustomerData() {
        combine(
            customerRepository.getCustomersFlow(),
            customerRepository.getPurchasesFlow()
        ) { customers, purchases ->
            val cust = customers.find { it.customerId == customerId }
            val custPurchases = purchases.filter { it.customerId == customerId }
            CustomerProfileUiState(
                isLoading = false,
                customer = cust,
                purchases = custPurchases,
                isDeleted = (cust == null && !_uiState.value.isLoading),
                errorMessage = if (cust == null) "Customer record not found" else null
            )
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun dialCustomer(context: Context) {
        val mobile = _uiState.value.customer?.mobile ?: return
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:+91$mobile")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Error
        }
    }

    fun openWhatsApp(context: Context) {
        val mobile = _uiState.value.customer?.mobile ?: return
        val name = _uiState.value.customer?.name ?: "Customer"
        val message = "Hello $name, Greetings from Mobile Shop! We are contacting you regarding your mobile purchase and service."
        val url = "https://wa.me/91$mobile?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Error
        }
    }

    fun updateCustomerDetails(updatedCustomer: Customer, actor: User) {
        viewModelScope.launch {
            val res = customerRepository.updateCustomer(updatedCustomer, actor)
            res.onSuccess {
                _uiState.update { it.copy(successMessage = "Customer updated successfully") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage) }
            }
        }
    }

    fun deleteCustomer(actor: User) {
        viewModelScope.launch {
            val res = customerRepository.deleteCustomer(customerId, actor)
            res.onSuccess {
                _uiState.update { it.copy(isDeleted = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage) }
            }
        }
    }

    fun addPurchase(
        brand: String,
        model: String,
        imei: String,
        price: Double,
        warrantyMonths: Int,
        billUri: Uri?,
        actor: User
    ) {
        viewModelScope.launch {
            val purchaseId = "pur_${System.currentTimeMillis().toString().takeLast(8)}"
            var billUrl = ""
            billUri?.let { uri ->
                val uploadRes = storageRepository.uploadBillImage(uri, purchaseId)
                billUrl = uploadRes.getOrDefault(uri.toString())
            }

            val expiry = System.currentTimeMillis() + (warrantyMonths.toLong() * 30L * 24 * 60 * 60 * 1000)
            val newPurchase = Purchase(
                purchaseId = purchaseId,
                customerId = customerId,
                brand = brand,
                model = model,
                imei = imei,
                price = price,
                purchaseDate = System.currentTimeMillis(),
                warrantyExpiry = expiry,
                billUrl = billUrl
            )

            val res = customerRepository.addPurchase(newPurchase, actor)
            res.onSuccess {
                _uiState.update { it.copy(successMessage = "Purchase added successfully") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage) }
            }
        }
    }

    fun deletePurchase(purchaseId: String, actor: User) {
        viewModelScope.launch {
            customerRepository.deletePurchase(purchaseId, actor)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
