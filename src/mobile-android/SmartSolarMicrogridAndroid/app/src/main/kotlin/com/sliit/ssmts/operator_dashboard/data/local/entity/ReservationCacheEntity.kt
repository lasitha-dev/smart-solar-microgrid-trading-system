/**
 * Description: SQLite entity representation for tbl_reservations_cache enabling offline persistence,
 * caching, and local queries for the Member 4 Operational Dashboard subsystem.
 */
package com.sliit.ssmts.operator_dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus

/**
 * Room entity representing cached energy reservations persisted in tbl_reservations_cache.
 *
 * @property reservationId Primary key representing remote MongoDB identifier.
 * @property prosumerNic Prosumer National Identity Card identifier.
 * @property stationName Human-readable solar microgrid station name.
 * @property scheduledTime Epoch timestamp in milliseconds for the scheduled slot.
 * @property allocatedBay Physical bay identifier designated for the transfer.
 * @property status Current reservation status string.
 * @property qrPayload Cryptographically signed QR payload string (if available).
 * @property estimatedKwh Forecasted energy amount in kilowatt-hours.
 * @property meteredKwh Actual delivered energy in kWh (populated post-finalization).
 * @property lastSyncedAt Epoch timestamp in milliseconds when this record was cached.
 */
@Entity(tableName = "tbl_reservations_cache")
data class ReservationCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "reservation_id")
    val reservationId: String,

    @ColumnInfo(name = "prosumer_nic")
    val prosumerNic: String,

    @ColumnInfo(name = "station_name")
    val stationName: String,

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: Long,

    @ColumnInfo(name = "allocated_bay")
    val allocatedBay: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "qr_payload")
    val qrPayload: String? = null,

    @ColumnInfo(name = "estimated_kwh")
    val estimatedKwh: Double = 0.0,

    @ColumnInfo(name = "metered_kwh")
    val meteredKwh: Double? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    /**
     * Maps this SQLite cache entity to a clean domain Reservation model.
     *
     * @return Pure domain Reservation model instance.
     */
    fun toDomain(): Reservation {
        return Reservation(
            id = reservationId,
            prosumerNic = prosumerNic,
            stationName = stationName,
            scheduledTimeMillis = scheduledTime,
            allocatedBay = allocatedBay,
            estimatedKwh = estimatedKwh,
            meteredKwh = meteredKwh,
            status = ReservationStatus.fromString(status),
            qrPayload = qrPayload,
            lastSyncedAtMillis = lastSyncedAt
        )
    }
}

/**
 * Maps a domain Reservation model to an SQLite cache entity for persistence.
 *
 * @return Room entity instance ready for insertion into tbl_reservations_cache.
 */
fun Reservation.toEntity(): ReservationCacheEntity {
    return ReservationCacheEntity(
        reservationId = id,
        prosumerNic = prosumerNic,
        stationName = stationName,
        scheduledTime = scheduledTimeMillis,
        allocatedBay = allocatedBay,
        status = status.name,
        qrPayload = qrPayload,
        estimatedKwh = estimatedKwh,
        meteredKwh = meteredKwh,
        lastSyncedAt = lastSyncedAtMillis
    )
}
