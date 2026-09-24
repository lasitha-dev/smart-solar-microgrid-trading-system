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
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.data.remote.interceptor.AuthTokenProvider
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import com.sliit.ssmts.operator_dashboard.util.TransferCompletedEvent
import com.sliit.ssmts.operator_dashboard.util.TransferSyncNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    companion object {
        private const val TEST_BEARER_TOKEN = "MOCK_OPERATOR_JWT_TOKEN_ABC123"
        private const val SPEC_RESERVATION_ID = "664fa10b9c3e2e1a4f001201"
        private const val SPEC_PROSUMER_NIC = "200012345678"
        private const val SPEC_STATION_NAME = "Peradeniya Agro-Voltaic Hub"
        private const val SPEC_ALLOCATED_BAY = "BAY-02"
        private const val SPEC_OPERATOR_ID = "OP-PERADENIYA-01"
        private const val SPEC_METERED_KWH = 24.65
    }

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

        val tokenProvider = AuthTokenProvider { TEST_BEARER_TOKEN }

        api = ApiClient.createOperatorDashboardApi(
            baseUrl = mockWebServer.url("/").toString(),
            tokenProvider = tokenProvider,
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
     * Asserts that entering 24.65 kWh issues PATCH /api/reservations/{id}/finalize,
     * verifies HTTP method, path, headers, and payload, handles 200 OK transition to Completed,
     * updates SQLite cache, and persists operator audit (MEMBER_4_SRS Table 6 & Section 4.4).
     */
    @Test
    fun finalizeTransfer_24_65Kwh_issuesPatchAndTransitionsToCompleted() = runBlocking {
        // 1. Seed initial reservation cache with status APPROVED
        val initialReservation = ReservationCacheEntity(
            reservationId = SPEC_RESERVATION_ID,
            prosumerNic = SPEC_PROSUMER_NIC,
            stationName = SPEC_STATION_NAME,
            scheduledTime = 1726655400000L,
            allocatedBay = SPEC_ALLOCATED_BAY,
            status = "APPROVED",
            meteredKwh = null,
            lastSyncedAt = 1726655400000L
        )
        reservationDao.upsertReservation(initialReservation)

        // 2. Enqueue 200 OK response from C# Web API
        val jsonResponse = """
            {
              "success": true,
              "reservationId": "$SPEC_RESERVATION_ID",
              "status": "Completed",
              "meteredEnergyKwh": $SPEC_METERED_KWH,
              "finalizedAt": "2026-09-18T11:45:00Z",
              "finalizedByOperator": "$SPEC_OPERATOR_ID"
            }
        """.trimIndent()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(jsonResponse)
        )

        // 3. Dispatch finalization
        val notes = "Transfer executed without voltage anomalies."
        val result = repository.finalizeTransfer(SPEC_RESERVATION_ID, SPEC_METERED_KWH, notes)

        // 4. Verify NetworkResult.Success and domain model fields
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.isSuccess)
        assertEquals(SPEC_RESERVATION_ID, data.reservationId)
        assertEquals(ReservationStatus.COMPLETED, data.status)
        assertEquals(SPEC_METERED_KWH, data.meteredEnergyKwh, 0.001)
        assertEquals("2026-09-18T11:45:00Z", data.finalizedAtIso)
        assertEquals(SPEC_OPERATOR_ID, data.finalizedByOperator)

        // 5. Verify RecordedRequest HTTP contract: Method, Path, Auth Header, and Body
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PATCH", recordedRequest.method)
        assertEquals("/api/reservations/$SPEC_RESERVATION_ID/finalize", recordedRequest.path)
        assertEquals("Bearer $TEST_BEARER_TOKEN", recordedRequest.getHeader("Authorization"))
        assertTrue(recordedRequest.getHeader("Content-Type")?.contains("application/json") == true)

        val requestBody = recordedRequest.body.readUtf8()
        assertTrue("Request body must contain meteredEnergyKwh", requestBody.contains("\"meteredEnergyKwh\":24.65"))
        assertTrue("Request body must contain notes", requestBody.contains("Transfer executed without voltage anomalies."))

        // 6. Verify SQLite cache was mutated to COMPLETED with metered power
        val updatedCache = reservationDao.getReservationById(SPEC_RESERVATION_ID)
        assertNotNull(updatedCache)
        assertEquals("COMPLETED", updatedCache?.status)
        assertEquals(SPEC_METERED_KWH, updatedCache?.meteredKwh ?: 0.0, 0.001)

        // 7. Verify operator audit log record was persisted
        val audits = auditDao.getAllAudits()
        assertEquals(1, audits.size)
        assertEquals(SPEC_RESERVATION_ID, audits[0].reservationId)
        assertEquals(SPEC_OPERATOR_ID, audits[0].operatorId)
        assertEquals(SPEC_METERED_KWH, audits[0].meteredKwh, 0.001)
        assertEquals(OperatorAuditEntity.STATUS_SYNCED, audits[0].syncStatus)
    }

    /**
     * Verifies that finalizing transfer broadcasts a reactive event via TransferSyncNotifier.
     */
    @Test
    fun finalizeTransfer_emitsTransferSyncNotifierEvent() = runBlocking {
        val jsonResponse = """
            {
              "success": true,
              "reservationId": "$SPEC_RESERVATION_ID",
              "status": "Completed",
              "meteredEnergyKwh": $SPEC_METERED_KWH,
              "finalizedAt": "2026-09-18T11:45:00Z",
              "finalizedByOperator": "$SPEC_OPERATOR_ID"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        var capturedEvent: TransferCompletedEvent? = null
        val job = launch(Dispatchers.Unconfined) {
            capturedEvent = TransferSyncNotifier.events.first()
        }

        repository.finalizeTransfer(SPEC_RESERVATION_ID, SPEC_METERED_KWH, null)
        job.join()

        assertNotNull(capturedEvent)
        assertEquals(SPEC_RESERVATION_ID, capturedEvent?.reservationId)
        assertEquals(SPEC_METERED_KWH, capturedEvent?.meteredKwh ?: 0.0, 0.001)
    }

    /**
     * Verifies that scanning an approved QR issues POST /api/reservations/verify-qr,
     * sends Bearer authorization, and returns domain reservation details.
     */
    @Test
    fun verifyScannedQr_approvedToken_returnsValidTrue() = runBlocking {
        val jsonResponse = """
            {
              "valid": true,
              "reservationId": "$SPEC_RESERVATION_ID",
              "prosumerNic": "$SPEC_PROSUMER_NIC",
              "stationName": "$SPEC_STATION_NAME",
              "allocatedBayId": "$SPEC_ALLOCATED_BAY",
              "status": "Approved",
              "message": "QR token valid. Proceed to physical energy transfer.",
              "errorCode": null
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val rawQr = "SSMTS-QR|$SPEC_RESERVATION_ID|$SPEC_PROSUMER_NIC|ST-002|2026-09-18T10:30:00Z|SIG-a9b8c7"
        val result = repository.verifyScannedQr(rawQr)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data.isValid)
        assertEquals(SPEC_RESERVATION_ID, data.reservationId)
        assertEquals(SPEC_PROSUMER_NIC, data.prosumerNic)
        assertEquals(SPEC_STATION_NAME, data.stationName)
        assertEquals(SPEC_ALLOCATED_BAY, data.allocatedBayId)
        assertEquals(ReservationStatus.APPROVED, data.status)
        assertNull(data.errorCode)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/api/reservations/verify-qr", recordedRequest.path)
        assertEquals("Bearer $TEST_BEARER_TOKEN", recordedRequest.getHeader("Authorization"))
        assertTrue(recordedRequest.body.readUtf8().contains(SPEC_RESERVATION_ID))
    }

    /**
     * Verifies that when server rejects QR with 409 Conflict (ERR_RESERVATION_ALREADY_COMPLETED),
     * repository maps it to NetworkResult.Error with HTTP_409 code.
     */
    @Test
    fun verifyScannedQr_serverConflictAlreadyCompleted_returnsNetworkError() = runBlocking {
        val errorJson = """
            {
              "valid": false,
              "errorCode": "ERR_RESERVATION_ALREADY_COMPLETED",
              "message": "This reservation was already finalized on 2026-09-18T11:05:00Z."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(errorJson))

        val result = repository.verifyScannedQr("SSMTS-QR|$SPEC_RESERVATION_ID|$SPEC_PROSUMER_NIC|ST-002|2026-09-18|SIG")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("HTTP_409", error.code)
        assertTrue(error.message.contains("ERR_RESERVATION_ALREADY_COMPLETED"))
    }

    /**
     * Verifies that when server rejects QR with 400 Bad Request (ERR_INVALID_QR_SIGNATURE),
     * repository maps it to NetworkResult.Error with HTTP_400 code.
     */
    @Test
    fun verifyScannedQr_serverBadRequestInvalidSignature_returnsNetworkError() = runBlocking {
        val errorJson = """
            {
              "valid": false,
              "errorCode": "ERR_INVALID_QR_SIGNATURE",
              "message": "Cryptographic HMAC signature mismatch."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(errorJson))

        val result = repository.verifyScannedQr("SSMTS-QR|TAMPERED|NIC|ST|TIME|BAD_SIG")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("HTTP_400", error.code)
        assertTrue(error.message.contains("ERR_INVALID_QR_SIGNATURE"))
    }

    /**
     * Verifies defensive validation rejects empty or whitespace-only QR string before network dispatch.
     */
    @Test
    fun verifyScannedQr_emptyPayload_returnsDefensiveValidationErrorWithoutNetworkCall() = runBlocking {
        val result = repository.verifyScannedQr("   ")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("ERR_EMPTY_QR", error.code)
        assertEquals(0, mockWebServer.requestCount)
    }

    /**
     * Verifies that when finalizing transfer fails with 409 Conflict, the local SQLite cache
     * is not modified to Completed and no operator audit record is stored.
     */
    @Test
    fun finalizeTransfer_serverConflictAlreadyCompleted_returnsHttp409AndDoesNotMutateCache() = runBlocking {
        val initialReservation = ReservationCacheEntity(
            reservationId = SPEC_RESERVATION_ID,
            prosumerNic = SPEC_PROSUMER_NIC,
            stationName = SPEC_STATION_NAME,
            scheduledTime = 1726655400000L,
            allocatedBay = SPEC_ALLOCATED_BAY,
            status = "APPROVED",
            meteredKwh = null,
            lastSyncedAt = 1726655400000L
        )
        reservationDao.upsertReservation(initialReservation)

        val errorJson = """
            {
              "success": false,
              "errorCode": "ERR_RESERVATION_ALREADY_COMPLETED",
              "message": "This reservation was already finalized."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(409).setBody(errorJson))

        val result = repository.finalizeTransfer(SPEC_RESERVATION_ID, 24.65)

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("HTTP_409", error.code)

        // Cache must remain APPROVED
        val cached = reservationDao.getReservationById(SPEC_RESERVATION_ID)
        assertNotNull(cached)
        assertEquals("APPROVED", cached?.status)
        assertNull(cached?.meteredKwh)

        // No audit entry written
        assertEquals(0, auditDao.getAllAudits().size)
    }

    /**
     * Verifies that when reservation ID does not exist on server, 404 Not Found returns HTTP_404.
     */
    @Test
    fun finalizeTransfer_serverNotFound_returnsHttp404() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "errorCode": "ERR_RESERVATION_NOT_FOUND",
              "message": "Reservation not found."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody(errorJson))

        val result = repository.finalizeTransfer("UNKNOWN-RES-ID", 15.0)

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("HTTP_404", error.code)
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
     * Verifies defensive validation rejects NaN metered energy readings before dispatching request.
     */
    @Test
    fun finalizeTransfer_nanMeteredEnergy_returnsDefensiveValidationError() = runBlocking {
        val result = repository.finalizeTransfer("RES-1", Double.NaN)

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

    /**
     * Verifies boundary values 0.01 kWh (minimum) and 999.99 kWh (maximum) succeed and dispatch valid requests.
     */
    @Test
    fun finalizeTransfer_boundaryValues_0_01KwhAnd999_99Kwh_succeeds() = runBlocking {
        // Minimum boundary: 0.01 kWh
        val minResponse = """
            {
              "success": true,
              "reservationId": "RES-BOUND-MIN",
              "status": "Completed",
              "meteredEnergyKwh": 0.01,
              "finalizedAt": "2026-09-18T12:00:00Z",
              "finalizedByOperator": "$SPEC_OPERATOR_ID"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(minResponse))

        val minResult = repository.finalizeTransfer("RES-BOUND-MIN", 0.01)
        assertTrue(minResult is NetworkResult.Success)
        assertEquals(0.01, (minResult as NetworkResult.Success).data.meteredEnergyKwh, 0.001)

        // Maximum boundary: 999.99 kWh
        val maxResponse = """
            {
              "success": true,
              "reservationId": "RES-BOUND-MAX",
              "status": "Completed",
              "meteredEnergyKwh": 999.99,
              "finalizedAt": "2026-09-18T12:00:00Z",
              "finalizedByOperator": "$SPEC_OPERATOR_ID"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(maxResponse))

        val maxResult = repository.finalizeTransfer("RES-BOUND-MAX", 999.99)
        assertTrue(maxResult is NetworkResult.Success)
        assertEquals(999.99, (maxResult as NetworkResult.Success).data.meteredEnergyKwh, 0.001)
    }

    /**
     * Verifies that network exceptions or abrupt server disconnects return NetworkResult.Exception.
     */
    @Test
    fun finalizeTransfer_networkDisconnect_returnsExceptionResult() = runBlocking {
        mockWebServer.shutdown()

        val result = repository.finalizeTransfer(SPEC_RESERVATION_ID, 25.0)

        assertTrue(result is NetworkResult.Exception)
    }
}
