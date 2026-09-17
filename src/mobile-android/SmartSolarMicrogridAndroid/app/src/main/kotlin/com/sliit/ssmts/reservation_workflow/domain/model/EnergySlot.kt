/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Pure Kotlin domain model for an energy slot.
 */

package com.sliit.ssmts.reservation_workflow.domain.model

data class EnergySlot(
    val id: String,
    val stationId: String,
    val date: String,
    val timeRange: String, // e.g. "08:00 - 09:00"
    val batterySlotId: String,
    val isAvailable: Boolean
)
