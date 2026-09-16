/**
 * Description: Unit test suite for DashboardViewModel validating operational booking feeds (FR-M4-02),
 * tab selection transitions, and reactive partitioning of today's active slots and pending reservations.
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests asserting feed streaming, tab transitions, and reactive collection in DashboardViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingsFeedViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFeedDashboardRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFeedDashboardRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Asserts that default tab is TODAY_ACTIVE upon initialization.
     */
    @Test
    fun initialTab_defaultsToTodayActive() = runTest(testDispatcher) {
        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(DashboardFeedTab.TODAY_ACTIVE, viewModel.selectedFeedTab.value)
    }

    /**
     * Asserts that todayActiveBookings accurately streams active approved reservations.
     */
    @Test
    fun todayActiveBookings_streamsApprovedReservations() = runTest(testDispatcher) {
        val activeRes = createTestReservation(id = "RES-ACTIVE-1", status = ReservationStatus.APPROVED)
        fakeRepository.todayActiveFlow.value = listOf(activeRes)

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(1, viewModel.todayActiveBookings.value.size)
        assertEquals("RES-ACTIVE-1", viewModel.todayActiveBookings.value[0].id)
        assertEquals(ReservationStatus.APPROVED, viewModel.todayActiveBookings.value[0].status)
    }

    /**
     * Asserts that pendingQueueBookings accurately streams pending reservations awaiting operator validation.
     */
    @Test
    fun pendingQueueBookings_streamsPendingReservations() = runTest(testDispatcher) {
        val pending1 = createTestReservation(id = "RES-PENDING-1", status = ReservationStatus.PENDING)
        val pending2 = createTestReservation(id = "RES-PENDING-2", status = ReservationStatus.PENDING)
        fakeRepository.pendingQueueFlow.value = listOf(pending1, pending2)

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        assertEquals(2, viewModel.pendingQueueBookings.value.size)
        assertEquals("RES-PENDING-1", viewModel.pendingQueueBookings.value[0].id)
        assertEquals("RES-PENDING-2", viewModel.pendingQueueBookings.value[1].id)
    }

    /**
     * Asserts that feedReservations dynamically emits list corresponding to selected feed tab.
     */
    @Test
    fun selectFeedTab_switchesFeedReservationsCorrectly() = runTest(testDispatcher) {
        val activeRes = createTestReservation(id = "ACTIVE-1", status = ReservationStatus.APPROVED)
        val pendingRes = createTestReservation(id = "PENDING-1", status = ReservationStatus.PENDING)

        fakeRepository.todayActiveFlow.value = listOf(activeRes)
        fakeRepository.pendingQueueFlow.value = listOf(pendingRes)

        viewModel = DashboardViewModel(fakeRepository)
        advanceUntilIdle()

        // Defaults to TODAY_ACTIVE
        assertEquals(DashboardFeedTab.TODAY_ACTIVE, viewModel.selectedFeedTab.value)
        assertEquals(1, viewModel.feedReservations.value.size)
        assertEquals("ACTIVE-1", viewModel.feedReservations.value[0].id)

        // Switch to PENDING_QUEUE
        viewModel.selectFeedTab(DashboardFeedTab.PENDING_QUEUE)
        advanceUntilIdle()

        assertEquals(DashboardFeedTab.PENDING_QUEUE, viewModel.selectedFeedTab.value)
        assertEquals(1, viewModel.feedReservations.value.size)
        assertEquals("PENDING-1", viewModel.feedReservations.value[0].id)

        // Switch back to TODAY_ACTIVE
        viewModel.selectFeedTab(DashboardFeedTab.TODAY_ACTIVE)
        advanceUntilIdle()

        assertEquals(DashboardFeedTab.TODAY_ACTIVE, viewModel.selectedFeedTab.value)
        assertEquals("ACTIVE-1", viewModel.feedReservations.value[0].id)
    }

    private fun createTestReservation(
        id: String,
        status: ReservationStatus = ReservationStatus.APPROVED
    ): Reservation {
        return Reservation(
            id = id,
            prosumerNic = "199412345678",
            stationName = "Colombo Central Hub",
            scheduledTimeMillis = System.currentTimeMillis() + 3600000L,
            allocatedBay = "BAY-02",
            estimatedKwh = 35.0,
            meteredKwh = null,
            status = status,
            qrPayload = "SSMTS-QR|$id|199412345678|STATION|1000|SIG"
        )
    }

    /**
     * Test double repository for feed state verification.
     */
    private class FakeFeedDashboardRepository : IDashboardRepository {
        val todayActiveFlow = MutableStateFlow<List<Reservation>>(emptyList())
        val pendingQueueFlow = MutableStateFlow<List<Reservation>>(emptyList())

        override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
            emit(NetworkResult.Success(DashboardMetrics()))
        }

        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            emit(emptyList())
        }

        override fun getTodayActiveReservationsStream(): Flow<List<Reservation>> {
            return todayActiveFlow
        }

        override fun getPendingQueueReservationsStream(): Flow<List<Reservation>> {
            return pendingQueueFlow
        }

        override suspend fun syncRemoteReservations(): NetworkResult<Unit> {
            return NetworkResult.Success(Unit)
        }
    }
}
