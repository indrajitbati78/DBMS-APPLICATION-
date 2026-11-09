package com.example.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityLog
import com.example.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*

data class ActivityLogUiState(
    val isLoading: Boolean = true,
    val logs: List<ActivityLog> = emptyList(),
    val error: String? = null
)

class ActivityLogViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityLogUiState())
    val uiState = _uiState.asStateFlow()

    init {
        customerRepository.getActivityLogsFlow()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) } }
            .onEach { logs -> _uiState.update { it.copy(isLoading = false, logs = logs) } }
            .launchIn(viewModelScope)
    }
}
