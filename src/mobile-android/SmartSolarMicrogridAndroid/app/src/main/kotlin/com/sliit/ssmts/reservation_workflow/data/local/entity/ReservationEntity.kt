/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Room Database entity for caching energy reservations locally.
 */

package com.sliit.ssmts.reservation_workflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_reservations_cache")
data class ReservationEntity(
    @PrimaryKey val reservationId: String,
    val prosumerId: String,
    val stationId: String,
    val bookingSlotId: String,
    val scheduledDateTime: String, // ISO 8601 string for SQLite compatibility
    val status: String,
    val qrCode: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
