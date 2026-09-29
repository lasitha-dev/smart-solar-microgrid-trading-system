/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for ReservationRepositoryImpl using MockWebServer and MockK.
 */

package com.sliit.ssmts.reservation_workflow.data.repository

import com.sliit.ssmts.reservation_workflow.data.local.dao.ReservationDao
import com.sliit.ssmts.reservation_workflow.data.remote.ReservationApi
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

class ReservationRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: ReservationApi
    private lateinit var dao: ReservationDao
    private lateinit var repository: ReservationRepositoryImpl

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReservationApi::class.java)

        dao = mockk<ReservationDao>(relaxed = true)
        repository = ReservationRepositoryImpl(api, dao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `createReservation returns Success on 201 response`() = runTest {
        val successJson = """
            {
                "success": true,
                "message": "Reservation created successfully",
                "data": {
                    "id": "60d5ec49f1b2c42d8c3b4a99",
                    "prosumerId": "200112345678",
                    "stationId": "60d5ec49f1b2c42d8c3b4a59",
                    "bookingSlotId": "slot-01",
                    "scheduledDateTime": "2026-09-25T10:00:00.000Z",
                    "status": "Pending",
                    "qrCode": null,
                    "requestedAt": "2026-09-20T10:00:00.000Z",
                    "updatedAt": "2026-09-20T10:00:00.000Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(successJson)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.createReservation(
            prosumerId = "200112345678",
            stationId = "60d5ec49f1b2c42d8c3b4a59",
            slotId = "slot-01",
            scheduledTime = Date()
        )

        assertTrue("Result should be Success", result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("60d5ec49f1b2c42d8c3b4a99", data.id)
        assertEquals("200112345678", data.prosumerId)
    }

    @Test
    fun `createReservation returns Error on 400 business rule response`() = runTest {
        val errorJson = """
            {
                "success": false,
                "message": "Reservation must be within 7 days from today.",
                "code": "RESERVATION_WINDOW_INVALID",
                "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorJson)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.createReservation(
            prosumerId = "200112345678",
            stationId = "60d5ec49f1b2c42d8c3b4a59",
            slotId = "slot-01",
            scheduledTime = Date()
        )

        assertTrue("Result should be Error", result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("within 7 days"))
    }

    @Test
    fun `cancelReservation returns Error on 409 conflict response`() = runTest {
        val conflictJson = """
            {
                "success": false,
                "message": "Cancellations are not allowed within 12 hours of the scheduled time.",
                "code": "MODIFICATION_WINDOW_CLOSED",
                "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody(conflictJson)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.cancelReservation("RES-999", "Emergency")

        assertTrue("Result should be Error", result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("409", error.code)
    }
}
