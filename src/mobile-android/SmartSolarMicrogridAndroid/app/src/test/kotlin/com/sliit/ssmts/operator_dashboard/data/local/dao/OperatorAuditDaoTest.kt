/**
 * Description: Unit test suite for OperatorAuditDao verifying Room SQLite in-memory operations,
 * offline audit record insertion, sync status transitions, and pending retry queries.
 */
package com.sliit.ssmts.operator_dashboard.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit test class verifying Room SQLite interactions on tbl_operator_audit_cache.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OperatorAuditDaoTest {

    private lateinit var database: SsmtsDatabase
    private lateinit var auditDao: OperatorAuditDao

    /**
     * Initializes an in-memory SQLite database before each test execution.
     */
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SsmtsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auditDao = database.operatorAuditDao()
    }

    /**
     * Closes the in-memory database after each test execution.
     */
    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    /**
     * Verifies that inserting an audit record stores it accurately in tbl_operator_audit_cache.
     */
    @Test
    fun insertAudit_storesRecordAccurately() = runBlocking {
        val audit = OperatorAuditEntity(
            verificationId = "AUDIT-UUID-001",
            reservationId = "664fa10b9c3e2e1a4f001201",
            operatorId = "OP-PERADENIYA-01",
            meteredKwh = 24.65,
            timestamp = 1726659900000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )

        auditDao.insertAudit(audit)

        val all = auditDao.getAllAudits()
        assertEquals(1, all.size)
        assertEquals("AUDIT-UUID-001", all[0].verificationId)
        assertEquals("664fa10b9c3e2e1a4f001201", all[0].reservationId)
        assertEquals("OP-PERADENIYA-01", all[0].operatorId)
        assertEquals(24.65, all[0].meteredKwh, 0.001)
        assertEquals(1726659900000L, all[0].timestamp)
        assertEquals(OperatorAuditEntity.STATUS_SYNCED, all[0].syncStatus)
    }

    /**
     * Verifies that getPendingRetries returns only records with sync_status = PENDING_RETRY
     * ordered chronologically ascending by timestamp.
     */
    @Test
    fun getPendingRetries_returnsOnlyPendingRetryStatusChronological() = runBlocking {
        val synced = OperatorAuditEntity(
            verificationId = "AUDIT-SYNCED",
            reservationId = "RES-1",
            operatorId = "OP-1",
            meteredKwh = 10.0,
            timestamp = 1000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )
        val pendingRetryLater = OperatorAuditEntity(
            verificationId = "AUDIT-RETRY-2",
            reservationId = "RES-3",
            operatorId = "OP-1",
            meteredKwh = 30.0,
            timestamp = 3000L,
            syncStatus = OperatorAuditEntity.STATUS_PENDING_RETRY
        )
        val pendingRetryEarlier = OperatorAuditEntity(
            verificationId = "AUDIT-RETRY-1",
            reservationId = "RES-2",
            operatorId = "OP-1",
            meteredKwh = 20.0,
            timestamp = 2000L,
            syncStatus = OperatorAuditEntity.STATUS_PENDING_RETRY
        )

        auditDao.insertAudit(synced)
        auditDao.insertAudit(pendingRetryLater)
        auditDao.insertAudit(pendingRetryEarlier)

        val pending = auditDao.getPendingRetries()
        assertEquals(2, pending.size)
        assertEquals("AUDIT-RETRY-1", pending[0].verificationId)
        assertEquals("AUDIT-RETRY-2", pending[1].verificationId)
        assertTrue(pending.all { it.syncStatus == OperatorAuditEntity.STATUS_PENDING_RETRY })
    }

    /**
     * Verifies that updateSyncStatus transitions an audit record from PENDING_RETRY to SYNCED.
     */
    @Test
    fun updateSyncStatus_transitionsFromPendingRetryToSynced() = runBlocking {
        val pending = OperatorAuditEntity(
            verificationId = "AUDIT-RETRY-TARGET",
            reservationId = "RES-SYNC-1",
            operatorId = "OP-COLOMBO",
            meteredKwh = 15.5,
            timestamp = 5000L,
            syncStatus = OperatorAuditEntity.STATUS_PENDING_RETRY
        )
        auditDao.insertAudit(pending)

        assertEquals(1, auditDao.getPendingRetries().size)

        val updatedRows = auditDao.updateSyncStatus("AUDIT-RETRY-TARGET", OperatorAuditEntity.STATUS_SYNCED)
        assertEquals(1, updatedRows)

        val remainingRetries = auditDao.getPendingRetries()
        assertEquals(0, remainingRetries.size)

        val all = auditDao.getAllAudits()
        assertEquals(1, all.size)
        assertEquals(OperatorAuditEntity.STATUS_SYNCED, all[0].syncStatus)
    }

    /**
     * Verifies updateSyncStatus on a non-existent verification ID returns 0 rows updated.
     */
    @Test
    fun updateSyncStatus_nonExistentId_returnsZero() = runBlocking {
        val updatedRows = auditDao.updateSyncStatus("DOES-NOT-EXIST", OperatorAuditEntity.STATUS_SYNCED)
        assertEquals(0, updatedRows)
    }

    /**
     * Verifies that getAllAudits returns all recorded audit transactions ordered newest first (timestamp DESC).
     */
    @Test
    fun getAllAudits_ordersByTimestampDesc() = runBlocking {
        val auditOld = OperatorAuditEntity(
            verificationId = "A-OLD",
            reservationId = "RES-OLD",
            operatorId = "OP-1",
            meteredKwh = 10.0,
            timestamp = 1000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )
        val auditNewest = OperatorAuditEntity(
            verificationId = "A-NEWEST",
            reservationId = "RES-NEW",
            operatorId = "OP-1",
            meteredKwh = 20.0,
            timestamp = 5000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )
        val auditMid = OperatorAuditEntity(
            verificationId = "A-MID",
            reservationId = "RES-MID",
            operatorId = "OP-1",
            meteredKwh = 15.0,
            timestamp = 3000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )

        auditDao.insertAudit(auditOld)
        auditDao.insertAudit(auditNewest)
        auditDao.insertAudit(auditMid)

        val results = auditDao.getAllAudits()
        assertEquals(3, results.size)
        assertEquals("A-NEWEST", results[0].verificationId)
        assertEquals("A-MID", results[1].verificationId)
        assertEquals("A-OLD", results[2].verificationId)
    }

    /**
     * Verifies that re-inserting an audit record with an existing verification ID replaces it (OnConflictStrategy.REPLACE).
     */
    @Test
    fun insertAudit_duplicateVerificationId_replacesExistingRecord() = runBlocking {
        val original = OperatorAuditEntity(
            verificationId = "AUDIT-DUP-ID",
            reservationId = "RES-ORIGINAL",
            operatorId = "OP-ORIGINAL",
            meteredKwh = 12.0,
            timestamp = 1000L,
            syncStatus = OperatorAuditEntity.STATUS_PENDING_RETRY
        )
        auditDao.insertAudit(original)

        val replacement = OperatorAuditEntity(
            verificationId = "AUDIT-DUP-ID",
            reservationId = "RES-REPLACED",
            operatorId = "OP-REPLACED",
            meteredKwh = 18.5,
            timestamp = 2000L,
            syncStatus = OperatorAuditEntity.STATUS_SYNCED
        )
        auditDao.insertAudit(replacement)

        val all = auditDao.getAllAudits()
        assertEquals(1, all.size)
        assertEquals("RES-REPLACED", all[0].reservationId)
        assertEquals("OP-REPLACED", all[0].operatorId)
        assertEquals(18.5, all[0].meteredKwh, 0.001)
        assertEquals(OperatorAuditEntity.STATUS_SYNCED, all[0].syncStatus)
    }
}
