/**
 * Description: Unit test suite for OperatorScannerViewModel asserting coroutine state transitions
 * between Idle, Verifying, Handshake, and Rejection states during QR verification (FR-M4-06).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IOperatorVerificationRepository
import com.sliit.ssmts.operator_dashboard.util.FastTestQrScenarios
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Validates reactive state transitions in OperatorScannerViewModel under Coroutine Test Dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OperatorScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeOperatorVerificationRepository
    private lateinit var viewModel: OperatorScannerViewModel

    /**
     * Sets up test coroutine dispatcher and initializes fake verification repository and ViewModel.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeOperatorVerificationRepository()
        viewModel = OperatorScannerViewModel(fakeRepository)
    }

    /**
     * Resets Main dispatcher after each test.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Asserts that initial ViewModel state is ScannerUiState.Idle.
     */
    @Test
    fun initialState_isIdle() {
        assertEquals(ScannerUiState.Idle, viewModel.uiState.value)
    }

    /**
     * Asserts that verifying a valid approved token transitions state to ScannerUiState.Handshake.
     */
    @Test
    fun verifyQrToken_validApproved_transitionsToHandshake() = runTest(testDispatcher) {
        val approvedResult = QrVerificationResult(
            isValid = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            prosumerNic = "200012345678",
            stationName = "Peradeniya Agro-Voltaic Hub",
            allocatedBayId = "BAY-02",
            status = ReservationStatus.APPROVED,
            message = "QR token valid. Proceed to physical energy transfer."
        )
        fakeRepository.verificationResponse = NetworkResult.Success(approvedResult)

        viewModel.verifyQrToken(FastTestQrScenarios.APPROVED_VALID_PAYLOAD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Handshake)
        val handshake = state as ScannerUiState.Handshake
        assertEquals("664fa10b9c3e2e1a4f001201", handshake.reservation.reservationId)
        assertEquals("200012345678", handshake.reservation.prosumerNic)
        assertEquals("BAY-02", handshake.reservation.allocatedBayId)
    }

    /**
     * Asserts that verifying an already completed token transitions state to ScannerUiState.Rejection.
     */
    @Test
    fun verifyQrToken_alreadyCompleted_transitionsToRejection() = runTest(testDispatcher) {
        val completedResult = QrVerificationResult(
            isValid = false,
            reservationId = "664fa10b9c3e2e1a4f001202",
            errorCode = "ERR_RESERVATION_ALREADY_COMPLETED",
            message = "This reservation has already been completed and finalized."
        )
        fakeRepository.verificationResponse = NetworkResult.Success(completedResult)

        viewModel.verifyQrToken(FastTestQrScenarios.ALREADY_COMPLETED_PAYLOAD)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("ERR_RESERVATION_ALREADY_COMPLETED", rejection.errorCode)
        assertEquals("This reservation has already been completed and finalized.", rejection.message)
    }

    /**
     * Asserts that network errors dispatch a structured Rejection state.
     */
    @Test
    fun verifyQrToken_networkError_transitionsToRejection() = runTest(testDispatcher) {
        fakeRepository.verificationResponse = NetworkResult.Error("500", "Internal server error")

        viewModel.verifyQrToken("SSMTS-QR|test|payload|hub|time|sig")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("500", rejection.errorCode)
        assertEquals("Internal server error", rejection.message)
    }

    /**
     * Asserts that network exceptions dispatch a structured Rejection state with exception code.
     */
    @Test
    fun verifyQrToken_networkException_transitionsToRejection() = runTest(testDispatcher) {
        fakeRepository.verificationResponse = NetworkResult.Exception(IOException("Socket timeout"))

        viewModel.verifyQrToken("SSMTS-QR|test|payload|hub|time|sig")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("ERR_NETWORK_EXCEPTION", rejection.errorCode)
        assertEquals("Socket timeout", rejection.message)
    }

    /**
     * Asserts that calling verifyQrToken while already verifying ignores the duplicate invocation.
     */
    @Test
    fun verifyQrToken_whileVerifying_ignoresDuplicateCalls() = runTest(testDispatcher) {
        val approvedResult = QrVerificationResult(isValid = true, reservationId = "res1")
        fakeRepository.verificationResponse = NetworkResult.Success(approvedResult)

        viewModel.verifyQrToken("SSMTS-QR|1|2|3|4|5")
        // Second call while in progress
        viewModel.verifyQrToken("SSMTS-QR|1|2|3|4|5")

        advanceUntilIdle()
        assertEquals(1, fakeRepository.verifyCallCount)
    }

    /**
     * Asserts that resetScannerState returns the UI state to ScannerUiState.Idle.
     */
    @Test
    fun resetScannerState_transitionsToIdle() = runTest(testDispatcher) {
        val completedResult = QrVerificationResult(isValid = false, message = "Rejected")
        fakeRepository.verificationResponse = NetworkResult.Success(completedResult)

        viewModel.verifyQrToken("SSMTS-QR|1|2|3|4|5")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ScannerUiState.Rejection)

        viewModel.resetScannerState()
        assertEquals(ScannerUiState.Idle, viewModel.uiState.value)
    }

    /**
     * Asserts that finalizeEnergyTransfer commits power reading and transitions to ScannerUiState.Finalized.
     */
    @Test
    fun finalizeEnergyTransfer_success_transitionsToFinalized() = runTest(testDispatcher) {
        viewModel.finalizeEnergyTransfer("res-fin-1", 24.65, "Nominal transfer")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Finalized)
        val finalized = state as ScannerUiState.Finalized
        assertEquals("res-fin-1", finalized.receipt.reservationId)
        assertEquals(24.65, finalized.receipt.meteredEnergyKwh, 0.001)
        assertTrue(finalized.receipt.isSuccess)
    }

    /**
     * Asserts that invalid metered kWh (< 0.01 or > 999.99) triggers Rejection without calling repository.
     */
    @Test
    fun finalizeEnergyTransfer_invalidKwh_transitionsToRejection() = runTest(testDispatcher) {
        viewModel.finalizeEnergyTransfer("res-fin-1", 0.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("ERR_INVALID_METERED_KWH", rejection.errorCode)
        assertEquals(0, fakeRepository.finalizeCallCount)
    }

    /**
     * Asserts that network errors during finalization dispatch a structured Rejection state.
     */
    @Test
    fun finalizeEnergyTransfer_networkError_transitionsToRejection() = runTest(testDispatcher) {
        fakeRepository.finalizeResponse = NetworkResult.Error("500", "Central database failure")

        viewModel.finalizeEnergyTransfer("res-fin-1", 15.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ScannerUiState.Rejection)
        val rejection = state as ScannerUiState.Rejection
        assertEquals("500", rejection.errorCode)
        assertEquals("Central database failure", rejection.message)
    }

    /**
     * Asserts that duplicate finalization requests while in-flight are ignored.
     */
    @Test
    fun finalizeEnergyTransfer_whileFinalizing_ignoresDuplicateCalls() = runTest(testDispatcher) {
        viewModel.finalizeEnergyTransfer("res-fin-1", 25.0)
        // Second call while in progress
        viewModel.finalizeEnergyTransfer("res-fin-1", 25.0)

        advanceUntilIdle()
        assertEquals(1, fakeRepository.finalizeCallCount)
    }

    /**
     * Test double repository implementation for unit testing OperatorScannerViewModel.
     */
    private class FakeOperatorVerificationRepository : IOperatorVerificationRepository {
        var verificationResponse: NetworkResult<QrVerificationResult> =
            NetworkResult.Success(QrVerificationResult(isValid = true))
        var verifyCallCount = 0

        var finalizeResponse: NetworkResult<FinalizeTransferResult>? = null
        var finalizeCallCount = 0

        override suspend fun verifyScannedQr(qrPayload: String): NetworkResult<QrVerificationResult> {
            verifyCallCount++
            return verificationResponse
        }

        override suspend fun finalizeTransfer(
            reservationId: String,
            meteredKwh: Double,
            notes: String?
        ): NetworkResult<FinalizeTransferResult> {
            finalizeCallCount++
            return finalizeResponse ?: NetworkResult.Success(
                FinalizeTransferResult(
                    isSuccess = true,
                    reservationId = reservationId,
                    meteredEnergyKwh = meteredKwh,
                    finalizedAtIso = "2026-09-18T12:00:00Z",
                    finalizedByOperator = "OP-TEST"
                )
            )
        }
    }
}
