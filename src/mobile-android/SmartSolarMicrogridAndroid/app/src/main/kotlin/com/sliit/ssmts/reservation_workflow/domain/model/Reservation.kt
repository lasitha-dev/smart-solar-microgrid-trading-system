/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Pure Kotlin domain model for an energy reservation.
 */

package com.sliit.ssmts.reservation_workflow.domain.model

import java.util.Date

data class Reservation(
    val id: String,
    val prosumerId: String,
    val stationId: String,
    val bookingSlotId: String,
    val scheduledDateTime: Date,
    val status: ReservationStatus,
    val qrCode: String?
)
