/**
 * Description: Data Transfer Object representing individual reservation items across booking history and real-time feeds.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Description: Encapsulates detailed reservation fields returned by GET /api/reservations.
 */
data class ReservationItemDto(
    @SerializedName("reservationId")
    val reservationId: String,

    @SerializedName("prosumerNic")
    val prosumerNic: String,

    @SerializedName("stationName")
    val stationName: String,

    @SerializedName("scheduledDateTime")
    val scheduledDateTime: String,

    @SerializedName("allocatedBayId")
    val allocatedBayId: String,

    @SerializedName("estimatedKwh")
    val estimatedKwh: Double,

    @SerializedName("meteredEnergyKwh")
    val meteredEnergyKwh: Double? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("qrCode")
    val qrCode: String? = null
)
