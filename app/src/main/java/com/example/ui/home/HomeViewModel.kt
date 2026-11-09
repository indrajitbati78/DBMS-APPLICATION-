package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.DashboardSummary
import com.example.data.model.Purchase
import com.example.data.repository.CustomerRepository
import com.example.service.ShopFirebaseMessagingService
import kotlinx.coroutines.flow.*

data class HomeUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary = DashboardSummary(),
    val recentCustomers: List<Customer> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val expiringPurchases: List<Purchase> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        combine(
            customerRepository.getCustomersFlow(),
            customerRepository.getPurchasesFlow()
        ) { customers, purchases ->
            val summary = customerRepository.calculateSummary(customers, purchases)
            val recent = customers.take(5)
            val expiring = purchases.filter { it.isExpiringIn30Days }
            HomeUiState(
                isLoading = false,
                summary = summary,
                recentCustomers = recent,
                purchases = purchases,
                expiringPurchases = expiring,
                error = null
            )
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.catch { e ->
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun triggerWarrantyNotification(context: Context) {
        val expiring = _uiState.value.expiringPurchases.firstOrNull()
        if (expiring != null) {
            val customerName = _uiState.value.recentCustomers.find { it.customerId == expiring.customerId }?.name ?: "Customer"
            ShopFirebaseMessagingService.triggerLocalWarrantyAlert(
                context,
                customerName,
                "${expiring.brand} ${expiring.model}",
                expiring.daysUntilWarrantyExpires
            )
        } else {
            ShopFirebaseMessagingService.triggerLocalWarrantyAlert(
                context,
                "Indrajit Gadhavi",
                "Samsung Galaxy S24 Ultra",
                14
            )
        }
    }
}
