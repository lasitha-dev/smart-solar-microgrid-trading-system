/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Domain enum representing reservation state transitions.
 */

package com.sliit.ssmts.reservation_workflow.domain.model

enum class ReservationStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    COMPLETED,
    UNKNOWN;

    companion object {
        fun fromString(status: String): ReservationStatus {
            return try {
                valueOf(status.uppercase())
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}
