/**
 * Description: Sealed state interface modeling reactive states for the Grid Operator QR scanner,
 * server verification handshake, and rejection dialogs (Rule 3 & FR-M4-06).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult

/**
 * Reactive state hierarchy governing the operator camera viewfinder, verification, and handshake modal.
 */
sealed interface ScannerUiState {

    /**
     * Default resting state: camera viewfinder active, framing guides visible, awaiting scan.
     */
    data object Idle : ScannerUiState

    /**
     * In-flight remote verification handshake with central C# Web API.
     */
    data object Verifying : ScannerUiState

    /**
     * Cryptographic verification succeeded on server: presents transfer handshake modal.
     *
     * @property reservation Verified reservation domain payload from central API.
     */
    data class Handshake(val reservation: QrVerificationResult) : ScannerUiState

    /**
     * Verification rejected by central API: presents explicit error explanation dialog.
     *
     * @property errorCode Structured rejection code (e.g., ERR_RESERVATION_ALREADY_COMPLETED).
     * @property message Human-readable rejection reason.
     */
    data class Rejection(val errorCode: String?, val message: String) : ScannerUiState
}
