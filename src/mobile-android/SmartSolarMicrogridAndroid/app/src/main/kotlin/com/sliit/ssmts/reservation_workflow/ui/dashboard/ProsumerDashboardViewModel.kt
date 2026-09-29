/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for the Prosumer Dashboard, counting reservations by status.
 */

package com.sliit.ssmts.reservation_workflow.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProsumerDashboardViewModel(
    private val repository: IReservationRepository
) : ViewModel() {

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _approvedCount = MutableStateFlow(0)
    val approvedCount: StateFlow<Int> = _approvedCount.asStateFlow()

    fun loadCounts(prosumerId: String) {
        viewModelScope.launch {
            repository.getMyReservations(prosumerId).collect { reservations ->
                _pendingCount.value = reservations.count { it.status.name == "PENDING" }
                _approvedCount.value = reservations.count { it.status.name == "APPROVED" }
            }
        }
    }
}
