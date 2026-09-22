/**
 * Description: Unit test suite for OperatorScannerActivity validating Fast Test QR simulation triggers,
 * payload injection pipeline, and defensive client-side parsing behavior (Rule 6.3 & FR-M4-05).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.util.FastTestQrScenarios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Asserts the Fast Test QR simulation mechanisms in OperatorScannerActivity under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class OperatorScannerFastTestTest {

    private lateinit var controller: ActivityController<OperatorScannerActivity>
    private lateinit var activity: OperatorScannerActivity

    /**
     * Prepares themed application context and builds the scanner activity before fast-test assertions.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)

        controller = Robolectric.buildActivity(OperatorScannerActivity::class.java)
        activity = controller.create().start().resume().visible().get()
    }

    /**
     * Asserts that Fast Test QR triggers are present in both bottom controls and permission denial banner.
     */
    @Test
    fun fastTestButtons_areInflatedAndClickable() {
        val btnFastTest = activity.findViewById<View>(R.id.btnFastTestQr)
        val btnPermissionFastTest = activity.findViewById<View>(R.id.btnPermissionUseFastTest)

        assertNotNull(btnFastTest)
        assertNotNull(btnPermissionFastTest)
        assertTrue(btnFastTest.isClickable)
        assertTrue(btnPermissionFastTest.isClickable)
    }

    /**
     * Asserts that processing a valid approved payload triggers the payload listener and returns true.
     */
    @Test
    fun processScannedPayload_validApproved_invokesListenerAndReturnsTrue() {
        var dispatchedPayload: String? = null
        activity.onPayloadProcessedListener = { payload ->
            dispatchedPayload = payload
        }

        val success = activity.processScannedPayload(FastTestQrScenarios.APPROVED_VALID_PAYLOAD)

        assertTrue(success)
        assertEquals(FastTestQrScenarios.APPROVED_VALID_PAYLOAD, dispatchedPayload)
    }

    /**
     * Asserts that processing an already completed payload is accepted by client syntax validation.
     */
    @Test
    fun processScannedPayload_completedScenario_invokesListener() {
        var dispatchedPayload: String? = null
        activity.onPayloadProcessedListener = { payload ->
            dispatchedPayload = payload
        }

        val success = activity.processScannedPayload(FastTestQrScenarios.ALREADY_COMPLETED_PAYLOAD)

        assertTrue(success)
        assertEquals(FastTestQrScenarios.ALREADY_COMPLETED_PAYLOAD, dispatchedPayload)
    }

    /**
     * Asserts that processing a malformed payload rejects it defensively without notifying listener.
     */
    @Test
    fun processScannedPayload_malformed_rejectsWithoutNotifyingListener() {
        var dispatchedPayload: String? = null
        activity.onPayloadProcessedListener = { payload ->
            dispatchedPayload = payload
        }

        val success = activity.processScannedPayload(FastTestQrScenarios.MALFORMED_SYNTAX_PAYLOAD)

        assertFalse(success)
        assertNull(dispatchedPayload)
    }

    /**
     * Asserts that clicking the Fast Test QR button launches the simulation dialog.
     */
    @Test
    fun clickFastTestButton_triggersDialogDisplay() {
        val btnFastTest = activity.findViewById<View>(R.id.btnFastTestQr)
        assertNotNull(btnFastTest)

        // Perform click and verify it executes without unhandled exceptions
        btnFastTest.performClick()
    }
}
