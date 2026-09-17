/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for ReservationViewModel checking business rule integration.
 */

package com.sliit.ssmts.reservation_workflow.ui.booking

import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationViewModelTest {

    private lateinit val repository: IReservationRepository
    private lateinit val viewModel: ReservationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(IReservationRepository::class.java)
        viewModel = ReservationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createReservation blocks if date is more than 7 days ahead`() = runTest {
        // Arrange
        val invalidDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 8)
        }.time

        // Act
        viewModel.createReservation("P1", "S1", "SLOT1", invalidDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.createState.value
        assertTrue("State should be Error", state is UiState.Error)
        val errorState = state as UiState.Error
        assertTrue("Error should mention 7 days", errorState.message.contains("7 days"))
        assertTrue("Error code should be RESERVATION_WINDOW_INVALID", errorState.code == "RESERVATION_WINDOW_INVALID")
    }
}
