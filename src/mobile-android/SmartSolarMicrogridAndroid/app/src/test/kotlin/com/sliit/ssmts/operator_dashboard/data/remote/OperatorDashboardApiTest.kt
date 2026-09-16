/**
 * Description: Integration unit test suite for OperatorDashboardApi verifying serialization,
 * deserialization, HTTP methods, and query parameter handling against MockWebServer.
 */
package com.sliit.ssmts.operator_dashboard.data.remote

import com.sliit.ssmts.operator_dashboard.data.remote.dto.FinalizeTransferDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.QrVerificationRequestDto
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
import java.io.IOException

/**
 * Integration unit test class for OperatorDashboardApi Retrofit endpoints.
 */
class OperatorDashboardApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: OperatorDashboardApi

    /**
     * Sets up MockWebServer and initializes Retrofit API client.
     */
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = ApiClient.createOperatorDashboardApi(
            baseUrl = mockWebServer.url("/").toString(),
            enableLogging = false
        )
    }

    /**
     * Shuts down MockWebServer after test completion.
     */
    @After
    @Throws(IOException::class)
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Verifies getDashboardMetrics parses response metrics and spotlight fields accurately.
     */
    @Test
    fun getDashboardMetrics_parsesSuccessfully() = runBlocking {
        val jsonResponse = """
            {
              "pendingReservationsCount": 4,
              "approvedFutureReservationsCount": 9,
              "completedTodayCount": 7,
              "activeSpotlight": {
                "reservationId": "RES-SPOT-101",
                "stationName": "Kandy Hub",
                "allocatedBayId": "BAY-02",
                "scheduledDateTime": "2026-09-16T14:30:00Z",
                "status": "Approved",
                "estimatedKwh": 32.5
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val response = api.getDashboardMetrics()
        assertTrue(response.isSuccessful)

        val body = response.body()
        assertNotNull(body)
        assertEquals(4, body?.pendingReservationsCount)
        assertEquals(9, body?.approvedFutureReservationsCount)
        assertEquals(7, body?.completedTodayCount)

        val spotlight = body?.activeSpotlight
        assertNotNull(spotlight)
        assertEquals("RES-SPOT-101", spotlight?.reservationId)
        assertEquals("Kandy Hub", spotlight?.stationName)
        assertEquals("BAY-02", spotlight?.allocatedBayId)
        assertEquals(32.5, spotlight?.estimatedKwh ?: 0.0, 0.001)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/reservations/dashboard-metrics", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    /**
     * Verifies getReservations includes query parameters and parses list of reservations.
     */
    @Test
    fun getReservations_withFilters_sendsQueryParameters() = runBlocking {
        val jsonResponse = """
            [
              {
                "reservationId": "RES-001",
                "prosumerNic": "199012345678",
                "stationName": "Colombo Hub",
                "scheduledDateTime": "2026-09-16T10:00:00Z",
                "allocatedBayId": "BAY-01",
                "estimatedKwh": 20.0,
                "meteredEnergyKwh": null,
                "status": "Approved",
                "qrCode": "SSMTS-QR|RES-001|..."
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val response = api.getReservations(status = "Approved", search = "1990", date = "2026-09-16")
        assertTrue(response.isSuccessful)

        val list = response.body()
        assertNotNull(list)
        assertEquals(1, list?.size)
        assertEquals("RES-001", list?.get(0)?.reservationId)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path?.contains("status=Approved") == true)
        assertTrue(recordedRequest.path?.contains("search=1990") == true)
        assertTrue(recordedRequest.path?.contains("date=2026-09-16") == true)
    }

    /**
     * Verifies verifyQr dispatches POST request and parses verification response.
     */
    @Test
    fun verifyQr_sendsPayload_andParsesResponse() = runBlocking {
        val jsonResponse = """
            {
              "valid": true,
              "reservationId": "RES-888",
              "prosumerNic": "199512345678",
              "stationName": "Galle Hub",
              "allocatedBayId": "BAY-03",
              "status": "Approved",
              "message": "Token verified successfully",
              "errorCode": null
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val requestPayload = QrVerificationRequestDto("SSMTS-QR|RES-888|199512345678|STATION|2026-09-16|SIGNATURE")
        val response = api.verifyQr(requestPayload)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertTrue(body?.valid == true)
        assertEquals("RES-888", body?.reservationId)
        assertEquals("Approved", body?.status)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/reservations/verify-qr", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.body.readUtf8().contains("SSMTS-QR|RES-888"))
    }

    /**
     * Verifies finalizeTransfer dispatches PATCH request and deserializes finalization receipt.
     */
    @Test
    fun finalizeTransfer_sendsMeteredKwh_andParsesResponse() = runBlocking {
        val jsonResponse = """
            {
              "success": true,
              "reservationId": "RES-999",
              "status": "Completed",
              "meteredEnergyKwh": 50.75,
              "finalizedAt": "2026-09-16T15:30:00Z",
              "finalizedByOperator": "OP-TEST"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val requestPayload = FinalizeTransferDto(meteredEnergyKwh = 50.75, notes = "Verified OK")
        val response = api.finalizeTransfer("RES-999", requestPayload)

        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertTrue(body?.success == true)
        assertEquals("RES-999", body?.reservationId)
        assertEquals("Completed", body?.status)
        assertEquals(50.75, body?.meteredEnergyKwh ?: 0.0, 0.001)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/reservations/RES-999/finalize", recordedRequest.path)
        assertEquals("PATCH", recordedRequest.method)
        assertTrue(recordedRequest.body.readUtf8().contains("50.75"))
    }

    /**
     * Verifies that server error codes (e.g., 400 Bad Request) are accurately reflected.
     */
    @Test
    fun verifyQr_errorResponse_returnsNonSuccessStatus() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("""{"valid":false,"errorCode":"ERR_INVALID_QR_SIGNATURE"}"""))

        val response = api.verifyQr(QrVerificationRequestDto("INVALID_TOKEN"))
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }
}
