/**
 * Description: ViewModel managing the Grid Operator QR code verification handshake, state
 * transitions, and delegation to central Web API endpoints (Rule 3, DIP & FR-M4-06).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.operator_dashboard.domain.repository.IOperatorVerificationRepository
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State holder and action handler for QR verification handshake in the Grid Operator scanner.
 *
 * @property repository Domain repository handling cryptographic verification with central API.
 */
class OperatorScannerViewModel(
    private val repository: IOperatorVerificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Dispatches the decoded or simulated QR payload to the central C# Web API for verification.
     *
     * @param qrPayload Raw delimited QR payload string.
     */
    fun verifyQrToken(qrPayload: String) {
        if (_uiState.value is ScannerUiState.Verifying) {
            return
        }

        _uiState.value = ScannerUiState.Verifying

        viewModelScope.launch {
            when (val result = repository.verifyScannedQr(qrPayload)) {
                is NetworkResult.Success -> {
                    val verification = result.data
                    if (verification.isValid) {
                        _uiState.value = ScannerUiState.Handshake(verification)
                    } else {
                        _uiState.value = ScannerUiState.Rejection(
                            errorCode = verification.errorCode,
                            message = verification.message ?: "Verification failed."
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = ScannerUiState.Rejection(
                        errorCode = result.code,
                        message = result.message
                    )
                }
                is NetworkResult.Exception -> {
                    _uiState.value = ScannerUiState.Rejection(
                        errorCode = "ERR_NETWORK_EXCEPTION",
                        message = result.throwable.localizedMessage ?: "Network connection error."
                    )
                }
            }
        }
    }

    /**
     * Finalizes the energy transfer by committing metered power metrics to the central system.
     *
     * @param reservationId The verified reservation ID.
     * @param meteredKwh Actual delivered energy in kWh (0.01 - 999.99 kWh).
     * @param notes Optional operator observations.
     */
    fun finalizeEnergyTransfer(
        reservationId: String,
        meteredKwh: Double,
        notes: String? = null
    ) {
        if (_uiState.value is ScannerUiState.Finalizing) {
            return
        }

        if (meteredKwh.isNaN() || meteredKwh < 0.01 || meteredKwh > 999.99) {
            _uiState.value = ScannerUiState.Rejection(
                errorCode = "ERR_INVALID_METERED_KWH",
                message = "Metered energy reading must be a positive decimal between 0.01 and 999.99 kWh."
            )
            return
        }

        _uiState.value = ScannerUiState.Finalizing

        viewModelScope.launch {
            when (val result = repository.finalizeTransfer(reservationId, meteredKwh, notes)) {
                is NetworkResult.Success -> {
                    _uiState.value = ScannerUiState.Finalized(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = ScannerUiState.Rejection(
                        errorCode = result.code,
                        message = result.message
                    )
                }
                is NetworkResult.Exception -> {
                    _uiState.value = ScannerUiState.Rejection(
                        errorCode = "ERR_FINALIZE_EXCEPTION",
                        message = result.throwable.localizedMessage ?: "Failed to finalize transfer."
                    )
                }
            }
        }
    }

    /**
     * Resets scanner UI state to Idle, dismissing dialogs and reactivating camera viewfinder.
     */
    fun resetScannerState() {
        _uiState.value = ScannerUiState.Idle
    }

    /**
     * Factory for constructing OperatorScannerViewModel with repository injection.
     */
    class Factory(
        private val repository: IOperatorVerificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OperatorScannerViewModel::class.java)) {
                return OperatorScannerViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
