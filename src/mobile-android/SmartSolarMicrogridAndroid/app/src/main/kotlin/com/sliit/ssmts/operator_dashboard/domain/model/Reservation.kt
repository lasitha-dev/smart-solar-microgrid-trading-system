/**
 * Description: Pure domain reservation model and status enumeration decoupled from database entities and DTOs.
 */
package com.sliit.ssmts.operator_dashboard.domain.model

import java.util.Locale

/**
 * Description: Strong type-safe representation of all possible lifecycle states of an energy trading reservation.
 */
enum class ReservationStatus(val value: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        /**
         * Safely parses a status string into a ReservationStatus enum constant, defaulting to PENDING if unknown.
         */
        fun fromString(status: String?): ReservationStatus {
            if (status.isNullOrBlank()) return PENDING
            return entries.firstOrNull {
                it.value.equals(status.trim(), ignoreCase = true) ||
                it.name.equals(status.trim(), ignoreCase = true)
            } ?: PENDING
        }
    }
}

/**
 * Description: Pure domain entity representing an energy-slot trading reservation.
 */
data class Reservation(
    val id: String,
    val prosumerNic: String,
    val stationName: String,
    val scheduledTimeMillis: Long,
    val allocatedBay: String,
    val estimatedKwh: Double,
    val meteredKwh: Double? = null,
    val status: ReservationStatus,
    val qrPayload: String? = null,
    val lastSyncedAtMillis: Long = System.currentTimeMillis()
) {
    /**
     * True if reservation has already undergone physical energy transfer finalization.
     */
    val isCompleted: Boolean
        get() = status == ReservationStatus.COMPLETED

    /**
     * Returns true if reservation is approved and waiting for physical arrival.
     */
    val isApproved: Boolean
        get() = status == ReservationStatus.APPROVED
}
