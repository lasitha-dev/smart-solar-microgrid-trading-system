/**
 * Description: Unit test suite for BookingHistoryViewModel validating 300ms search debouncing,
 * distinct query deduplication, status chip filtering, and repository state streaming (FR-M4-03.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

/**
 * Unit tests asserting debounce timing, filter dispatch, and reactive collection in BookingHistoryViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingHistoryViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHistoryDashboardRepository
    private lateinit var viewModel: BookingHistoryViewModel

    /**
     * Sets up test coroutine dispatcher and initializes mock repository before each test.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHistoryDashboardRepository()
    }

    /**
     * Resets Main dispatcher after each test.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Asserts that initial state loads all reservations with 'All' filter and empty search query.
     */
    @Test
    fun initialState_loadsAllReservationsInSuccessState() = runTest(testDispatcher) {
        val r1 = createReservation(id = "RES-1", status = ReservationStatus.PENDING)
        val r2 = createReservation(id = "RES-2", status = ReservationStatus.APPROVED)
        fakeRepository.mockReservations = listOf(r1, r2)

        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals("All", viewModel.selectedStatus.value)

        assertTrue(viewModel.historyUiState.value is UiState.Success)
        val successList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(2, successList.size)
    }

    /**
     * Asserts that search query changes are debounced by 300ms before dispatching to repository (FR-M4-03.2).
     */
    @Test
    fun searchQuery_debounces300ms_beforeTriggeringQuery() = runTest(testDispatcher) {
        val r1 = createReservation(id = "RES-1", stationName = "Colombo Central Hub")
        val r2 = createReservation(id = "RES-2", stationName = "Kandy Station")
        fakeRepository.mockReservations = listOf(r1, r2)

        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        val initialCallCount = fakeRepository.queryCallCount

        // Change query to 'Colombo'
        viewModel.onSearchQueryChanged("Colombo")
        assertEquals("Colombo", viewModel.searchQuery.value)

        // Advance 200ms: debounce window has NOT elapsed
        advanceTimeBy(200L)
        assertEquals(initialCallCount, fakeRepository.queryCallCount)

        // Advance another 150ms (total 350ms): debounce window HAS elapsed
        advanceTimeBy(150L)
        advanceUntilIdle()

        assertEquals("Colombo", fakeRepository.lastQueriedSearch)
        assertTrue(viewModel.historyUiState.value is UiState.Success)
        val resultList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(1, resultList.size)
        assertEquals("RES-1", resultList[0].id)
    }

    /**
     * Asserts that repeating the identical search query does not re-dispatch to the repository.
     */
    @Test
    fun searchQuery_identicalInput_doesNotReTrigger() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Kandy")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val callsAfterFirst = fakeRepository.queryCallCount

        // Dispatch identical text again
        viewModel.onSearchQueryChanged("Kandy")
        advanceTimeBy(350L)
        advanceUntilIdle()

        assertEquals(callsAfterFirst, fakeRepository.queryCallCount)
    }

    /**
     * Asserts that selecting a status filter chip updates results immediately without debounce.
     */
    @Test
    fun statusFilter_updatesImmediatelyWithoutDebounce() = runTest(testDispatcher) {
        val pendingRes = createReservation(id = "RES-PENDING", status = ReservationStatus.PENDING)
        val completedRes = createReservation(id = "RES-COMPLETED", status = ReservationStatus.COMPLETED)
        fakeRepository.mockReservations = listOf(pendingRes, completedRes)

        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected("Completed")
        advanceUntilIdle()

        assertEquals("Completed", fakeRepository.lastQueriedStatus)
        assertTrue(viewModel.historyUiState.value is UiState.Success)
        val resultList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(1, resultList.size)
        assertEquals("RES-COMPLETED", resultList[0].id)
    }

    /**
     * Asserts that refresh invokes repository.syncRemoteReservations() and manages isRefreshing state.
     */
    @Test
    fun refresh_triggersRemoteSync() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(fakeRepository.syncCalled)
        assertFalse(viewModel.isRefreshing.value)
    }

    private fun createReservation(
        id: String,
        stationName: String = "Colombo Central Hub",
        status: ReservationStatus = ReservationStatus.APPROVED
    ): Reservation {
        return Reservation(
            id = id,
            prosumerNic = "199412345678",
            stationName = stationName,
            scheduledTimeMillis = 1773720000000L,
            allocatedBay = "BAY-03",
            estimatedKwh = 30.0,
            meteredKwh = null,
            status = status,
            qrPayload = "SSMTS-QR|$id|199412345678|STATION|1000|SIG"
        )
    }

    /**
     * Test double repository for verifying search and filter queries.
     */
    private class FakeHistoryDashboardRepository : IDashboardRepository {
        var lastQueriedStatus: String? = null
        var lastQueriedSearch: String? = null
        var queryCallCount: Int = 0
        var syncCalled: Boolean = false
        var mockReservations: List<Reservation> = emptyList()

        /**
         * Emits filtered cached reservations stream.
         */
        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            lastQueriedStatus = status
            lastQueriedSearch = search
            queryCallCount++
            val filtered = mockReservations.filter { item ->
                val matchesStatus = status.isNullOrBlank() ||
                        status.equals("All", ignoreCase = true) ||
                        item.status.value.equals(status, ignoreCase = true)
                val matchesSearch = search.isNullOrBlank() ||
                        item.stationName.contains(search, ignoreCase = true) ||
                        item.prosumerNic.contains(search, ignoreCase = true) ||
                        item.id.contains(search, ignoreCase = true)
                matchesStatus && matchesSearch
            }
            emit(filtered)
        }

        /**
         * Emits mock dashboard metrics stream.
         */
        override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
            emit(NetworkResult.Success(DashboardMetrics()))
        }

        /**
         * Emits empty today active reservations stream.
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
         * Records remote sync invocation.
         */
        override suspend fun syncRemoteReservations(): NetworkResult<Unit> {
            syncCalled = true
            return NetworkResult.Success(Unit)
        }
    }
}
