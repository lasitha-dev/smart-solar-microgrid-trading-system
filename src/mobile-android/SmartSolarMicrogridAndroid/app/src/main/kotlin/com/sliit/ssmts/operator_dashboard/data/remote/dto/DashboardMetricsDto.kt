/**
 * Description: Data Transfer Objects representing operational dashboard metrics and active booking spotlight responses.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Description: Encapsulates live count metrics and spotlight booking returned by GET /api/reservations/dashboard-metrics.
 */
data class DashboardMetricsDto(
    @SerializedName("pendingReservationsCount")
    val pendingReservationsCount: Int,

    @SerializedName("approvedFutureReservationsCount")
    val approvedFutureReservationsCount: Int,

    @SerializedName("completedTodayCount")
    val completedTodayCount: Int,

    @SerializedName("activeSpotlight")
    val activeSpotlight: ActiveSpotlightDto? = null
)

/**
 * Description: Encapsulates the nearest upcoming approved reservation details for the active spotlight card.
 */
data class ActiveSpotlightDto(
    @SerializedName("reservationId")
    val reservationId: String,

    @SerializedName("stationName")
    val stationName: String,

    @SerializedName("allocatedBayId")
    val allocatedBayId: String,

    @SerializedName("scheduledDateTime")
    val scheduledDateTime: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("estimatedKwh")
    val estimatedKwh: Double
)
