/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for API request to create a reservation.
 */

package com.sliit.ssmts.reservation_workflow.data.remote.dto

data class CreateReservationRequestDto(
    val prosumerId: String,
    val stationId: String,
    val bookingSlotId: String,
    val scheduledDateTime: String
)
