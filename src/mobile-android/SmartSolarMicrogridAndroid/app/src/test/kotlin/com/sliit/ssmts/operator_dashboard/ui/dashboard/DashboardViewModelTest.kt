/**
 * Description: Mandatory unit test suite for DashboardViewModel verifying Coroutine Test Dispatcher
 * state transitions from Loading to Success, count precision, error handling, and refresh flow.
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
     * Verifies that refresh invokes forced synchronization on repository.
     */
    @Test
    fun refresh_triggersForcedRefreshOnRepository() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Success(DashboardMetrics())

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue("Forced refresh must be dispatched to repository", fakeRepository.lastForceRefreshRequested)
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
     * Test double implementation of IDashboardRepository for ViewModel verification.
     */
    private class FakeDashboardRepository : IDashboardRepository {
        var metricsResult: NetworkResult<DashboardMetrics> = NetworkResult.Success(DashboardMetrics())
        var lastForceRefreshRequested: Boolean = false

        override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
            lastForceRefreshRequested = forceRefresh
            emit(metricsResult)
        }

        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        override fun getTodayActiveReservationsStream(): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        override fun getPendingQueueReservationsStream(): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        override suspend fun syncRemoteReservations(): NetworkResult<Unit> {
            return NetworkResult.Success(Unit)
        }
    }
}
