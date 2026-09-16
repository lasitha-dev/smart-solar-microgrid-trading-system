/**
 * Description: Room Data Access Object (DAO) for tbl_operator_audit_cache tracking local verification
 * and finalization audit records for offline retry and audit logs.
 */
package com.sliit.ssmts.operator_dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity

/**
 * Data access object for interacting with tbl_operator_audit_cache.
 */
@Dao
interface OperatorAuditDao {

    /**
     * Inserts an operator verification audit record.
     *
     * @param audit Entity representing the local audit transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: OperatorAuditEntity)

    /**
     * Retrieves all audit records that failed online sync and require retry.
     *
     * @return List of audit entities with status 'PENDING_RETRY'.
     */
    @Query("SELECT * FROM tbl_operator_audit_cache WHERE sync_status = 'PENDING_RETRY' ORDER BY timestamp ASC")
    suspend fun getPendingRetries(): List<OperatorAuditEntity>

    /**
     * Updates the synchronization status of an audit record.
     *
     * @param verificationId Audit UUID.
     * @param status Updated status string ('SYNCED' or 'PENDING_RETRY').
     * @return Number of rows updated.
     */
    @Query("UPDATE tbl_operator_audit_cache SET sync_status = :status WHERE verification_id = :verificationId")
    suspend fun updateSyncStatus(verificationId: String, status: String): Int

    /**
     * Retrieves all recorded audit transactions ordered by time descending.
     *
     * @return List of all operator audit records.
     */
    @Query("SELECT * FROM tbl_operator_audit_cache ORDER BY timestamp DESC")
    suspend fun getAllAudits(): List<OperatorAuditEntity>
}
