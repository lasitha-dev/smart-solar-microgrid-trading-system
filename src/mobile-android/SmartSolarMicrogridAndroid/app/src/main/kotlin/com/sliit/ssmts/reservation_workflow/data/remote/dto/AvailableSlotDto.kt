/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for receiving available slot details from API.
 */

package com.sliit.ssmts.reservation_workflow.data.remote.dto

data class AvailableSlotDto(
    val id: String,
    val stationId: String,
    val slotDate: String,
    val startTime: String,
    val endTime: String,
    val batterySlotId: String,
    val status: String
)
