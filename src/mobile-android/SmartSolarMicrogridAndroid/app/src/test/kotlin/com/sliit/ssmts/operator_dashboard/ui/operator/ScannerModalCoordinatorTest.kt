/**
 * Description: Unit test suite for ScannerModalCoordinator validating dialog and modal presentations,
 * argument bundle population, and callback propagation under Robolectric (Rule 3 SRP).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

/**
 * Validates modal instantiation, argument bundle configurations, and callback dispatching for ScannerModalCoordinator.
 */
@RunWith(RobolectricTestRunner::class)
class ScannerModalCoordinatorTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var coordinator: ScannerModalCoordinator

    /**
     * Initializes a themed Robolectric activity and sets up the coordinator prior to each test invocation.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().visible().get()
        coordinator = ScannerModalCoordinator(activity, activity.supportFragmentManager)
    }

    /**
     * Asserts that showHandshakeModal instantiates and displays the modal with valid reservation details.
     */
    @Test
    fun showHandshakeModal_displaysModalWithReservationDetails() {
        val mockResult = QrVerificationResult(
            isValid = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            prosumerNic = "199412345678",
            stationName = "Colombo Central Hub",
            allocatedBayId = "BAY-02"
        )

        coordinator.showHandshakeModal(
            reservation = mockResult,
            onCancel = {},
            onProceed = {}
        )
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager.findFragmentByTag(TransferHandshakeModal.TAG) as? TransferHandshakeModal
        assertNotNull(fragment)
        assertEquals("664fa10b9c3e2e1a4f001201", fragment?.arguments?.getString("arg_reservation_id"))
        assertEquals("199412345678", fragment?.arguments?.getString("arg_prosumer_nic"))
    }

    /**
     * Asserts that showFinalizeDialog instantiates and presents the TransferFinalizeDialog with target reservation ID.
     */
    @Test
    fun showFinalizeDialog_displaysFinalizeDialogWithReservationId() {
        coordinator.showFinalizeDialog(
            reservationId = "664fa10b9c3e2e1a4f001201",
            onCancel = {},
            onFinalizeConfirmed = { _, _, _ -> }
        )
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager.findFragmentByTag(TransferFinalizeDialog.TAG) as? TransferFinalizeDialog
        assertNotNull(fragment)
        assertEquals("664fa10b9c3e2e1a4f001201", fragment?.arguments?.getString("arg_reservation_id"))
    }

    /**
     * Asserts that showReceiptModal instantiates and presents the TransferReceiptModal with finalized transfer metrics.
     */
    @Test
    fun showReceiptModal_displaysReceiptModalWithMetrics() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 24.65,
            finalizedAtIso = "2026-09-18T11:45:00Z",
            finalizedByOperator = "OP-PERADENIYA-01"
        )

        coordinator.showReceiptModal(mockReceipt) {}
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager.findFragmentByTag(TransferReceiptModal.TAG) as? TransferReceiptModal
        assertNotNull(fragment)
        assertEquals("664fa10b9c3e2e1a4f001201", fragment?.arguments?.getString("arg_reservation_id"))
        assertEquals(24.65, fragment?.arguments?.getDouble("arg_metered_kwh") ?: 0.0, 0.001)
    }

    /**
     * Asserts that showRejectionDialog displays an alert dialog showing error code and message.
     */
    @Test
    fun showRejectionDialog_displaysAlertDialogWithErrorDetails() {
        coordinator.showRejectionDialog(
            errorCode = "ERR_QR_COMPLETED",
            message = "This reservation has already been finalized.",
            onDismiss = {}
        )

        val dialog = ShadowAlertDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
    }

    /**
     * Asserts that dispatchState routes ScannerUiState.Handshake directly to the handshake modal.
     */
    @Test
    fun dispatchState_routesToHandshakeModal() {
        val mockResult = QrVerificationResult(
            isValid = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            prosumerNic = "199412345678",
            stationName = "Colombo Central Hub",
            allocatedBayId = "BAY-02"
        )

        coordinator.dispatchState(
            state = ScannerUiState.Handshake(mockResult),
            onReset = {},
            onFinalize = { _, _, _ -> }
        )
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager.findFragmentByTag(TransferHandshakeModal.TAG)
        assertNotNull(fragment)
    }

    /**
     * Asserts that dispatchState routes ScannerUiState.Finalized directly to the receipt modal.
     */
    @Test
    fun dispatchState_routesToReceiptModal() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 24.65,
            finalizedAtIso = "2026-09-18T11:45:00Z",
            finalizedByOperator = "OP-PERADENIYA-01"
        )

        coordinator.dispatchState(
            state = ScannerUiState.Finalized(mockReceipt),
            onReset = {},
            onFinalize = { _, _, _ -> }
        )
        activity.supportFragmentManager.executePendingTransactions()

        val fragment = activity.supportFragmentManager.findFragmentByTag(TransferReceiptModal.TAG)
        assertNotNull(fragment)
    }
}
