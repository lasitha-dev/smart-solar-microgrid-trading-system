/**
 * Description: ViewModel managing state for operational metrics counter cards, active booking spotlight,
 * offline fallback indication, swipe-to-refresh synchronization, and real-time operational feeds (FR-M4-01, FR-M4-02).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import com.sliit.ssmts.operator_dashboard.util.TransferSyncNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder and business coordinator for the operational dashboard UI.
 *
 * @property repository Injected repository abstraction for retrieving operational metrics and cached booking feeds.
 */
class DashboardViewModel(
    private val repository: IDashboardRepository,
    private val operatorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DashboardMetrics>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardMetrics>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedFeedTab = MutableStateFlow(DashboardFeedTab.TODAY_ACTIVE)
    val selectedFeedTab: StateFlow<DashboardFeedTab> = _selectedFeedTab.asStateFlow()

    private val _todayActiveBookings = MutableStateFlow<List<Reservation>>(emptyList())
    val todayActiveBookings: StateFlow<List<Reservation>> = _todayActiveBookings.asStateFlow()

    private val _pendingQueueBookings = MutableStateFlow<List<Reservation>>(emptyList())
    val pendingQueueBookings: StateFlow<List<Reservation>> = _pendingQueueBookings.asStateFlow()

    val feedReservations: StateFlow<List<Reservation>> = combine(
        _selectedFeedTab,
        _todayActiveBookings,
        _pendingQueueBookings
    ) { tab, todayActive, pendingQueue ->
        when (tab) {
            DashboardFeedTab.TODAY_ACTIVE -> todayActive
            DashboardFeedTab.PENDING_QUEUE -> pendingQueue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        loadMetrics(forceRefresh = false)
        observeFeeds()
        observeTransferSyncEvents()
    }

    /**
     * Observes reactive transfer finalization events across the application to trigger
     * cache recalculation and metric refresh (FR-M4-07.4).
     */
    private fun observeTransferSyncEvents() {
        viewModelScope.launch {
            TransferSyncNotifier.events.collect {
                loadMetrics(forceRefresh = true)
            }
        }
    }

    /**
     * Initiates real-time observation of today's active slots and pending reservation queues.
     */
    private fun observeFeeds() {
        viewModelScope.launch {
            repository.getTodayActiveReservationsStream().collect { reservations ->
                _todayActiveBookings.value = reservations
            }
        }
        viewModelScope.launch {
            repository.getPendingQueueReservationsStream().collect { reservations ->
                _pendingQueueBookings.value = reservations
            }
        }
    }

    /**
     * Changes the active feed tab between today's active slots and the pending queue.
     *
     * @param tab Selected operational feed tab.
     */
    fun selectFeedTab(tab: DashboardFeedTab) {
        _selectedFeedTab.value = tab
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

            repository.getDashboardMetricsStream(forceRefresh = forceRefresh, operatorId = operatorId).collect { result ->
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
                    repository.syncRemoteReservations(operatorId = operatorId)
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
     * Approves a pending reservation and updates the operational dashboard state.
     *
     * @param reservationId Identifier of the reservation to approve.
     * @param onComplete Callback invoked with result success or error message.
     */
    fun approveReservation(reservationId: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = repository.approveReservation(reservationId, operatorId)) {
                is NetworkResult.Success -> {
                    refresh()
                    onComplete?.invoke(true, null)
                }
                is NetworkResult.Error -> {
                    refresh()
                    onComplete?.invoke(false, result.message)
                }
                is NetworkResult.Exception -> {
                    refresh()
                    onComplete?.invoke(false, result.throwable.localizedMessage ?: "Failed to approve reservation.")
                }
            }
        }
    }

    /**
     * Rejects a pending reservation and updates the operational dashboard state.
     *
     * @param reservationId Identifier of the reservation to reject.
     * @param reason Optional rejection reason.
     * @param onComplete Callback invoked with result success or error message.
     */
    fun rejectReservation(reservationId: String, reason: String? = null, onComplete: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = repository.rejectReservation(reservationId, reason, operatorId)) {
                is NetworkResult.Success -> {
                    refresh()
                    onComplete?.invoke(true, null)
                }
                is NetworkResult.Error -> {
                    refresh()
                    onComplete?.invoke(false, result.message)
                }
                is NetworkResult.Exception -> {
                    refresh()
                    onComplete?.invoke(false, result.throwable.localizedMessage ?: "Failed to reject reservation.")
                }
            }
        }
    }

    /**
     * Factory for constructing DashboardViewModel instances with injected IDashboardRepository.
     */
    class Factory(
        private val repository: IDashboardRepository,
        private val operatorId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(repository, operatorId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
