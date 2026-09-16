/**
 * Description: Unit test suite for DashboardRepositoryImpl verifying live metric retrieval,
 * offline SQLite cache fallback (FR-M4-01.5), remote sync, and search streams.
 */
package com.sliit.ssmts.operator_dashboard.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit tests validating DashboardRepositoryImpl offline fallback and coordination behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DashboardRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: SsmtsDatabase
    private lateinit var reservationDao: ReservationCacheDao
    private lateinit var api: OperatorDashboardApi
    private lateinit var repository: DashboardRepositoryImpl

    /**
     * Initializes MockWebServer, in-memory SQLite database, and repository before each test.
     */
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SsmtsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reservationDao = database.reservationCacheDao()

        api = ApiClient.createOperatorDashboardApi(
            baseUrl = mockWebServer.url("/").toString(),
            enableLogging = false
        )

        repository = DashboardRepositoryImpl(
            api = api,
            dao = reservationDao,
            dispatcher = Dispatchers.Unconfined
        )
    }

    /**
     * Shuts down MockWebServer and closes in-memory database after each test.
     */
    @After
    @Throws(IOException::class)
    fun tearDown() {
        mockWebServer.shutdown()
        database.close()
    }

    /**
     * Verifies getDashboardMetricsStream emits live metrics from remote API when online.
     */
    @Test
    fun getDashboardMetricsStream_whenOnline_emitsLiveMetrics() = runBlocking {
        val jsonResponse = """
            {
              "pendingReservationsCount": 8,
              "approvedFutureReservationsCount": 15,
              "completedTodayCount": 5,
              "activeSpotlight": {
                "reservationId": "RES-SPOT-ONLINE",
                "stationName": "Negombo Solar Hub",
                "allocatedBayId": "BAY-01",
                "scheduledDateTime": "2026-09-16T14:00:00Z",
                "status": "Approved",
                "estimatedKwh": 40.0
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val emissions = repository.getDashboardMetricsStream(forceRefresh = true).toList()
        val latest = emissions.last()

        assertTrue(latest is NetworkResult.Success)
        val data = (latest as NetworkResult.Success).data
        assertFalse(data.isOfflineCached)
        assertEquals(8, data.pendingReservationsCount)
        assertEquals(15, data.approvedFutureReservationsCount)
        assertEquals(5, data.completedTodayCount)
        assertEquals("RES-SPOT-ONLINE", data.activeSpotlight?.reservationId)
    }

    /**
     * Verifies getDashboardMetricsStream falls back to tbl_reservations_cache when network is unreachable (FR-M4-01.5).
     */
    @Test
    fun getDashboardMetricsStream_whenOffline_fallsBackToCache() = runBlocking {
        val now = System.currentTimeMillis()
        val cached1 = ReservationCacheEntity(
            reservationId = "RES-P1",
            prosumerNic = "199112345678",
            stationName = "Local Station",
            scheduledTime = now + 100000L,
            allocatedBay = "BAY-05",
            status = "PENDING",
            estimatedKwh = 10.0,
            lastSyncedAt = now
        )
        val cached2 = ReservationCacheEntity(
            reservationId = "RES-A1",
            prosumerNic = "199212345678",
            stationName = "Local Station",
            scheduledTime = now + 500000L,
            allocatedBay = "BAY-02",
            status = "APPROVED",
            estimatedKwh = 35.0,
            lastSyncedAt = now
        )
        reservationDao.upsertReservations(listOf(cached1, cached2))

        // Remote API responds with 500 Server Error
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val emissions = repository.getDashboardMetricsStream(forceRefresh = true).toList()
        val fallbackResult = emissions.last()

        assertTrue(fallbackResult is NetworkResult.Success)
        val fallback = (fallbackResult as NetworkResult.Success).data
        assertTrue(fallback.isOfflineCached)
        assertEquals(1, fallback.pendingReservationsCount)
        assertEquals(1, fallback.approvedFutureReservationsCount)
        assertNotNull(fallback.activeSpotlight)
        assertEquals("RES-A1", fallback.activeSpotlight?.reservationId)
    }

    /**
     * Verifies getCachedReservationsStream filters local database records by keyword and status.
     */
    @Test
    fun getCachedReservationsStream_filtersByStatusAndKeyword() = runBlocking {
        val r1 = ReservationCacheEntity(
            reservationId = "RES-F1",
            prosumerNic = "199011111111",
            stationName = "Colombo Central",
            scheduledTime = 1000L,
            allocatedBay = "BAY-01",
            status = "APPROVED",
            lastSyncedAt = 1000L
        )
        val r2 = ReservationCacheEntity(
            reservationId = "RES-F2",
            prosumerNic = "200022222222",
            stationName = "Kandy Central",
            scheduledTime = 2000L,
            allocatedBay = "BAY-02",
            status = "PENDING",
            lastSyncedAt = 1000L
        )
        reservationDao.upsertReservations(listOf(r1, r2))

        val approvedList = repository.getCachedReservationsStream(status = "Approved", search = "Colombo").first()
        assertEquals(1, approvedList.size)
        assertEquals("RES-F1", approvedList[0].id)
        assertEquals(ReservationStatus.APPROVED, approvedList[0].status)
    }

    /**
     * Verifies syncRemoteReservations synchronizes remote records into tbl_reservations_cache.
     */
    @Test
    fun syncRemoteReservations_upsertsAllToLocalCache() = runBlocking {
        val jsonResponse = """
            [
              {
                "reservationId": "RES-SYNC-1",
                "prosumerNic": "199033333333",
                "stationName": "Galle Hub",
                "scheduledDateTime": "2026-09-16T12:00:00Z",
                "allocatedBayId": "BAY-03",
                "estimatedKwh": 25.0,
                "meteredEnergyKwh": null,
                "status": "Approved",
                "qrCode": "QR-CODE-TEST"
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val syncResult = repository.syncRemoteReservations()
        assertTrue(syncResult is NetworkResult.Success)

        val cachedRecords = reservationDao.getAllReservationsFlow().first()
        assertEquals(1, cachedRecords.size)
        assertEquals("RES-SYNC-1", cachedRecords[0].reservationId)
        assertEquals("Galle Hub", cachedRecords[0].stationName)
    }
}
