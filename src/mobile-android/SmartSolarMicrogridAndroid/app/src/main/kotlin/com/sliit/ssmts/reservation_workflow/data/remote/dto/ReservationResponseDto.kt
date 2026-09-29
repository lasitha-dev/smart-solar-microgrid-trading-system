/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for receiving reservation details from API.
 */

package com.sliit.ssmts.reservation_workflow.data.remote.dto

data class ReservationResponseDto(
    val id: String,
    val prosumerId: String,
    val stationId: String,
    val bookingSlotId: String,
    val scheduledDateTime: String,
    val status: String,
    val qrCode: String?,
    val requestedAt: String,
    val updatedAt: String,
    val cancelledAt: String?,
    val cancelReason: String?,
    val finalizedBy: String?,
    val finalizedAt: String?
)
