/**
 * Description: SQLite entity representation for tbl_operator_audit_cache tracking local
 * verification and finalization actions performed by grid operators.
 */
package com.sliit.ssmts.operator_dashboard.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing operator audit records persisted in tbl_operator_audit_cache.
 *
 * @property verificationId Unique local audit UUID.
 * @property reservationId Remote identifier of the verified energy reservation.
 * @property operatorId Unique identifier of the authenticated grid operator.
 * @property meteredKwh Actual physical metered energy in kilowatt-hours recorded.
 * @property timestamp Epoch timestamp in milliseconds when verification occurred.
 * @property syncStatus Synchronization status: 'SYNCED' or 'PENDING_RETRY'.
 */
@Entity(tableName = "tbl_operator_audit_cache")
data class OperatorAuditEntity(
    @PrimaryKey
    @ColumnInfo(name = "verification_id")
    val verificationId: String,

    @ColumnInfo(name = "reservation_id")
    val reservationId: String,

    @ColumnInfo(name = "operator_id")
    val operatorId: String,

    @ColumnInfo(name = "metered_kwh")
    val meteredKwh: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String
) {
    companion object {
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_PENDING_RETRY = "PENDING_RETRY"
    }
}
