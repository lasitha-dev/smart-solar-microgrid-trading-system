/**
 * Description: Pure domain model representing operational dashboard metric counters and active booking spotlight widget data.
 */
package com.sliit.ssmts.operator_dashboard.domain.model

/**
 * Description: Holds live aggregated operational counters and spotlight slot data for the Grid Operator console.
 */
data class DashboardMetrics(
    val pendingReservationsCount: Int = 0,
    val approvedFutureReservationsCount: Int = 0,
    val completedTodayCount: Int = 0,
    val activeSpotlight: ActiveSpotlightReservation? = null,
    val isOfflineCached: Boolean = false,
    val lastSyncedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Description: Detailed domain representation of the nearest upcoming approved reservation spotlight.
 */
data class ActiveSpotlightReservation(
    val reservationId: String,
    val stationName: String,
    val allocatedBayId: String,
    val scheduledDateTimeIso: String,
    val scheduledTimeMillis: Long,
    val status: ReservationStatus = ReservationStatus.APPROVED,
    val estimatedKwh: Double = 0.0
)
