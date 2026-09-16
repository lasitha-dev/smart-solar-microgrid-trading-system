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
