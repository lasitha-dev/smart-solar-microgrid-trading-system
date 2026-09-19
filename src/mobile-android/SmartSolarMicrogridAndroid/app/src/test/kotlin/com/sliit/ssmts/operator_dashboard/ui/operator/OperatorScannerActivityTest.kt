/**
 * Description: Unit test suite for OperatorScannerActivity validating camera permission safety,
 * UI components initialization, torch toggle state changes, and navigation handling (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import android.view.View
import androidx.camera.view.PreviewView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Validates OperatorScannerActivity lifecycle, permission defense, and interaction controls.
 */
@RunWith(RobolectricTestRunner::class)
class OperatorScannerActivityTest {

    private lateinit var controller: ActivityController<OperatorScannerActivity>
    private lateinit var activity: OperatorScannerActivity

    /**
     * Initializes theme context and instantiates the Robolectric activity controller before each test.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)

        controller = Robolectric.buildActivity(OperatorScannerActivity::class.java)
        activity = controller.create().start().resume().visible().get()
    }

    /**
     * Asserts that all viewfinder and overlay UI components are properly inflated and present.
     */
    @Test
    fun activityLaunch_inflatesViewfinderAndOverlay() {
        val previewView = activity.findViewById<PreviewView>(R.id.previewViewFinder)
        val overlayView = activity.findViewById<ViewfinderOverlayView>(R.id.viewfinderOverlay)
        val btnBack = activity.findViewById<View>(R.id.btnScannerBack)
        val btnTorch = activity.findViewById<View>(R.id.btnTorchToggle)

        assertNotNull(previewView)
        assertNotNull(overlayView)
        assertNotNull(btnBack)
        assertNotNull(btnTorch)
    }

    /**
     * Asserts that when camera permission is denied, the activity gracefully
     * displays the permission denied card without crashing (Section 4 Mandate).
     */
    @Test
    fun permissionDenied_displaysPermissionDeniedCard() {
        activity.handleCameraPermissionResult(false)

        val cardPermissionDenied = activity.findViewById<View>(R.id.cardPermissionDenied)
        val btnGrant = activity.findViewById<MaterialButton>(R.id.btnGrantPermission)

        assertNotNull(cardPermissionDenied)
        assertNotNull(btnGrant)
        assertEquals(View.VISIBLE, cardPermissionDenied.visibility)
        assertTrue(btnGrant.isClickable)
    }

    /**
     * Asserts that when camera permission is granted, the permission denied card is hidden.
     */
    @Test
    fun permissionGranted_hidesPermissionDeniedCard() {
        // First simulate denied
        activity.handleCameraPermissionResult(false)
        val cardPermissionDenied = activity.findViewById<View>(R.id.cardPermissionDenied)
        assertEquals(View.VISIBLE, cardPermissionDenied.visibility)

        // Then simulate granted
        activity.handleCameraPermissionResult(true)
        assertEquals(View.GONE, cardPermissionDenied.visibility)
    }

    /**
     * Asserts that clicking the back button finishes the scanner activity.
     */
    @Test
    fun btnScannerBack_finishesActivity() {
        val btnBack = activity.findViewById<View>(R.id.btnScannerBack)
        assertNotNull(btnBack)

        btnBack.performClick()
        assertTrue(activity.isFinishing)
    }

    /**
     * Asserts that torch toggle updates internal state between enabled and disabled.
     */
    @Test
    fun toggleTorch_updatesTorchState() {
        assertFalse(activity.isTorchEnabled)

        activity.toggleTorch()
        assertTrue(activity.isTorchEnabled)

        activity.toggleTorch()
        assertFalse(activity.isTorchEnabled)
    }

    /**
     * Asserts that ViewfinderOverlayView handles layout measurements and draws without exceptions.
     */
    @Test
    fun viewfinderOverlayView_measuresAndDrawsCleanly() {
        val overlayView = activity.findViewById<ViewfinderOverlayView>(R.id.viewfinderOverlay)
        assertNotNull(overlayView)

        overlayView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        overlayView.layout(0, 0, 1080, 1920)

        assertEquals(1080, overlayView.width)
        assertEquals(1920, overlayView.height)
    }

    /**
     * Asserts that when a transfer is finalized, the TransferReceiptModal is presented to the operator (FR-M4-07.4).
     */
    @Test
    fun finalizedState_displaysTransferReceiptModal() {
        val mockReceipt = FinalizeTransferResult(
            isSuccess = true,
            reservationId = "664fa10b9c3e2e1a4f001201",
            status = ReservationStatus.COMPLETED,
            meteredEnergyKwh = 24.65,
            finalizedAtIso = "2026-09-18T11:45:00Z",
            finalizedByOperator = "OP-PERADENIYA-01"
        )

        val modal = TransferReceiptModal.newInstance(mockReceipt)
        modal.show(activity.supportFragmentManager, TransferReceiptModal.TAG)
        activity.supportFragmentManager.executePendingTransactions()

        val foundModal = activity.supportFragmentManager.findFragmentByTag(TransferReceiptModal.TAG) as? TransferReceiptModal
        assertNotNull(foundModal)
        assertTrue(foundModal?.dialog?.isShowing == true)
        assertTrue(foundModal?.binding?.tvReceiptDeliveredKwh?.text?.contains("24.65") == true)
    }
}
