/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for displaying and filtering booking history.
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

    private var allReservations: List<Reservation> = emptyList()

    fun loadHistory(prosumerId: String) {
        viewModelScope.launch {
            repository.getMyReservations(prosumerId).collect { list ->
                allReservations = list
                _reservations.value = list
            }
        }
    }

    fun filterByStatus(statusName: String) {
        if (statusName == "All") {
            _reservations.value = allReservations
        } else {
            _reservations.value = allReservations.filter { it.status.name == statusName.uppercase() }
        }
    }

    fun searchByStation(query: String) {
        if (query.isBlank()) {
            _reservations.value = allReservations
        } else {
            _reservations.value = allReservations.filter { 
                it.stationId.contains(query, ignoreCase = true) 
            }
        }
    }
}
