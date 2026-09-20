/**
 * Description: Comprehensive end-to-end integration test validating the entire Grid Operator lifecycle
 * for viva examination: dashboard metrics, active spotlight countdown, search/filters, Fast Test QR handshake,
 * metered energy finalization, dual SQLite cache commit, and reactive UI synchronization.
 */
package com.sliit.ssmts.operator_dashboard.viva

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.local.dao.OperatorAuditDao
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.data.remote.interceptor.AuthTokenProvider
import com.sliit.ssmts.operator_dashboard.data.repository.DashboardRepositoryImpl
import com.sliit.ssmts.operator_dashboard.data.repository.OperatorVerificationRepositoryImpl
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.ui.dashboard.DashboardFeedTab
import com.sliit.ssmts.operator_dashboard.ui.dashboard.DashboardViewModel
import com.sliit.ssmts.operator_dashboard.ui.history.BookingHistoryViewModel
import com.sliit.ssmts.operator_dashboard.ui.operator.OperatorScannerViewModel
import com.sliit.ssmts.operator_dashboard.ui.operator.ScannerUiState
import com.sliit.ssmts.operator_dashboard.util.FastTestQrScenarios
import com.sliit.ssmts.operator_dashboard.util.TimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.util.Calendar

/**
 * End-to-end integration test validating all Member 4 requirements in a single interconnected scenario.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VivaDemonstrationFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: SsmtsDatabase
    private lateinit var reservationDao: ReservationCacheDao
    private lateinit var auditDao: OperatorAuditDao
    private lateinit var api: OperatorDashboardApi

    private lateinit var dashboardRepository: DashboardRepositoryImpl
    private lateinit var verificationRepository: OperatorVerificationRepositoryImpl

    private val testTodayMillis: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Prepares in-memory Room database, MockWebServer, and repository instances before test execution.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SsmtsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reservationDao = database.reservationCacheDao()
        auditDao = database.operatorAuditDao()

        mockWebServer = MockWebServer()
        mockWebServer.start(java.net.InetAddress.getByName("127.0.0.1"), 0)

        api = ApiClient.createOperatorDashboardApi(
            baseUrl = mockWebServer.url("/").toString(),
            tokenProvider = { "VIVA_OPERATOR_BEARER_JWT_2026" },
            enableLogging = false
        )

        dashboardRepository = DashboardRepositoryImpl(api, reservationDao, Dispatchers.Unconfined)
        verificationRepository = OperatorVerificationRepositoryImpl(api, reservationDao, auditDao, Dispatchers.Unconfined)
    }

    /**
     * Cleans up MockWebServer, closes database, and resets coroutine test dispatchers.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
        mockWebServer.shutdown()
    }

    /**
     * Seeds initial reservations representing Table 6 specifications into the in-memory Room cache.
     */
    private suspend fun seedInitialReservations() {
        val now = System.currentTimeMillis()
        val reservations = listOf(
            // 1. Target Approved slot (Table 6 spec: 664fa10b9c3e2e1a4f001201) scheduled today (+30 mins)
            ReservationCacheEntity(
                reservationId = "664fa10b9c3e2e1a4f001201",
                prosumerNic = "200012345678",
                stationName = "Peradeniya Agro-Voltaic Hub",
                scheduledTime = now + 1800000L,
                allocatedBay = "BAY-02",
                estimatedKwh = 25.0,
                meteredKwh = null,
                status = "Approved",
                lastSyncedAt = now
            ),
            // 2. Another future approved slot in 3 days
            ReservationCacheEntity(
                reservationId = "664fa10b9c3e2e1a4f001202",
                prosumerNic = "199411223344",
                stationName = "Colombo Central Hub",
                scheduledTime = now + (86400000L * 3),
                allocatedBay = "BAY-01",
                estimatedKwh = 18.5,
                meteredKwh = null,
                status = "Approved",
                lastSyncedAt = now
            ),
            // 3. Pending slot awaiting operator review in 2 days
            ReservationCacheEntity(
                reservationId = "664fa10b9c3e2e1a4f001203",
                prosumerNic = "198855667788",
                stationName = "Galle Maritime Microgrid",
                scheduledTime = now + (86400000L * 2),
                allocatedBay = "BAY-04",
                estimatedKwh = 30.0,
                meteredKwh = null,
                status = "Pending",
                lastSyncedAt = now
            ),
            // 4. Completed slot earlier today (-30 mins)
            ReservationCacheEntity(
                reservationId = "664fa10b9c3e2e1a4f001204",
                prosumerNic = "197599887766",
                stationName = "Peradeniya Agro-Voltaic Hub",
                scheduledTime = now - 1800000L,
                allocatedBay = "BAY-03",
                estimatedKwh = 15.0,
                meteredKwh = 14.85,
                status = "Completed",
                lastSyncedAt = now
            )
        )
        reservationDao.upsertReservations(reservations)
    }

    /**
     * Executes the complete end-to-end operator lifecycle demonstration:
     * 1. Dashboard metrics aggregation & spotlight selection
     * 2. Search & filter chip queries
     * 3. Fast Test QR payload injection & verification handshake
     * 4. Metered kWh entry & central API finalization
     * 5. Dual-table SQLite cache synchronization
     * 6. Reactive dashboard re-aggregation with updated counters
     */
    @Test
    fun viva_complete_operator_lifecycle_flow() = runTest(testDispatcher) {
        // Step 1: Seed database with initial records
        seedInitialReservations()

        // --- STAGE 1: OPERATIONAL DASHBOARD METRICS & SPOTLIGHT ---
        val initialMetricsResponseJson = """
            {
              "pendingReservationsCount": 1,
              "approvedFutureReservationsCount": 2,
              "completedTodayCount": 1,
              "activeSpotlight": {
                "reservationId": "664fa10b9c3e2e1a4f001201",
                "stationName": "Peradeniya Agro-Voltaic Hub",
                "allocatedBayId": "BAY-02",
                "scheduledDateTime": "2026-09-18T10:30:00Z",
                "status": "Approved",
                "estimatedKwh": 25.0
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(initialMetricsResponseJson))

        val dashboardVm = DashboardViewModel(dashboardRepository)
        val dashboardState = awaitState(
            getCurrentState = { dashboardVm.uiState.value },
            isPending = { it is UiState.Loading }
        )

        assertTrue("Dashboard should load successfully from cache", dashboardState is UiState.Success)
        val metrics = (dashboardState as UiState.Success).data

        assertEquals("Pending count should match seeded Pending slots", 1, metrics.pendingReservationsCount)
        assertEquals("Approved future count should match seeded Approved slots", 2, metrics.approvedFutureReservationsCount)
        assertEquals("Completed today count should match seeded Completed slots", 1, metrics.completedTodayCount)

        assertNotNull("Active spotlight should be present for today's nearest slot", metrics.activeSpotlight)
        assertEquals("664fa10b9c3e2e1a4f001201", metrics.activeSpotlight?.reservationId)
        assertEquals("BAY-02", metrics.activeSpotlight?.allocatedBayId)
        assertEquals(25.0, metrics.activeSpotlight?.estimatedKwh ?: 0.0, 0.01)

        val countdown = TimeFormatter.formatCountdown(metrics.activeSpotlight?.scheduledTimeMillis ?: 0L)
        assertFalse("Countdown should be formatted and non-empty", countdown.isBlank())

        // Verify feeds: Today's Active has 1 item, Pending Queue has 1 item
        dashboardVm.selectFeedTab(DashboardFeedTab.TODAY_ACTIVE)
        advanceUntilIdle()
        assertEquals(1, dashboardVm.feedReservations.value.size)
        assertEquals("664fa10b9c3e2e1a4f001201", dashboardVm.feedReservations.value[0].id)

        dashboardVm.selectFeedTab(DashboardFeedTab.PENDING_QUEUE)
        advanceUntilIdle()
        assertEquals(1, dashboardVm.feedReservations.value.size)
        assertEquals("664fa10b9c3e2e1a4f001203", dashboardVm.feedReservations.value[0].id)

        // --- STAGE 2: SEARCHABLE BOOKING HISTORY & FILTER CHIPS ---
        val historyVm = BookingHistoryViewModel(dashboardRepository)
        advanceUntilIdle()

        // Search by Prosumer NIC (Rule 1.9: 300ms debounce)
        historyVm.onSearchQueryChanged("200012345678")
        advanceTimeBy(350L)
        val nicSearchState = awaitState(
            getCurrentState = { historyVm.historyUiState.value },
            isPending = { it is UiState.Loading }
        )
        assertTrue(nicSearchState is UiState.Success)
        val nicResults = (nicSearchState as UiState.Success).data
        assertEquals(1, nicResults.size)
        assertEquals("664fa10b9c3e2e1a4f001201", nicResults[0].id)

        // Filter by Status: Approved
        historyVm.onSearchQueryChanged("")
        historyVm.onStatusFilterSelected("Approved")
        advanceTimeBy(350L)
        val approvedSearchState = awaitState(
            getCurrentState = { historyVm.historyUiState.value },
            isPending = { it is UiState.Loading }
        )
        assertTrue(approvedSearchState is UiState.Success)
        val approvedResults = (approvedSearchState as UiState.Success).data
        assertEquals(2, approvedResults.size)
        assertTrue(approvedResults.all { it.status == ReservationStatus.APPROVED })

        // --- STAGE 3 & 4: FAST TEST QR SCAN & VERIFICATION HANDSHAKE ---
        val scannerVm = OperatorScannerViewModel(verificationRepository)
        advanceUntilIdle()

        // Mock central C# API verify-qr response for Table 6 spec
        val verifyQrResponseJson = """
            {
                "valid": true,
                "reservationId": "664fa10b9c3e2e1a4f001201",
                "prosumerNic": "200012345678",
                "stationName": "Peradeniya Agro-Voltaic Hub",
                "allocatedBayId": "BAY-02",
                "scheduledDateTime": "2026-09-18T10:30:00Z",
                "status": "Approved",
                "message": "Token signature valid. Ready for transfer."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(verifyQrResponseJson))

        // Inject Fast Test QR simulation payload (Rule 6.3)
        val validPayload = FastTestQrScenarios.APPROVED_VALID_PAYLOAD
        scannerVm.verifyQrToken(validPayload)
        val scannerState = awaitState(
            getCurrentState = { scannerVm.uiState.value },
            isPending = { it is ScannerUiState.Verifying }
        )

        assertTrue("Scanner should transition to Handshake on valid QR", scannerState is ScannerUiState.Handshake)
        val handshake = (scannerState as ScannerUiState.Handshake).reservation
        assertEquals("664fa10b9c3e2e1a4f001201", handshake.reservationId)
        assertEquals("BAY-02", handshake.allocatedBayId)
        assertEquals("200012345678", handshake.prosumerNic)

        // --- STAGE 5: METERED ENERGY INPUT & FINALIZATION COMMIT ---
        val finalizeResponseJson = """
            {
                "success": true,
                "reservationId": "664fa10b9c3e2e1a4f001201",
                "status": "Completed",
                "meteredEnergyKwh": 24.65,
                "finalizedAt": "2026-09-18T11:45:00Z",
                "finalizedByOperator": "OP-PERADENIYA-01"
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(finalizeResponseJson))

        // Submit metered energy 24.65 kWh
        scannerVm.finalizeEnergyTransfer(
            reservationId = "664fa10b9c3e2e1a4f001201",
            meteredKwh = 24.65,
            notes = "Completed solar injection at Bay-02."
        )
        val finalizedState = awaitState(
            getCurrentState = { scannerVm.uiState.value },
            isPending = { it is ScannerUiState.Finalizing }
        )

        assertTrue("Scanner should transition to Finalized state", finalizedState is ScannerUiState.Finalized)
        val receipt = (finalizedState as ScannerUiState.Finalized).receipt
        assertEquals("664fa10b9c3e2e1a4f001201", receipt.reservationId)
        assertEquals(24.65, receipt.meteredEnergyKwh, 0.001)
        assertEquals(ReservationStatus.COMPLETED, receipt.status)

        // --- STAGE 6: LOCAL SQLITE CACHE SYNC & AUDIT PERSISTENCE ---
        val updatedReservation = reservationDao.getReservationById("664fa10b9c3e2e1a4f001201")
        assertNotNull("Reservation must exist in local SQLite cache", updatedReservation)
        assertEquals("COMPLETED", updatedReservation?.status)
        assertEquals(24.65, updatedReservation?.meteredKwh ?: 0.0, 0.001)

        val audits = auditDao.getAllAudits()
        assertTrue("Operator audit trail must contain entry", audits.isNotEmpty())
        val latestAudit = audits.first()
        assertEquals("664fa10b9c3e2e1a4f001201", latestAudit.reservationId)
        assertEquals(24.65, latestAudit.meteredKwh, 0.001)

        // --- STAGE 7: REACTIVE DASHBOARD COUNTER RECALCULATION ---
        val refreshedMetricsResponseJson = """
            {
              "pendingReservationsCount": 1,
              "approvedFutureReservationsCount": 1,
              "completedTodayCount": 2,
              "activeSpotlight": null
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(refreshedMetricsResponseJson))

        dashboardVm.loadMetrics(forceRefresh = true)
        val refreshedState = awaitState(
            getCurrentState = { dashboardVm.uiState.value },
            isPending = { it is UiState.Loading }
        )

        assertTrue(refreshedState is UiState.Success)
        val updatedMetrics = (refreshedState as UiState.Success).data

        assertEquals("Completed count must now be 2 after finalization", 2, updatedMetrics.completedTodayCount)
        assertEquals("Pending count should remain 1", 1, updatedMetrics.pendingReservationsCount)
        assertEquals("Approved future count should now be 1", 1, updatedMetrics.approvedFutureReservationsCount)
    }

    /**
     * Asserts that attempting to scan an already completed reservation is rejected
     * by the central API with an explicit ERR_RESERVATION_ALREADY_COMPLETED error.
     */
    @Test
    fun viva_double_finalize_rejection_flow() = runTest(testDispatcher) {
        seedInitialReservations()

        val scannerVm = OperatorScannerViewModel(verificationRepository)
        advanceUntilIdle()

        // Mock 409 Conflict rejection response
        val conflictJson = """
            {
                "valid": false,
                "errorCode": "ERR_RESERVATION_ALREADY_COMPLETED",
                "message": "This reservation has already been completed and finalized."
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(conflictJson))

        val completedPayload = FastTestQrScenarios.ALREADY_COMPLETED_PAYLOAD
        scannerVm.verifyQrToken(completedPayload)
        val state = awaitState(
            getCurrentState = { scannerVm.uiState.value },
            isPending = { it is ScannerUiState.Verifying }
        )

        assertTrue("Re-scanning completed slot must transition to Rejection", state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("ERR_RESERVATION_ALREADY_COMPLETED", rejection.errorCode)
        assertTrue(rejection.message.contains("already been completed"))
    }

    /**
     * Waits for an asynchronous ViewModel state transition that relies on external OkHttp thread execution.
     */
    private fun <T> awaitState(
        getCurrentState: () -> T,
        isPending: (T) -> Boolean,
        maxAttempts: Int = 40,
        intervalMs: Long = 50L
    ): T {
        testDispatcher.scheduler.advanceUntilIdle()
        var attempts = 0
        while (isPending(getCurrentState()) && attempts < maxAttempts) {
            Thread.sleep(intervalMs)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }
        return getCurrentState()
    }

    /**
     * Asserts that entering out-of-bounds metered energy values (< 0.01 or > 999.99 kWh)
     * triggers defensive client validation before any network call is dispatched.
     */
    @Test
    fun viva_defensive_metered_kwh_validation_flow() = runTest(testDispatcher) {
        val scannerVm = OperatorScannerViewModel(verificationRepository)
        advanceUntilIdle()

        // Negative value test
        scannerVm.finalizeEnergyTransfer("664fa10b9c3e2e1a4f001201", -5.0, null)
        advanceUntilIdle()
        assertTrue(scannerVm.uiState.value is ScannerUiState.Rejection)
        assertEquals(0, mockWebServer.requestCount)

        // Zero value test
        scannerVm.finalizeEnergyTransfer("664fa10b9c3e2e1a4f001201", 0.0, null)
        advanceUntilIdle()
        assertTrue(scannerVm.uiState.value is ScannerUiState.Rejection)
        assertEquals(0, mockWebServer.requestCount)

        // Exceeds upper limit 999.99 kWh
        scannerVm.finalizeEnergyTransfer("664fa10b9c3e2e1a4f001201", 1000.0, null)
        advanceUntilIdle()
        assertTrue(scannerVm.uiState.value is ScannerUiState.Rejection)
        assertEquals(0, mockWebServer.requestCount)
    }
}
