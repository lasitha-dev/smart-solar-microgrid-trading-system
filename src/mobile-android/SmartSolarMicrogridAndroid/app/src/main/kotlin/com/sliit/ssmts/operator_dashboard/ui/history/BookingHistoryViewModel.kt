/**
 * Description: ViewModel managing debounced case-insensitive search queries, status filter chips,
 * and reactive reservation history streaming for the grid operator (FR-M4-03).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder and search coordinator for the booking history subsystem.
 *
 * @property repository Injected repository abstraction for querying cached energy reservations.
 */
class BookingHistoryViewModel(
    private val repository: IDashboardRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow("All")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Reactive UI state stream combining 300ms debounced search keywords and filter status chips (FR-M4-03.2).
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val historyUiState: StateFlow<UiState<List<Reservation>>> = combine(
        _searchQuery.debounce(300L),
        _selectedStatus
    ) { query, status ->
        Pair(query, status)
    }.flatMapLatest { (query, status) ->
        repository.getCachedReservationsStream(status = status, search = query)
            .map<List<Reservation>, UiState<List<Reservation>>> { list ->
                UiState.Success(list)
            }
            .onStart {
                emit(UiState.Loading)
            }
            .catch { e ->
                emit(UiState.Error(e.localizedMessage ?: "Failed to load reservation history"))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiState.Loading
    )

    /**
     * Updates the active keyword search query, triggering debounced flow emission.
     *
     * @param query Search query matching station name, prosumer NIC, or reservation reference ID.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the active status filter chip ('All', 'Pending', 'Approved', 'Completed', 'Cancelled').
     *
     * @param status Selected status filter name.
     */
    fun onStatusFilterSelected(status: String) {
        _selectedStatus.value = status
    }

    /**
     * Dispatches manual server synchronization, pulling latest reservations to SQLite cache.
     */
    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.syncRemoteReservations()
            } catch (_: Exception) {
                // Network failures preserve offline SQLite cache gracefully
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Factory constructing BookingHistoryViewModel with injected IDashboardRepository.
     */
    class Factory(
        private val repository: IDashboardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookingHistoryViewModel::class.java)) {
                return BookingHistoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
