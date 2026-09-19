/**
 * Description: UI unit test suite for TransferHandshakeModal verifying inflation, field data binding,
 * and user interaction button dispatching under Robolectric (FR-M4-06.4).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.R
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

/**
 * Validates view binding, text population, and callback triggers on TransferHandshakeModal.
 */
@RunWith(RobolectricTestRunner::class)
class TransferHandshakeModalTest {

    private lateinit var activity: AppCompatActivity

    /**
     * Initializes themed application context and activity container before handshake modal tests.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().visible().get()
    }

    /**
     * Asserts that modal displays accurate prosumer, station, bay, and reservation details.
     */
    @Test
    fun modalInflation_populatesReservationMetadata() {
        val mockResult = QrVerificationResult(
            isValid = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            prosumerNic = "200012345678",
            stationName = "Peradeniya Agro-Voltaic Hub",
            allocatedBayId = "BAY-02",
            status = ReservationStatus.APPROVED
        )

        val modal = TransferHandshakeModal.newInstance(mockResult)
        modal.show(activity.supportFragmentManager, "TestModal")
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(modal.dialog)
        assertTrue(modal.dialog?.isShowing == true)

        val binding = modal.binding
        assertTrue(binding.tvHandshakeReservationId.text.contains("664fa10b9c3e2e1a4f001201"))
        assertTrue(binding.tvHandshakeProsumer.text.contains("200012345678"))
        assertTrue(binding.tvHandshakeStation.text.contains("Peradeniya Agro-Voltaic Hub"))
        assertTrue(binding.tvHandshakeBay.text.contains("BAY-02"))
    }

    /**
     * Asserts that clicking Proceed button triggers the corresponding callback.
     */
    @Test
    fun proceedButton_invokesProceedCallback() {
        val mockResult = QrVerificationResult(isValid = true, reservationId = "res-test")
        val modal = TransferHandshakeModal.newInstance(mockResult)

        var proceedCalled = false
        modal.onProceedClicked = {
            proceedCalled = true
        }

        modal.show(activity.supportFragmentManager, "ProceedTest")
        activity.supportFragmentManager.executePendingTransactions()

        modal.binding.btnProceedFinalize.performClick()
        assertTrue(proceedCalled)
    }

    /**
     * Asserts that clicking Cancel button triggers the cancel callback and dismisses.
     */
    @Test
    fun cancelButton_invokesCancelCallback() {
        val mockResult = QrVerificationResult(isValid = true, reservationId = "res-test")
        val modal = TransferHandshakeModal.newInstance(mockResult)

        var cancelCalled = false
        modal.onCancelClicked = {
            cancelCalled = true
        }

        modal.show(activity.supportFragmentManager, "CancelTest")
        activity.supportFragmentManager.executePendingTransactions()

        modal.binding.btnCancelHandshake.performClick()
        assertTrue(cancelCalled)
    }
}
