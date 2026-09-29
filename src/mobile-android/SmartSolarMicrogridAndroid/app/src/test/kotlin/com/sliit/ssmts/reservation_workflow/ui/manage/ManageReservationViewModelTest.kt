/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for ManageReservationViewModel enforcing 12-hour and 7-day business rules using MockK.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ManageReservationViewModelTest {

    private lateinit var repository: IReservationRepository
    private lateinit var viewModel: ManageReservationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk<IReservationRepository>()
        viewModel = ManageReservationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancelReservation emits error when less than 12 hours remain`() = runTest {
        // Arrange: Scheduled 5 hours from now (< 12 hours)
        val nearScheduledTime = Date(System.currentTimeMillis() + (5 * 3600 * 1000L))
        val reservation = Reservation(
            id = "RES-001",
            prosumerId = "P1",
            stationId = "S1",
            bookingSlotId = "B1",
            scheduledDateTime = nearScheduledTime,
            status = ReservationStatus.PENDING,
            qrCode = null
        )

        // Act
        viewModel.cancelReservation(reservation, "Change of plans")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.cancelState.value
        assertTrue("State should be Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals("MODIFICATION_WINDOW_CLOSED", errorState.code)
        assertTrue(errorState.message.contains("12 hours"))
    }

    @Test
    fun `cancelReservation emits success when more than 12 hours remain`() = runTest {
        // Arrange: Scheduled 24 hours from now (> 12 hours)
        val futureTime = Date(System.currentTimeMillis() + (24 * 3600 * 1000L))
        val reservation = Reservation(
            id = "RES-002",
            prosumerId = "P1",
            stationId = "S1",
            bookingSlotId = "B1",
            scheduledDateTime = futureTime,
            status = ReservationStatus.PENDING,
            qrCode = null
        )

        coEvery { repository.cancelReservation("RES-002", "No longer needed") } returns NetworkResult.Success(Unit)

        // Act
        viewModel.cancelReservation(reservation, "No longer needed")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.cancelState.value
        assertTrue("State should be Success", state is UiState.Success)
    }

    @Test
    fun `updateReservation emits error when new date exceeds 7 days`() = runTest {
        // Arrange: Existing booking valid (> 12 hours)
        val validCurrentTime = Date(System.currentTimeMillis() + (24 * 3600 * 1000L))
        val reservation = Reservation(
            id = "RES-003",
            prosumerId = "P1",
            stationId = "S1",
            bookingSlotId = "B1",
            scheduledDateTime = validCurrentTime,
            status = ReservationStatus.PENDING,
            qrCode = null
        )

        // New date is 10 days ahead
        val invalidNewDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 10)
        }.time

        // Act
        viewModel.updateReservation(reservation, "B2", invalidNewDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.manageState.value
        assertTrue("State should be Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertEquals("RESERVATION_WINDOW_INVALID", errorState.code)
        assertTrue(errorState.message.contains("7 days"))
    }
}
