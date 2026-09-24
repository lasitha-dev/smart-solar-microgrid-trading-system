/**
 * Description: Data Transfer Objects for operator QR scanning verification request and response contracts.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Description: Request payload dispatched to POST /api/reservations/verify-qr containing the raw scanned QR string.
 */
data class QrVerificationRequestDto(
    @SerializedName("qrPayload")
    val qrPayload: String
)

/**
 * Description: Validation response returned from POST /api/reservations/verify-qr with reservation details or rejection reason.
 */
data class QrVerificationResponseDto(
    @SerializedName("valid")
    val valid: Boolean,

    @SerializedName("reservationId")
    val reservationId: String? = null,

    @SerializedName("prosumerNic")
    val prosumerNic: String? = null,

    @SerializedName("stationName")
    val stationName: String? = null,

    @SerializedName("allocatedBayId")
    val allocatedBayId: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("errorCode")
    val errorCode: String? = null
)
