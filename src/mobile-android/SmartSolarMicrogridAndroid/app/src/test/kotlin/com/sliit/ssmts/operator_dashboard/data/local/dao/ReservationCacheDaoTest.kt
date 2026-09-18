/**
 * Description: Unit test suite for ReservationCacheDao verifying in-memory SQLite operations,
 * chronological ordering, keyword search, metric aggregations, and finalization status updates.
 */
package com.sliit.ssmts.operator_dashboard.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit test class verifying Room SQLite interactions on tbl_reservations_cache.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReservationCacheDaoTest {

    private lateinit var database: SsmtsDatabase
    private lateinit var reservationCacheDao: ReservationCacheDao

    companion object {
        private const val SPEC_RESERVATION_ID = "664fa10b9c3e2e1a4f001201"
        private const val SPEC_PROSUMER_NIC = "200012345678"
        private const val SPEC_STATION_NAME = "Peradeniya Agro-Voltaic Hub"
        private const val SPEC_ALLOCATED_BAY = "BAY-02"
        private const val SPEC_METERED_KWH = 24.65
    }

    /**
     * Initializes an in-memory SQLite database before each test execution.
     */
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SsmtsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reservationCacheDao = database.reservationCacheDao()
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
     * Verifies that inserting reservations and observing them returns them sorted chronologically.
     */
    @Test
    fun insertAndRetrieve_ordersByScheduledTimeAsc() = runBlocking {
        val item1 = createTestEntity(id = "RES-001", scheduledTime = 3000L, status = "APPROVED")
        val item2 = createTestEntity(id = "RES-002", scheduledTime = 1000L, status = "PENDING")
        val item3 = createTestEntity(id = "RES-003", scheduledTime = 2000L, status = "COMPLETED")

        reservationCacheDao.upsertReservations(listOf(item1, item2, item3))

        val results = reservationCacheDao.getAllReservationsFlow().first()
        assertEquals(3, results.size)
        assertEquals("RES-002", results[0].reservationId)
        assertEquals("RES-003", results[1].reservationId)
        assertEquals("RES-001", results[2].reservationId)
    }

    /**
     * Verifies that filtering by status produces only matching records.
     */
    @Test
    fun getReservationsByStatusFlow_filtersCorrectly() = runBlocking {
        val pending = createTestEntity(id = "RES-P", status = "PENDING")
        val approved = createTestEntity(id = "RES-A", status = "APPROVED")

        reservationCacheDao.upsertReservations(listOf(pending, approved))

        val pendingResults = reservationCacheDao.getReservationsByStatusFlow("PENDING").first()
        assertEquals(1, pendingResults.size)
        assertEquals("RES-P", pendingResults[0].reservationId)

        val approvedResults = reservationCacheDao.getReservationsByStatusFlow("Approved").first()
        assertEquals(1, approvedResults.size)
        assertEquals("RES-A", approvedResults[0].reservationId)
    }

    /**
     * Verifies keyword searching by prosumer NIC, station name, or ID with optional status filtering.
     */
    @Test
    fun searchReservationsFlow_matchesKeywordAndStatus() = runBlocking {
        val res1 = createTestEntity(id = "RES-101", nic = "199512345678", station = "Solar Hub North", status = "PENDING")
        val res2 = createTestEntity(id = "RES-102", nic = "200098765432", station = "Solar Hub South", status = "APPROVED")
        val res3 = createTestEntity(id = "RES-103", nic = "199599999999", station = "Colombo Central", status = "APPROVED")

        reservationCacheDao.upsertReservations(listOf(res1, res2, res3))

        // Search by partial NIC with status filter
        val nicMatches = reservationCacheDao.searchReservationsFlow(status = "APPROVED", query = "1995").first()
        assertEquals(1, nicMatches.size)
        assertEquals("RES-103", nicMatches[0].reservationId)

        // Search by partial station name with no status filter
        val stationMatches = reservationCacheDao.searchReservationsFlow(status = null, query = "Solar Hub").first()
        assertEquals(2, stationMatches.size)

        // Search with blank query returns all matching status
        val allApproved = reservationCacheDao.searchReservationsFlow(status = "APPROVED", query = "").first()
        assertEquals(2, allApproved.size)
    }

    /**
     * Verifies Table 6 specification: updating finalized record with 24.65 kWh transitions status
     * from Approved to Completed in SQLite cache.
     */
    @Test
    fun updateFinalizationStatus_specReservation_transitionsApprovedToCompletedWith24_65Kwh() = runBlocking {
        val initial = createTestEntity(
            id = SPEC_RESERVATION_ID,
            nic = SPEC_PROSUMER_NIC,
            station = SPEC_STATION_NAME,
            bay = SPEC_ALLOCATED_BAY,
            status = "APPROVED",
            meteredKwh = null
        )
        reservationCacheDao.upsertReservation(initial)

        val syncTime = 1726659900000L
        val updatedRows = reservationCacheDao.updateFinalizationStatus(
            id = SPEC_RESERVATION_ID,
            status = "COMPLETED",
            meteredKwh = SPEC_METERED_KWH,
            lastSyncedAt = syncTime
        )
        assertEquals(1, updatedRows)

        val fetched = reservationCacheDao.getReservationById(SPEC_RESERVATION_ID)
        assertNotNull(fetched)
        assertEquals("COMPLETED", fetched?.status)
        assertEquals(SPEC_METERED_KWH, fetched?.meteredKwh ?: 0.0, 0.001)
        assertEquals(syncTime, fetched?.lastSyncedAt)
        assertEquals(SPEC_PROSUMER_NIC, fetched?.prosumerNic)
        assertEquals(SPEC_STATION_NAME, fetched?.stationName)
    }

    /**
     * Verifies that updateFinalizationStatus updates status to Completed and records metered kWh.
     */
    @Test
    fun updateFinalizationStatus_updatesStatusAndMeteredKwh() = runBlocking {
        val initial = createTestEntity(id = "RES-FIN", status = "APPROVED", meteredKwh = null)
        reservationCacheDao.upsertReservation(initial)

        val updatedRows = reservationCacheDao.updateFinalizationStatus(
            id = "RES-FIN",
            status = "COMPLETED",
            meteredKwh = 48.75,
            lastSyncedAt = 9999L
        )
        assertEquals(1, updatedRows)

        val fetched = reservationCacheDao.getReservationById("RES-FIN")
        assertNotNull(fetched)
        assertEquals("COMPLETED", fetched?.status)
        assertEquals(48.75, fetched?.meteredKwh ?: 0.0, 0.001)
        assertEquals(9999L, fetched?.lastSyncedAt)
    }

    /**
     * Verifies that re-inserting an entity with an existing ID replaces the record (OnConflictStrategy.REPLACE).
     */
    @Test
    fun upsertReservation_duplicateId_replacesExistingRecord() = runBlocking {
        val original = createTestEntity(id = "RES-DUP", bay = "BAY-01", status = "PENDING")
        reservationCacheDao.upsertReservation(original)

        val updated = createTestEntity(id = "RES-DUP", bay = "BAY-99", status = "APPROVED")
        reservationCacheDao.upsertReservation(updated)

        val fetched = reservationCacheDao.getReservationById("RES-DUP")
        assertNotNull(fetched)
        assertEquals("BAY-99", fetched?.allocatedBay)
        assertEquals("APPROVED", fetched?.status)

        // Verify table size is still 1
        val all = reservationCacheDao.getAllReservationsFlow().first()
        assertEquals(1, all.size)
    }

    /**
     * Verifies deleteById selectively removes the target reservation and leaves other records intact.
     */
    @Test
    fun deleteById_removesTargetReservationOnly() = runBlocking {
        val res1 = createTestEntity(id = "DEL-1")
        val res2 = createTestEntity(id = "DEL-2")
        reservationCacheDao.upsertReservations(listOf(res1, res2))

        val deletedRows = reservationCacheDao.deleteById("DEL-1")
        assertEquals(1, deletedRows)

        assertNull(reservationCacheDao.getReservationById("DEL-1"))
        assertNotNull(reservationCacheDao.getReservationById("DEL-2"))
    }

    /**
     * Verifies deleteById on a non-existent identifier returns zero rows removed.
     */
    @Test
    fun deleteById_nonExistentId_returnsZero() = runBlocking {
        val deletedRows = reservationCacheDao.deleteById("NON-EXISTENT-ID")
        assertEquals(0, deletedRows)
    }

    /**
     * Verifies getReservationById returns null when the reservation does not exist in SQLite.
     */
    @Test
    fun getReservationById_nonExistent_returnsNull() = runBlocking {
        val result = reservationCacheDao.getReservationById("DOES-NOT-EXIST")
        assertNull(result)
    }

    /**
     * Verifies updateFinalizationStatus returns zero rows modified when updating a non-existent ID.
     */
    @Test
    fun updateFinalizationStatus_nonExistent_returnsZero() = runBlocking {
        val updatedRows = reservationCacheDao.updateFinalizationStatus(
            id = "GHOST-ID",
            status = "COMPLETED",
            meteredKwh = 10.0,
            lastSyncedAt = 12345L
        )
        assertEquals(0, updatedRows)
    }

    /**
     * Verifies aggregation counts for pending, approved future, and completed today.
     */
    @Test
    fun countMetrics_aggregatesAccurately() = runBlocking {
        val now = 5000L
        val dayStart = 0L
        val dayEnd = 10000L

        val p1 = createTestEntity(id = "P1", status = "PENDING", scheduledTime = 1000L)
        val p2 = createTestEntity(id = "P2", status = "Pending", scheduledTime = 2000L)
        val aPast = createTestEntity(id = "AP", status = "APPROVED", scheduledTime = 4000L)
        val aFuture = createTestEntity(id = "AF", status = "APPROVED", scheduledTime = 6000L)
        val cToday = createTestEntity(id = "CT", status = "COMPLETED", scheduledTime = 3000L)
        val cOtherDay = createTestEntity(id = "CO", status = "COMPLETED", scheduledTime = 15000L)

        reservationCacheDao.upsertReservations(listOf(p1, p2, aPast, aFuture, cToday, cOtherDay))

        assertEquals(2, reservationCacheDao.getPendingCount())
        assertEquals(1, reservationCacheDao.getApprovedFutureCount(now))
        assertEquals(1, reservationCacheDao.getCompletedTodayCount(dayStart, dayEnd))
    }

    /**
     * Verifies spotlight selection returns the earliest upcoming approved reservation.
     */
    @Test
    fun getActiveSpotlight_returnsNearestApprovedFutureSlot() = runBlocking {
        val now = 5000L
        val past = createTestEntity(id = "PAST", status = "APPROVED", scheduledTime = 4000L)
        val nearestFuture = createTestEntity(id = "NEAREST", status = "APPROVED", scheduledTime = 5500L)
        val laterFuture = createTestEntity(id = "LATER", status = "APPROVED", scheduledTime = 7000L)

        reservationCacheDao.upsertReservations(listOf(past, nearestFuture, laterFuture))

        val spotlight = reservationCacheDao.getActiveSpotlight(now)
        assertNotNull(spotlight)
        assertEquals("NEAREST", spotlight?.reservationId)
    }

    /**
     * Verifies getActiveSpotlight returns null when no future approved reservations exist.
     */
    @Test
    fun getActiveSpotlight_noFutureApprovedReservations_returnsNull() = runBlocking {
        val now = 10_000L
        val pastApproved = createTestEntity(id = "PAST-APPROVED", status = "APPROVED", scheduledTime = 5_000L)
        val futurePending = createTestEntity(id = "FUTURE-PENDING", status = "PENDING", scheduledTime = 15_000L)
        val futureCompleted = createTestEntity(id = "FUTURE-COMPLETED", status = "COMPLETED", scheduledTime = 20_000L)

        reservationCacheDao.upsertReservations(listOf(pastApproved, futurePending, futureCompleted))

        val spotlight = reservationCacheDao.getActiveSpotlight(now)
        assertNull(spotlight)
    }

    /**
     * Verifies clearAll removes all records from cache.
     */
    @Test
    fun clearAll_emptiesTable() = runBlocking {
        val res = createTestEntity(id = "RES-1")
        reservationCacheDao.upsertReservation(res)
        reservationCacheDao.clearAll()

        val all = reservationCacheDao.getAllReservationsFlow().first()
        assertEquals(0, all.size)
    }

    /**
     * Verifies getTodayActiveReservationsFlow returns only approved reservations scheduled within today's window.
     */
    @Test
    fun getTodayActiveReservationsFlow_filtersByApprovedStatusAndDayWindow() = runBlocking {
        val startOfDay = 10_000L
        val endOfDay = 20_000L

        val beforeTodayApproved = createTestEntity(id = "RES-BEFORE", scheduledTime = 9_000L, status = "APPROVED")
        val todayApproved1 = createTestEntity(id = "RES-TODAY-1", scheduledTime = 12_000L, status = "APPROVED")
        val todayApproved2 = createTestEntity(id = "RES-TODAY-2", scheduledTime = 15_000L, status = "APPROVED")
        val todayPending = createTestEntity(id = "RES-TODAY-PENDING", scheduledTime = 14_000L, status = "PENDING")
        val afterTodayApproved = createTestEntity(id = "RES-AFTER", scheduledTime = 25_000L, status = "APPROVED")

        reservationCacheDao.upsertReservations(
            listOf(beforeTodayApproved, todayApproved1, todayApproved2, todayPending, afterTodayApproved)
        )

        val activeToday = reservationCacheDao.getTodayActiveReservationsFlow(startOfDay, endOfDay).first()
        assertEquals(2, activeToday.size)
        assertEquals("RES-TODAY-1", activeToday[0].reservationId)
        assertEquals("RES-TODAY-2", activeToday[1].reservationId)
    }

    private fun createTestEntity(
        id: String,
        nic: String = "199012345678",
        station: String = "Colombo Hub",
        scheduledTime: Long = 1000L,
        bay: String = "BAY-01",
        status: String = "APPROVED",
        estimatedKwh: Double = 25.0,
        meteredKwh: Double? = null
    ): ReservationCacheEntity {
        return ReservationCacheEntity(
            reservationId = id,
            prosumerNic = nic,
            stationName = station,
            scheduledTime = scheduledTime,
            allocatedBay = bay,
            status = status,
            qrPayload = "SSMTS-QR|$id|$nic|STATION|$scheduledTime|TESTSIG",
            estimatedKwh = estimatedKwh,
            meteredKwh = meteredKwh,
            lastSyncedAt = 1000L
        )
    }
}
