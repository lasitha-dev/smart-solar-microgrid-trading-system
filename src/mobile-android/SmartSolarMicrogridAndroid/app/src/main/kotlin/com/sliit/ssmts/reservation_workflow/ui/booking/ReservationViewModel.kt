/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for creating a new reservation, handling business rules client-side.
 */

package com.sliit.ssmts.reservation_workflow.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.reservation_workflow.domain.model.EnergySlot
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.util.DateRuleValidator
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ReservationViewModel(
    private val repository: IReservationRepository
) : ViewModel() {

    private val _createState = MutableStateFlow<UiState<Reservation>>(UiState.Idle)
    val createState: StateFlow<UiState<Reservation>> = _createState.asStateFlow()

    private val _slotsState = MutableStateFlow<UiState<List<EnergySlot>>>(UiState.Idle)
    val slotsState: StateFlow<UiState<List<EnergySlot>>> = _slotsState.asStateFlow()

    fun loadSlots(stationId: String, date: Date) {
        viewModelScope.launch {
            _slotsState.value = UiState.Loading
            when (val result = repository.getAvailableSlots(stationId, date)) {
                is NetworkResult.Success -> {
                    _slotsState.value = UiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _slotsState.value = UiState.Error(result.message, result.code)
                }
                is NetworkResult.Exception -> {
                    _slotsState.value = UiState.Error(result.e.message ?: "An unexpected error occurred")
                }
            }
        }
    }

    fun createReservation(prosumerId: String, stationId: String, slotId: String, scheduledTime: Date) {
        viewModelScope.launch {
            _createState.value = UiState.Loading
            
            // Client-side validation of the 7-day advance booking rule
            if (!DateRuleValidator.isWithin7Days(scheduledTime)) {
                _createState.value = UiState.Error(
                    message = "Reservation date must be within 7 days from today.",
                    code = "RESERVATION_WINDOW_INVALID"
                )
                return@launch
            }

            // Perform network request
            when (val result = repository.createReservation(prosumerId, stationId, slotId, scheduledTime)) {
                is NetworkResult.Success -> {
                    _createState.value = UiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _createState.value = UiState.Error(result.message, result.code)
                }
                is NetworkResult.Exception -> {
                    _createState.value = UiState.Error(result.e.message ?: "An unexpected error occurred")
                }
            }
        }
    }
    
    fun resetState() {
        _createState.value = UiState.Idle
    }
}
