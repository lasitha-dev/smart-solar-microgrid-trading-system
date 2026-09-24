/**
 * Description: Unit test suite verifying swipe-to-refresh coordination, remote reservation
 * cache synchronization, and offline fallback resilience on DashboardViewModel.
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit test validating swipe-to-refresh and offline fallback coordination behavior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardOfflineSyncTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: MockSyncDashboardRepository
    private lateinit var viewModel: DashboardViewModel

    /**
     * Sets up test coroutine dispatcher and mock sync repository before each test.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = MockSyncDashboardRepository()
    }

    /**
     * Resets Main dispatcher after each test.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that refresh() coordinates both syncRemoteReservations() and forced metrics load.
     */
    @Test
    fun refresh_coordinatesReservationSyncAndMetricsRefresh() = runTest(testDispatcher) {
        val initialMetrics = DashboardMetrics(pendingReservationsCount = 2, isOfflineCached = false)
        val refreshedMetrics = DashboardMetrics(pendingReservationsCount = 5, isOfflineCached = false)

        fakeRepository.metricsResult = NetworkResult.Success(initialMetrics)
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        // Prepare refreshed metrics and execute refresh
        fakeRepository.metricsResult = NetworkResult.Success(refreshedMetrics)
        viewModel.refresh()

        // Before coroutines finish, isRefreshing should be true
        assertTrue("isRefreshing must be true during refresh execution", viewModel.isRefreshing.value)

        advanceUntilIdle()

        // Verify syncRemoteReservations was called
        assertTrue("syncRemoteReservations must be invoked on refresh", fakeRepository.syncReservationsCalled)
        assertEquals(1, fakeRepository.syncReservationsCallCount)

        // Verify forced refresh was passed
        assertTrue("forceRefresh must be true on refresh", fakeRepository.lastForceRefreshParam)

        // Verify isRefreshing resets to false
        assertFalse("isRefreshing must reset to false after completion", viewModel.isRefreshing.value)

        // Verify updated metrics
        val state = viewModel.uiState.value as UiState.Success
        assertEquals(5, state.data.pendingReservationsCount)
    }

    /**
     * Verifies that when remote sync fails (offline mode), cached records are preserved and spinner dismissed.
     */
    @Test
    fun refresh_whenOfflineSyncFails_resetsRefreshingAndPreservesCachedState() = runTest(testDispatcher) {
        val cachedMetrics = DashboardMetrics(
            pendingReservationsCount = 3,
            approvedFutureReservationsCount = 7,
            completedTodayCount = 2,
            isOfflineCached = true
        )

        fakeRepository.metricsResult = NetworkResult.Success(cachedMetrics)
        fakeRepository.syncResult = NetworkResult.Error("Network unreachable", "ERR_OFFLINE")

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // Spinner must be dismissed
        assertFalse("isRefreshing must reset to false even if sync fails", viewModel.isRefreshing.value)

        // Cached state must be intact with isOfflineCached = true
        val state = viewModel.uiState.value as UiState.Success
        assertTrue("Offline cached indicator must remain true", state.data.isOfflineCached)
        assertEquals(3, state.data.pendingReservationsCount)
    }

    /**
     * Verifies that exceptions during refresh ensure isRefreshing is reset in finally block.
     */
    @Test
    fun refresh_whenExceptionThrown_resetsRefreshingState() = runTest(testDispatcher) {
        fakeRepository.metricsResult = NetworkResult.Exception(IOException("Socket timeout"))
        fakeRepository.throwExceptionOnSync = true

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse("isRefreshing must be false after exception", viewModel.isRefreshing.value)
    }

    /**
     * Test double repository for tracking sync and metrics invocations.
     */
    private class MockSyncDashboardRepository : IDashboardRepository {
        var metricsResult: NetworkResult<DashboardMetrics> = NetworkResult.Success(DashboardMetrics())
        var syncResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
        var syncReservationsCalled = false
        var syncReservationsCallCount = 0
        var lastForceRefreshParam = false
        var throwExceptionOnSync = false

        /**
         * Emits configured metrics result stream.
         */
        override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
            lastForceRefreshParam = forceRefresh
            emit(metricsResult)
        }

        /**
         * Emits empty cached reservations flow.
         */
        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        /**
         * Emits empty today active reservations flow.
         */
        override fun getTodayActiveReservationsStream(): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        /**
         * Emits empty pending queue flow.
         */
        override fun getPendingQueueReservationsStream(): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        /**
         * Simulates syncing remote reservations with error triggering capabilities.
         */
        override suspend fun syncRemoteReservations(): NetworkResult<Unit> {
            syncReservationsCalled = true
            syncReservationsCallCount++
            if (throwExceptionOnSync) {
                throw IOException("Simulated network outage")
            }
            return syncResult
        }
    }
}
