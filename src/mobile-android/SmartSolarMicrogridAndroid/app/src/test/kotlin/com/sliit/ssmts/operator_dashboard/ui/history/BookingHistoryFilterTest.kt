/**
 * Description: Mandatory unit test suite for Booking History filtering (Section 5 / FR-M4-03.3),
 * asserting exact record counts when filtering a 50-item dataset across all 5 status chips and keywords.
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import com.sliit.ssmts.R
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates search and filter permutations against a controlled 50-item dataset per Rule 5.3.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingHistoryFilterTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: DatasetHistoryRepository
    private lateinit var viewModel: BookingHistoryViewModel

    /**
     * Sets up test coroutine dispatcher and seeds 50 mock reservations repository.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = DatasetHistoryRepository(generate50MockReservations())
    }

    /**
     * Resets Main dispatcher after each test execution.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Asserts that selecting chip 'All' with no query returns all 50 items.
     */
    @Test
    fun filterByAll_returns50Items() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.historyUiState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(50, list.size)
    }

    /**
     * Asserts that selecting chip 'Pending' returns exactly 15 records.
     */
    @Test
    fun filterByPending_returns15Items() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(BookingFilterState.PENDING.filterValue)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(15, list.size)
        assertTrue(list.all { it.status == ReservationStatus.PENDING })
    }

    /**
     * Asserts that selecting chip 'Approved' returns exactly 15 records.
     */
    @Test
    fun filterByApproved_returns15Items() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(BookingFilterState.APPROVED.filterValue)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(15, list.size)
        assertTrue(list.all { it.status == ReservationStatus.APPROVED })
    }

    /**
     * Asserts that selecting chip 'Completed' returns exactly 12 records.
     */
    @Test
    fun filterByCompleted_returns12Items() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(BookingFilterState.COMPLETED.filterValue)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(12, list.size)
        assertTrue(list.all { it.status == ReservationStatus.COMPLETED })
    }

    /**
     * Asserts that selecting chip 'Cancelled' returns exactly 8 records.
     */
    @Test
    fun filterByCancelled_returns8Items() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(BookingFilterState.CANCELLED.filterValue)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(8, list.size)
        assertTrue(list.all { it.status == ReservationStatus.CANCELLED })
    }

    /**
     * Asserts that combining 'All' filter with 'Colombo' keyword search returns exact match count (23 items).
     */
    @Test
    fun filterByStationKeyword_andAllStatus_returnsExactCount() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Colombo")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(23, list.size)
        assertTrue(list.all { it.stationName.contains("Colombo", ignoreCase = true) })
    }

    /**
     * Asserts that combining 'Completed' filter with 'Colombo' keyword search returns exact match count (6 items).
     */
    @Test
    fun filterByStationKeyword_andCompletedStatus_returnsExactCount() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterSelected(BookingFilterState.COMPLETED.filterValue)
        viewModel.onSearchQueryChanged("Colombo")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(6, list.size)
        assertTrue(list.all { it.status == ReservationStatus.COMPLETED && it.stationName.contains("Colombo") })
    }

    /**
     * Asserts that text queries are case-insensitive ("colombo", "COLOMBO", "CoLoMbO" return identical 23 items).
     */
    @Test
    fun filterByStationKeyword_caseInsensitive_returnsExactSameCount() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        for (query in listOf("colombo", "COLOMBO", "CoLoMbO")) {
            viewModel.onSearchQueryChanged(query)
            advanceTimeBy(350L)
            advanceUntilIdle()

            val list = (viewModel.historyUiState.value as UiState.Success).data
            assertEquals("Query '$query' must yield 23 items", 23, list.size)
        }
    }

    /**
     * Asserts that searching by Prosumer NIC returns exact match records.
     */
    @Test
    fun filterByProsumerNicKeyword_returnsExactCount() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        // Search specific individual NIC
        viewModel.onSearchQueryChanged("199010000001")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val singleList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(1, singleList.size)
        assertEquals("RES-001", singleList[0].id)

        // Search prefix shared by items 10 through 19
        viewModel.onSearchQueryChanged("19901000001")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val subsetList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(10, subsetList.size)
    }

    /**
     * Asserts that searching by Reservation ID returns exact matching records.
     */
    @Test
    fun filterByReservationIdKeyword_returnsExactCount() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("RES-005")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("RES-005", list[0].id)
    }

    /**
     * Asserts compound filtering matrix: active "Colombo" search query (23 items) across all 5 chips sequentially.
     */
    @Test
    fun filterCompound_searchColombo_andSwitchAll5Chips() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        // Set active search query
        viewModel.onSearchQueryChanged("Colombo")
        advanceTimeBy(350L)
        advanceUntilIdle()

        // All -> 23
        viewModel.onStatusFilterSelected(BookingFilterState.ALL.filterValue)
        advanceUntilIdle()
        assertEquals(23, (viewModel.historyUiState.value as UiState.Success).data.size)

        // Pending -> 8
        viewModel.onStatusFilterSelected(BookingFilterState.PENDING.filterValue)
        advanceUntilIdle()
        val pendingList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(8, pendingList.size)
        assertTrue(pendingList.all { it.status == ReservationStatus.PENDING })

        // Approved -> 5
        viewModel.onStatusFilterSelected(BookingFilterState.APPROVED.filterValue)
        advanceUntilIdle()
        val approvedList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(5, approvedList.size)
        assertTrue(approvedList.all { it.status == ReservationStatus.APPROVED })

        // Completed -> 6
        viewModel.onStatusFilterSelected(BookingFilterState.COMPLETED.filterValue)
        advanceUntilIdle()
        val completedList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(6, completedList.size)
        assertTrue(completedList.all { it.status == ReservationStatus.COMPLETED })

        // Cancelled -> 4
        viewModel.onStatusFilterSelected(BookingFilterState.CANCELLED.filterValue)
        advanceUntilIdle()
        val cancelledList = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(4, cancelledList.size)
        assertTrue(cancelledList.all { it.status == ReservationStatus.CANCELLED })

        // Return to All -> 23
        viewModel.onStatusFilterSelected(BookingFilterState.ALL.filterValue)
        advanceUntilIdle()
        assertEquals(23, (viewModel.historyUiState.value as UiState.Success).data.size)

        // Clear search query -> 50
        viewModel.onSearchQueryChanged("")
        advanceTimeBy(350L)
        advanceUntilIdle()
        assertEquals(50, (viewModel.historyUiState.value as UiState.Success).data.size)
    }

    /**
     * Asserts that searching for a non-existent keyword returns an empty list without error.
     */
    @Test
    fun filterByNonExistentKeyword_returnsEmptyList() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("NonExistentStationXYZ")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(0, list.size)
    }

    /**
     * Asserts that filtering by a status with a non-matching keyword returns an empty list.
     */
    @Test
    fun filterByStatusWithNonMatchingKeyword_returnsEmptyList() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        // Filter by Cancelled, but search for Kandy (no Cancelled reservations in Kandy)
        viewModel.onStatusFilterSelected(BookingFilterState.CANCELLED.filterValue)
        viewModel.onSearchQueryChanged("Kandy")
        advanceTimeBy(350L)
        advanceUntilIdle()

        val list = (viewModel.historyUiState.value as UiState.Success).data
        assertEquals(0, list.size)
    }

    /**
     * Asserts that rapid keystrokes within 300ms do not filter prematurely until 300ms debounce completes.
     */
    @Test
    fun searchDebounce_rapidInput_doesNotFilterUntil300msElapsed() = runTest(testDispatcher) {
        viewModel = BookingHistoryViewModel(fakeRepository)
        advanceUntilIdle()

        // Initial 50 items
        assertEquals(50, (viewModel.historyUiState.value as UiState.Success).data.size)

        // Type 'Col' at t=0
        viewModel.onSearchQueryChanged("Col")
        advanceTimeBy(150L)
        // Still 50 items (debounce not expired)
        assertEquals(50, (viewModel.historyUiState.value as UiState.Success).data.size)

        // Type 'Colombo' at t=150 (resets debounce timer)
        viewModel.onSearchQueryChanged("Colombo")
        advanceTimeBy(200L)
        // 200ms after second keystroke, still not 300ms
        assertEquals(50, (viewModel.historyUiState.value as UiState.Success).data.size)

        // Advance remaining 150ms (total 350ms since second keystroke)
        advanceTimeBy(150L)
        advanceUntilIdle()
        // Now debounced query triggers and produces 23 items
        assertEquals(23, (viewModel.historyUiState.value as UiState.Success).data.size)
    }

    /**
     * Asserts that BookingFilterState.fromChipId accurately resolves all 5 chip states.
     */
    @Test
    fun bookingFilterState_fromChipId_resolvesExpectedStates() {
        assertEquals(BookingFilterState.ALL, BookingFilterState.fromChipId(R.id.chipFilterAll))
        assertEquals(BookingFilterState.PENDING, BookingFilterState.fromChipId(R.id.chipFilterPending))
        assertEquals(BookingFilterState.APPROVED, BookingFilterState.fromChipId(R.id.chipFilterApproved))
        assertEquals(BookingFilterState.COMPLETED, BookingFilterState.fromChipId(R.id.chipFilterCompleted))
        assertEquals(BookingFilterState.CANCELLED, BookingFilterState.fromChipId(R.id.chipFilterCancelled))
        assertEquals(BookingFilterState.ALL, BookingFilterState.fromChipId(-9999))
    }

    /**
     * Generates a controlled dataset of exactly 50 reservation items:
     * - 15 Pending (8 Colombo, 7 Kandy)
     * - 15 Approved (5 Colombo, 10 Galle)
     * - 12 Completed (6 Colombo, 6 Jaffna)
     * - 8 Cancelled (4 Colombo, 4 Matara)
     */
    private fun generate50MockReservations(): List<Reservation> {
        val list = mutableListOf<Reservation>()
        var idCounter = 1

        /**
         * Helper closure to append a batch of mock reservations with designated status and station name.
         */
        fun addItems(count: Int, status: ReservationStatus, station: String) {
            repeat(count) {
                val id = "RES-${idCounter.toString().padStart(3, '0')}"
                val nic = "1990${(10000000 + idCounter)}"
                list.add(
                    Reservation(
                        id = id,
                        prosumerNic = nic,
                        stationName = station,
                        scheduledTimeMillis = 1773720000000L + (idCounter * 60000L),
                        allocatedBay = "BAY-0${(idCounter % 4) + 1}",
                        estimatedKwh = 25.0 + (idCounter % 15),
                        meteredKwh = if (status == ReservationStatus.COMPLETED) 24.8 + (idCounter % 15) else null,
                        status = status,
                        qrPayload = "SSMTS-QR|$id|$nic|STATION|1000|SIG"
                    )
                )
                idCounter++
            }
        }

        addItems(8, ReservationStatus.PENDING, "Colombo Central Hub")
        addItems(7, ReservationStatus.PENDING, "Kandy Station")
        addItems(5, ReservationStatus.APPROVED, "Colombo Central Hub")
        addItems(10, ReservationStatus.APPROVED, "Galle Hub")
        addItems(6, ReservationStatus.COMPLETED, "Colombo Central Hub")
        addItems(6, ReservationStatus.COMPLETED, "Jaffna Hub")
        addItems(4, ReservationStatus.CANCELLED, "Colombo Central Hub")
        addItems(4, ReservationStatus.CANCELLED, "Matara Station")

        return list
    }

    /**
     * Test repository filtering in-memory dataset according to SQLite query semantics.
     */
    private class DatasetHistoryRepository(
        private val dataset: List<Reservation>
    ) : IDashboardRepository {

        /**
         * Emits filtered dataset matching status and search query terms.
         */
        override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> = flow {
            val normalizedStatus = if (status.isNullOrBlank() || status.equals("All", ignoreCase = true)) null else status.trim()
            val normalizedSearch = if (search.isNullOrBlank()) null else search.trim()

            val filtered = dataset.filter { item ->
                val matchesStatus = normalizedStatus == null || item.status.value.equals(normalizedStatus, ignoreCase = true)
                val matchesSearch = normalizedSearch == null ||
                        item.stationName.contains(normalizedSearch, ignoreCase = true) ||
                        item.prosumerNic.contains(normalizedSearch, ignoreCase = true) ||
                        item.id.contains(normalizedSearch, ignoreCase = true)
                matchesStatus && matchesSearch
            }
            emit(filtered)
        }

        /**
         * Emits mock dashboard metrics stream.
         */
        override fun getDashboardMetricsStream(forceRefresh: Boolean, operatorId: String?): Flow<NetworkResult<DashboardMetrics>> = flow {
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
         * Simulates syncing remote reservations.
         */
        override suspend fun syncRemoteReservations(operatorId: String?): NetworkResult<Unit> {
            return NetworkResult.Success(Unit)
        }

        override suspend fun approveReservation(reservationId: String, operatorId: String?): NetworkResult<Reservation> {
            return NetworkResult.Success(
                Reservation(
                    id = reservationId,
                    prosumerNic = "200012345678",
                    stationName = "Test Station",
                    scheduledTimeMillis = System.currentTimeMillis(),
                    allocatedBay = "BAY-01",
                    status = com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus.APPROVED,
                    estimatedKwh = 10.0
                )
            )
        }

        override suspend fun rejectReservation(reservationId: String, reason: String?, operatorId: String?): NetworkResult<Reservation> {
            return NetworkResult.Success(
                Reservation(
                    id = reservationId,
                    prosumerNic = "200012345678",
                    stationName = "Test Station",
                    scheduledTimeMillis = System.currentTimeMillis(),
                    allocatedBay = "BAY-01",
                    status = com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus.CANCELLED,
                    estimatedKwh = 10.0
                )
            )
        }
    }
}
