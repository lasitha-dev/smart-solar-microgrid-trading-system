/**
 * Description: Unit test suite for OperatorVerificationRepositoryImpl verifying QR code validation,
 * defensive metered energy bounds checking, SQLite cache updates, and audit logging against MockWebServer.
 */
package com.sliit.ssmts.operator_dashboard.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.local.dao.OperatorAuditDao
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit test validating OperatorVerificationRepositoryImpl compliance with rules and contracts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OperatorVerificationRepoTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: SsmtsDatabase
    private lateinit var reservationDao: ReservationCacheDao
    private lateinit var auditDao: OperatorAuditDao
    private lateinit var api: OperatorDashboardApi
    private lateinit var repository: OperatorVerificationRepositoryImpl

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
        auditDao = database.operatorAuditDao()

        api = ApiClient.createOperatorDashboardApi(
            baseUrl = mockWebServer.url("/").toString(),
            enableLogging = false
        )

        repository = OperatorVerificationRepositoryImpl(
            api = api,
            reservationDao = reservationDao,
            auditDao = auditDao,
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
     * Verifies that scanning an approved QR returns valid: true and domain reservation details.
     */
    @Test
    fun verifyScannedQr_approvedToken_returnsValidTrue() = runBlocking {
        val jsonResponse = """
            {
              "valid": true,
              "reservationId": "RES-VERIFY-1",
              "prosumerNic": "199412345678",
              "stationName": "Solar Station Alpha",
              "allocatedBayId": "BAY-03",
              "status": "Approved",
              "message": "Token signature verified successfully.",
              "errorCode": null
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = repository.verifyScannedQr("SSMTS-QR|RES-VERIFY-1|199412345678|STATION|2026-09-16|SIG")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.isValid)
        assertEquals("RES-VERIFY-1", data.reservationId)
        assertEquals("199412345678", data.prosumerNic)
        assertEquals("BAY-03", data.allocatedBayId)
        assertEquals(ReservationStatus.APPROVED, data.status)
    }

    /**
     * Verifies that finalizing transfer commits meteredEnergyKwh with 200 OK, updates SQLite cache,
     * and persists an operator audit transaction record.
     */
    @Test
    fun finalizeTransfer_validInput_commitsMeteredKwhAndUpdatesLocalCache() = runBlocking {
        val initialReservation = ReservationCacheEntity(
            reservationId = "RES-FINALIZE-10",
            prosumerNic = "200012345678",
            stationName = "Solar Station Beta",
            scheduledTime = 1000L,
            allocatedBay = "BAY-02",
            status = "APPROVED",
            meteredKwh = null,
            lastSyncedAt = 1000L
        )
        reservationDao.upsertReservation(initialReservation)

        val jsonResponse = """
            {
              "success": true,
              "reservationId": "RES-FINALIZE-10",
              "status": "Completed",
              "meteredEnergyKwh": 42.50,
              "finalizedAt": "2026-09-16T15:45:00Z",
              "finalizedByOperator": "OPERATOR-99"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = repository.finalizeTransfer("RES-FINALIZE-10", 42.50, "Delivered smoothly")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.isSuccess)
        assertEquals("RES-FINALIZE-10", data.reservationId)
        assertEquals(ReservationStatus.COMPLETED, data.status)
        assertEquals(42.50, data.meteredEnergyKwh, 0.001)

        // Verify SQLite cache was updated
        val updated = reservationDao.getReservationById("RES-FINALIZE-10")
        assertNotNull(updated)
        assertEquals("COMPLETED", updated?.status)
        assertEquals(42.50, updated?.meteredKwh ?: 0.0, 0.001)

        // Verify operator audit record was created
        val audits = auditDao.getAllAudits()
        assertEquals(1, audits.size)
        assertEquals("RES-FINALIZE-10", audits[0].reservationId)
        assertEquals("OPERATOR-99", audits[0].operatorId)
        assertEquals(42.50, audits[0].meteredKwh, 0.001)
    }

    /**
     * Verifies defensive validation rejects metered energy below 0.01 kWh before dispatching request.
     */
    @Test
    fun finalizeTransfer_inputBelowMinimum_returnsDefensiveValidationError() = runBlocking {
        val result = repository.finalizeTransfer("RES-1", 0.005)

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("ERR_INVALID_METERED_KWH", error.code)
        assertEquals(0, mockWebServer.requestCount)
    }

    /**
     * Verifies defensive validation rejects metered energy above 999.99 kWh before dispatching request.
     */
    @Test
    fun finalizeTransfer_inputAboveMaximum_returnsDefensiveValidationError() = runBlocking {
        val result = repository.finalizeTransfer("RES-1", 1000.0)

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("ERR_INVALID_METERED_KWH", error.code)
        assertEquals(0, mockWebServer.requestCount)
    }

    /**
     * Verifies defensive validation rejects blank reservation ID.
     */
    @Test
    fun finalizeTransfer_blankReservationId_returnsInvalidIdError() = runBlocking {
        val result = repository.finalizeTransfer("   ", 25.0)

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("ERR_INVALID_ID", error.code)
        assertEquals(0, mockWebServer.requestCount)
    }
}
