/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for displaying and filtering booking history with offline-first Room Flow.
 */

package com.sliit.ssmts.reservation_workflow.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookingHistoryViewModel(
    private val repository: IReservationRepository
) : ViewModel() {

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var allReservations: List<Reservation> = emptyList()
    private var currentFilter: String = "All"

    fun loadHistory(prosumerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Sync from network to Room cache in background
            launch {
                repository.syncReservations(prosumerId)
                _isLoading.value = false
            }
            // Stream continuous updates from Room DB Flow
            repository.getMyReservations(prosumerId).collect { list ->
                allReservations = list
                applyFilter(currentFilter)
                _isLoading.value = false
            }
        }
    }

    fun filterByStatus(statusName: String) {
        currentFilter = statusName
        applyFilter(statusName)
    }

    private fun applyFilter(statusName: String) {
        if (statusName.equals("All", ignoreCase = true)) {
            _reservations.value = allReservations
        } else {
            _reservations.value = allReservations.filter { 
                it.status.name.equals(statusName, ignoreCase = true) 
            }
        }
    }

    fun searchByStation(query: String) {
        if (query.isBlank()) {
            applyFilter(currentFilter)
        } else {
            _reservations.value = allReservations.filter { 
                it.stationId.contains(query, ignoreCase = true) 
            }
        }
    }
}
