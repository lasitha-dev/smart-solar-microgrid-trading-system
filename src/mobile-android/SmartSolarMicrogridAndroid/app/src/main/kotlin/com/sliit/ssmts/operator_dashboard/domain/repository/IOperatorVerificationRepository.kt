/**
 * Description: Inverted repository interface defining operator QR verification handshake and energy transfer finalization actions.
 */
package com.sliit.ssmts.operator_dashboard.domain.repository

import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult
import com.sliit.ssmts.operator_dashboard.util.NetworkResult

/**
 * Description: Abstraction consumed by Operator ViewModels to execute cryptographic verification and commit metered power.
 */
interface IOperatorVerificationRepository {

    /**
     * Dispatches the scanned QR code token to the central C# Web API for signature verification.
     *
     * @param qrPayload The raw scanned token string from the device camera.
     * @return NetworkResult containing verified reservation details or rejection reason.
     */
    suspend fun verifyScannedQr(qrPayload: String): NetworkResult<QrVerificationResult>

    /**
     * Commits the actual delivered power reading to finalize the energy transfer transaction.
     *
     * @param reservationId The verified reservation ID.
     * @param meteredKwh The physical power measured at the solar station hub (0.01 - 999.99 kWh).
     * @param notes Optional operator operational observations.
     * @return NetworkResult confirming transfer finalization and receipt metrics.
     */
    suspend fun finalizeTransfer(
        reservationId: String,
        meteredKwh: Double,
        notes: String? = null
    ): NetworkResult<FinalizeTransferResult>
}
