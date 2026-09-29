/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for updating and canceling reservations, handling 12-hour business rule.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class ManageReservationViewModel(
    private val repository: IReservationRepository
) : ViewModel() {

    private val _manageState = MutableStateFlow<UiState<Reservation>>(UiState.Idle)
    val manageState: StateFlow<UiState<Reservation>> = _manageState.asStateFlow()

    private val _cancelState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val cancelState: StateFlow<UiState<Unit>> = _cancelState.asStateFlow()

    fun updateReservation(currentReservation: Reservation, newSlotId: String, newTime: Date) {
        viewModelScope.launch {
            _manageState.value = UiState.Loading
            
            // Client-side validation of the 12-hour rule
            if (!DateRuleValidator.has12HoursRemaining(currentReservation.scheduledDateTime)) {
                _manageState.value = UiState.Error(
                    message = "Cannot modify reservation within 12 hours of scheduled time.",
                    code = "MODIFICATION_WINDOW_CLOSED"
                )
                return@launch
            }

            // Client-side validation of the 7-day rule for new time
            if (!DateRuleValidator.isWithin7Days(newTime)) {
                _manageState.value = UiState.Error(
                    message = "New date must be within 7 days from today.",
                    code = "RESERVATION_WINDOW_INVALID"
                )
                return@launch
            }

            when (val result = repository.updateReservation(currentReservation.id, newSlotId, newTime)) {
                is NetworkResult.Success -> {
                    _manageState.value = UiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _manageState.value = UiState.Error(result.message, result.code)
                }
                is NetworkResult.Exception -> {
                    _manageState.value = UiState.Error(result.e.message ?: "An unexpected error occurred")
                }
            }
        }
    }

    fun cancelReservation(currentReservation: Reservation, reason: String? = null) {
        viewModelScope.launch {
            _cancelState.value = UiState.Loading
            
            // Client-side validation of the 12-hour rule
            if (!DateRuleValidator.has12HoursRemaining(currentReservation.scheduledDateTime)) {
                _cancelState.value = UiState.Error(
                    message = "Cannot cancel reservation within 12 hours of scheduled time.",
                    code = "MODIFICATION_WINDOW_CLOSED"
                )
                return@launch
            }

            when (val result = repository.cancelReservation(currentReservation.id, reason)) {
                is NetworkResult.Success -> {
                    _cancelState.value = UiState.Success(Unit)
                }
                is NetworkResult.Error -> {
                    _cancelState.value = UiState.Error(result.message, result.code)
                }
                is NetworkResult.Exception -> {
                    _cancelState.value = UiState.Error(result.e.message ?: "An unexpected error occurred")
                }
            }
        }
    }
    
    fun resetState() {
        _manageState.value = UiState.Idle
        _cancelState.value = UiState.Idle
    }
}
