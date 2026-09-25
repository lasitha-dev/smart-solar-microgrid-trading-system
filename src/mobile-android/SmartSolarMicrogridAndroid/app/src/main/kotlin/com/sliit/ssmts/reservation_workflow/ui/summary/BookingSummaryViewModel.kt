/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: ViewModel for displaying the summary/receipt of a reservation.
 */

package com.sliit.ssmts.reservation_workflow.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookingSummaryViewModel(
    private val repository: IReservationRepository
) : ViewModel() {

    private val _summaryState = MutableStateFlow<UiState<Reservation>>(UiState.Idle)
    val summaryState: StateFlow<UiState<Reservation>> = _summaryState.asStateFlow()

    fun loadReservation(id: String) {
        viewModelScope.launch {
            _summaryState.value = UiState.Loading
            
            when (val result = repository.getReservation(id)) {
                is NetworkResult.Success -> {
                    _summaryState.value = UiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _summaryState.value = UiState.Error(result.message, result.code)
                }
                is NetworkResult.Exception -> {
                    _summaryState.value = UiState.Error(result.e.message ?: "Failed to load summary")
                }
            }
        }
    }
}
