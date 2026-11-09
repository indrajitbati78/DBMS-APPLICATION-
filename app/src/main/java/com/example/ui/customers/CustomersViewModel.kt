package com.example.ui.customers

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Customer
import com.example.data.model.Purchase
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.util.Calendar

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    PAST_7_DAYS("Past 7 Days"),
    PAST_30_DAYS("Past 30 Days")
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME_A_TO_Z("Name (A to Z)"),
    NAME_Z_TO_A("Name (Z to A)")
}

data class CustomersUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val activeFilter: DateFilter = DateFilter.ALL,
    val activeSort: SortOption = SortOption.NEWEST,
    val allCustomers: List<Customer> = emptyList(),
    val filteredCustomers: List<Customer> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
    val error: String? = null
)

class CustomersViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(DateFilter.ALL)
    private val _activeSort = MutableStateFlow(SortOption.NEWEST)

    val uiState: StateFlow<CustomersUiState> = combine(
        customerRepository.getCustomersFlow(),
        customerRepository.getPurchasesFlow(),
        _searchQuery,
        _activeFilter,
        _activeSort
    ) { customers, purchases, query, filter, sort ->
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // 1. Date filter
        val dateFiltered = when (filter) {
            DateFilter.ALL -> customers
            DateFilter.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                customers.filter { it.createdAt >= cal.timeInMillis }
            }
            DateFilter.PAST_7_DAYS -> customers.filter { it.createdAt >= now - (7 * oneDay) }
            DateFilter.PAST_30_DAYS -> customers.filter { it.createdAt >= now - (30 * oneDay) }
        }

        // 2. Search query (name or mobile)
        val searchFiltered = if (query.isBlank()) {
            dateFiltered
        } else {
            val q = query.trim().lowercase()
            dateFiltered.filter {
                it.name.lowercase().contains(q) ||
                it.mobile.contains(q) ||
                it.city.lowercase().contains(q)
            }
        }

        // 3. Sorting
        val sorted = when (sort) {
            SortOption.NEWEST -> searchFiltered.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> searchFiltered.sortedBy { it.createdAt }
            SortOption.NAME_A_TO_Z -> searchFiltered.sortedBy { it.name.lowercase() }
            SortOption.NAME_Z_TO_A -> searchFiltered.sortedByDescending { it.name.lowercase() }
        }

        CustomersUiState(
            isLoading = false,
            searchQuery = query,
            activeFilter = filter,
            activeSort = sort,
            allCustomers = customers,
            filteredCustomers = sorted,
            purchases = purchases,
            error = null
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CustomersUiState()
    )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onFilterChanged(filter: DateFilter) {
        _activeFilter.value = filter
    }

    fun onSortChanged(sort: SortOption) {
        _activeSort.value = sort
    }

    fun exportAndShareCsv(context: Context) {
        val state = uiState.value
        val csvData = customerRepository.generateCustomerCsv(state.filteredCustomers, state.purchases)

        try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "mobile_shop_customers_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)
            writer.write(csvData)
            writer.flush()
            writer.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Mobile Shop Customers Export")
                putExtra(Intent.EXTRA_TEXT, "Exported ${state.filteredCustomers.size} customer records from Mobile Shop Customer Manager.")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Customer Records CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Error handling
        }
    }
}
