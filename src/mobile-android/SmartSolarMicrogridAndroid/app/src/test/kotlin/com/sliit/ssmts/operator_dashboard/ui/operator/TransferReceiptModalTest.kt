/**
 * Description: UI unit test suite for TransferReceiptModal verifying view binding, metric formatting,
 * status badge rendering, and button callbacks under Robolectric (FR-M4-07.4).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/**
 * Validates view binding, delivered power formatting, and callback triggers on TransferReceiptModal.
 */
@RunWith(RobolectricTestRunner::class)
class TransferReceiptModalTest {

    private lateinit var activity: AppCompatActivity

    /**
     * Initializes the Robolectric test activity with the application theme prior to modal instantiation.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().visible().get()
    }

    /**
     * Asserts that modal displays accurate delivered energy, reservation reference, operator ID, and timestamp.
     */
    @Test
    fun modalInflation_populatesReceiptMetrics() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 24.65,
            finalizedAtIso = "2026-09-18T11:45:00Z",
            finalizedByOperator = "OP-PERADENIYA-01"
        )

        val modal = TransferReceiptModal.newInstance(mockReceipt)
        modal.show(activity.supportFragmentManager, "TestReceiptModal")
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(modal.dialog)
        assertTrue(modal.dialog?.isShowing == true)

        val binding = modal.binding
        assertTrue(binding.tvReceiptDeliveredKwh.text.contains("24.65"))
        assertTrue(binding.tvReceiptReservationId.text.contains("664fa10b9c3e2e1a4f001201"))
        assertTrue(binding.tvReceiptOperator.text.contains("OP-PERADENIYA-01"))
        assertTrue(binding.tvReceiptTimestamp.text.contains("2026-09-18T11:45:00Z"))
        assertEquals(ReservationStatus.COMPLETED, binding.badgeReceiptStatus.currentStatus)
    }

    /**
     * Asserts that clicking the Done button triggers onDoneClicked callback and dismisses modal.
     */
    @Test
    fun doneButton_invokesDoneCallback() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "RES-999",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 15.00,
            finalizedAtIso = "2026-09-18T12:00:00Z",
            finalizedByOperator = "OP-01"
        )

        var doneClicked = false
        val modal = TransferReceiptModal.newInstance(mockReceipt).apply {
            onDoneClicked = { doneClicked = true }
        }

        modal.show(activity.supportFragmentManager, "TestReceiptModal")
        activity.supportFragmentManager.executePendingTransactions()

        modal.binding.btnDoneReceipt.performClick()
        ShadowLooper.idleMainLooper()

        assertTrue(doneClicked)
    }

    /**
     * Asserts that dismissing the dialog without clicking Done still triggers onDoneClicked callback defensively.
     */
    @Test
    fun dialogDismiss_invokesDoneCallbackDefensively() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "RES-888",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 12.50,
            finalizedAtIso = "2026-09-18T12:30:00Z",
            finalizedByOperator = "OP-02"
        )

        var doneClicked = false
        val modal = TransferReceiptModal.newInstance(mockReceipt).apply {
            onDoneClicked = { doneClicked = true }
        }

        modal.show(activity.supportFragmentManager, "TestReceiptModal")
        activity.supportFragmentManager.executePendingTransactions()

        modal.dismiss()
        ShadowLooper.idleMainLooper()

        assertTrue(doneClicked)
    }
}
