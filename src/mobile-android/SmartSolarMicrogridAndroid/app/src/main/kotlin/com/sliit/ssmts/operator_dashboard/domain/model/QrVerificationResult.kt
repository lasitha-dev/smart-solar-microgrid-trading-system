/**
 * Description: Domain models encapsulating QR server verification handshake outcomes and energy transfer finalization results.
 */
package com.sliit.ssmts.operator_dashboard.domain.model

/**
 * Description: Domain model representing the server verification outcome of a scanned QR token.
 */
data class QrVerificationResult(
    val isValid: Boolean,
    val reservationId: String? = null,
    val prosumerNic: String? = null,
    val stationName: String? = null,
    val allocatedBayId: String? = null,
    val status: ReservationStatus? = null,
    val message: String? = null,
    val errorCode: String? = null
)

/**
 * Description: Domain model confirming completed energy transfer transaction metrics.
 */
data class FinalizeTransferResult(
    val isSuccess: Boolean,
    val reservationId: String,
    val status: ReservationStatus = ReservationStatus.COMPLETED,
    val meteredEnergyKwh: Double,
    val finalizedAtIso: String,
    val finalizedByOperator: String
)
