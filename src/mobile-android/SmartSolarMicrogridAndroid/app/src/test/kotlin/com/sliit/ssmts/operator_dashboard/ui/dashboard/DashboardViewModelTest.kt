/**
 * Description: Mandatory unit test suite for DashboardViewModel verifying Coroutine Test Dispatcher
 * state transitions from Loading to Success, count precision, error handling, and refresh flow.
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import com.sliit.ssmts.operator_dashboard.domain.model.ActiveSpotlightReservation
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.ui.common.isError
import com.sliit.ssmts.operator_dashboard.ui.common.isLoading
import com.sliit.ssmts.operator_dashboard.ui.common.isSuccess
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import com.sliit.ssmts.operator_dashboard.util.TransferSyncNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit test validating DashboardViewModel coroutine flows and UI state emissions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDashboardRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDashboardRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Asserts that UI state transitions from Loading -> Success and that counter counts match API responses.
     */
    @Test
    fun initialState_transitionsFromLoadingToSuccess_andCountsMatch() = runTest(testDispatcher) {
        val mockMetrics = DashboardMetrics(
            pendingReservationsCount = 5,
            approvedFutureReservationsCount = 12,
            completedTodayCount = 8,
            activeSpotlight = ActiveSpotlightReservation(
                reservationId = "RES-SPOT-1",
                stationName = "Colombo Hub",
                allocatedBayId = "BAY-03",
                scheduledDateTimeIso = "2026-09-17T10:00:00Z",
                scheduledTimeMillis = 1000000L,
                status = ReservationStatus.APPROVED,
                estimatedKwh = 35.50
            ),
            isOfflineCached = false
        )
        fakeRepository.metricsResult = NetworkResult.Success(mockMetrics)

        viewModel = DashboardViewModel(fakeRepository)

        // Initial state before dispatcher advances should be Loading
        assertTrue("Initial state must be Loading", viewModel.uiState.value.isLoading)

        // Advance coroutines
        advanceUntilIdle()

        // State must now be Success with matching counts
        assertTrue("State must transition to Success", viewModel.uiState.value.isSuccess)

        val successData = (viewModel.uiState.value as UiState.Success).data
        assertEquals(5, successData.pendingReservationsCount)
        assertEquals(12, successData.approvedFutureReservationsCount)
        assertEquals(8, successData.completedTodayCount)
        assertNotNull(successData.activeSpotlight)
        assertEquals("Colombo Hub", successData.activeSpotlight?.stationName)
        assertEquals(35.50, successData.activeSpotlight?.estimatedKwh ?: 0.0, 0.001)
    }

    /**
     * Verifies that error response from repository transitions state to UiState.Error.
     */
    @Test
    fun loadMetrics_repositoryEmitsError_transitionsToErrorState() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Error(
            message = "Server error occurred",
            code = "HTTP_500"
        )

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isError)
        val errorState = viewModel.uiState.value as UiState.Error
        assertEquals("Server error occurred", errorState.message)
        assertEquals("HTTP_500", errorState.errorCode)
    }

    /**
     * Verifies that connection exceptions transition state to UiState.Error.
     */
    @Test
    fun loadMetrics_repositoryEmitsException_transitionsToErrorState() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Exception(
            IOException("Connection refused")
        )

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isError)
        val errorState = viewModel.uiState.value as UiState.Error
        assertEquals("Connection refused", errorState.message)
    }

    /**
     * Verifies that error recovery succeeds when reloading metrics after an initial error.
     */
    @Test
    fun errorRecovery_afterFailure_reloadsToSuccess() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Error(code = "ERR_NET", message = "Network down")

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isError)

        // Update repository with successful metrics and trigger reload
        val recoveredMetrics = DashboardMetrics(pendingReservationsCount = 7, completedTodayCount = 3)
        fakeRepository.metricsResult = NetworkResult.Success(recoveredMetrics)

        viewModel.loadMetrics(forceRefresh = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        val successData = (viewModel.uiState.value as UiState.Success).data
        assertEquals(7, successData.pendingReservationsCount)
        assertEquals(3, successData.completedTodayCount)
    }

    /**
     * Verifies that refresh invokes forced synchronization on repository and updates isRefreshing indicator.
     */
    @Test
    fun refresh_triggersForcedRefreshOnRepository() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Success(DashboardMetrics())

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse("isRefreshing must initially be false", viewModel.isRefreshing.value)

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse("isRefreshing must return to false after completion", viewModel.isRefreshing.value)
        assertTrue("syncRemoteReservations must be invoked", fakeRepository.syncRemoteCalled)
        assertTrue("Forced refresh must be dispatched to repository", fakeRepository.lastForceRefreshRequested)
    }

    /**
     * Verifies that switching operational feed tabs toggles between today's active slots and the pending queue.
     */
    @Test
    fun feedTabs_switchingTogglesBetweenActiveAndPendingFeeds() = runTest(testDispatcher) {
        val res1 = Reservation(id = "RES-ACTIVE-1", prosumerNic = "200012345678", stationName = "Hub 1", scheduledTimeMillis = 1000L, allocatedBay = "BAY-01", estimatedKwh = 20.0, status = ReservationStatus.APPROVED)
        val res2 = Reservation(id = "RES-ACTIVE-2", prosumerNic = "200012345679", stationName = "Hub 2", scheduledTimeMillis = 2000L, allocatedBay = "BAY-02", estimatedKwh = 25.0, status = ReservationStatus.APPROVED)
        val res3 = Reservation(id = "RES-PENDING-1", prosumerNic = "200012345680", stationName = "Hub 3", scheduledTimeMillis = 3000L, allocatedBay = "BAY-03", estimatedKwh = 30.0, status = ReservationStatus.PENDING)

        fakeRepository.todayActiveList = listOf(res1, res2)
        fakeRepository.pendingQueueList = listOf(res3)

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        // Default tab is TODAY_ACTIVE
        assertEquals(DashboardFeedTab.TODAY_ACTIVE, viewModel.selectedFeedTab.value)
        assertEquals(2, viewModel.feedReservations.value.size)
        assertEquals("RES-ACTIVE-1", viewModel.feedReservations.value[0].id)

        // Switch to PENDING_QUEUE
        viewModel.selectFeedTab(DashboardFeedTab.PENDING_QUEUE)
        advanceUntilIdle()

        assertEquals(DashboardFeedTab.PENDING_QUEUE, viewModel.selectedFeedTab.value)
        assertEquals(1, viewModel.feedReservations.value.size)
        assertEquals("RES-PENDING-1", viewModel.feedReservations.value[0].id)

        // Switch back to TODAY_ACTIVE
        viewModel.selectFeedTab(DashboardFeedTab.TODAY_ACTIVE)
        advanceUntilIdle()

        assertEquals(DashboardFeedTab.TODAY_ACTIVE, viewModel.selectedFeedTab.value)
        assertEquals(2, viewModel.feedReservations.value.size)
    }

    /**
     * Verifies that offline cached flag is propagated to presentation model.
     */
    @Test
    fun loadMetrics_offlineCachedState_preservesOfflineFlag() = runTest(testDispatcher) {
        val cachedMetrics = DashboardMetrics(
            pendingReservationsCount = 2,
            approvedFutureReservationsCount = 3,
            completedTodayCount = 1,
            isOfflineCached = true
        )
        fakeRepository.metricsResult = NetworkResult.Success(cachedMetrics)

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        val successData = (viewModel.uiState.value as UiState.Success).data
        assertTrue("isOfflineCached flag must be true", successData.isOfflineCached)
    }

    /**
     * Asserts that receiving a TransferSyncNotifier event triggers a forced dashboard metrics refresh (FR-M4-07.4).
     */
    @Test
    fun transferCompletedNotification_triggersForcedMetricsReload() = runTest(testDispatcher) {
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.lastForceRefreshRequested = false

        TransferSyncNotifier.notifyTransferCompleted("RES-FINAL-01", 24.65)
        advanceUntilIdle()

        assertTrue("Transfer completion must trigger forced reload", fakeRepository.lastForceRefreshRequested)
    }

    /**
     * Asserts that ViewModel Factory constructs DashboardViewModel cleanly and rejects foreign classes.
     */
    @Test
    fun factory_constructsViewModelAndRejectsForeignClasses() {
        val factory = DashboardViewModel.Factory(fakeRepository)
        val created = factory.create(DashboardViewModel::class.java)

        assertNotNull(created)

        try {
            factory.create(ForeignTestViewModel::class.java)
            fail("Expected IllegalArgumentException for foreign ViewModel class")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unknown ViewModel class") == true)
        }
    }

    private class ForeignTestViewModel : ViewModel()

    /**
     * Test double implementation of IDashboardRepository for ViewModel verification.
     */
    private class FakeDashboardRepository : IDashboardRepository {
        var metricsResult: NetworkResult<DashboardMetrics> = NetworkResult.Success(DashboardMetrics())
        var lastForceRefreshRequested: Boolean = false
        var syncRemoteCalled: Boolean = false
        var todayActiveList: List<Reservation> = emptyList()
        var pendingQueueList: List<Reservation> = emptyList()

        override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
            lastForceRefreshRequested = forceRefresh
            emit(metricsResult)
        }

        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        override fun getTodayActiveReservationsStream(): Flow<List<Reservation>> = flow {
            emit(todayActiveList)
        }

        override fun getPendingQueueReservationsStream(): Flow<List<Reservation>> = flow {
            emit(pendingQueueList)
        }

        override suspend fun syncRemoteReservations(): NetworkResult<Unit> {
            syncRemoteCalled = true
            return NetworkResult.Success(Unit)
        }
    }
}
