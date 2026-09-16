/**
 * Description: ViewModel managing state for operational metrics counter cards, active booking spotlight,
 * offline fallback indication, and swipe-to-refresh synchronization (FR-M4-01).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder and business coordinator for the operational dashboard UI.
 *
 * @property repository Injected repository abstraction for retrieving operational metrics.
 */
class DashboardViewModel(
    private val repository: IDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardMetrics>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardMetrics>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadMetrics(forceRefresh = false)
    }

    /**
     * Dispatches operational metrics query to the repository, streaming cached and remote states.
     *
     * @param forceRefresh Whether to force remote synchronization bypassing cache.
     */
    fun loadMetrics(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && _uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            }

            repository.getDashboardMetricsStream(forceRefresh = forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.value = UiState.Success(result.data)
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = UiState.Error(
                            message = result.message,
                            errorCode = result.code
                        )
                    }
                    is NetworkResult.Exception -> {
                        _uiState.value = UiState.Error(
                            message = result.throwable.localizedMessage ?: "Unexpected connection error occurred."
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes manual swipe-to-refresh, coordinating remote reservation synchronization
     * and live metrics polling while managing the isRefreshing indicator.
     */
    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                try {
                    repository.syncRemoteReservations()
                } catch (_: Exception) {
                    // Remote sync failure falls back to local cache gracefully
                }
                loadMetrics(forceRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Factory for constructing DashboardViewModel instances with injected IDashboardRepository.
     */
    class Factory(
        private val repository: IDashboardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
