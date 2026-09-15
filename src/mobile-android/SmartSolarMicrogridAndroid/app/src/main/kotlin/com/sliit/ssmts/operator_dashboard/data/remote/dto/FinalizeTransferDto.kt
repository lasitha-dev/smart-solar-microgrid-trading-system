/**
 * Description: Data Transfer Objects for energy transfer finalization requests and completion receipt responses.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Description: Request payload dispatched to PATCH /api/reservations/{id}/finalize containing verified metered power.
 */
data class FinalizeTransferDto(
    @SerializedName("meteredEnergyKwh")
    val meteredEnergyKwh: Double,

    @SerializedName("notes")
    val notes: String? = null
)

/**
 * Description: Finalization response receipt confirming completed transaction status and committed metered kWh.
 */
data class FinalizeTransferResponseDto(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("reservationId")
    val reservationId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("meteredEnergyKwh")
    val meteredEnergyKwh: Double,

    @SerializedName("finalizedAt")
    val finalizedAt: String,

    @SerializedName("finalizedByOperator")
    val finalizedByOperator: String
)
