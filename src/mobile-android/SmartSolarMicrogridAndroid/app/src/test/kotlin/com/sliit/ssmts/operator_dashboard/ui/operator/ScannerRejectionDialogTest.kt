/**
 * Description: Unit test suite for ScannerRejectionDialog verifying error code mapping
 * and dialog dismissal callback handling under Robolectric (FR-M4-06.3).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Asserts error code mapping and presentation on ScannerRejectionDialog.
 */
@RunWith(RobolectricTestRunner::class)
class ScannerRejectionDialogTest {

    private lateinit var activity: AppCompatActivity

    /**
     * Initializes themed application context and builds parent activity before dialog tests.
     */
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().visible().get()
    }

    /**
     * Asserts that predefined error codes correctly map to their localized string explanations.
     */
    @Test
    fun mapErrorCodeToMessage_mapsKnownErrorCodes() {
        val completedMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_RESERVATION_ALREADY_COMPLETED",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_already_completed), completedMsg)

        val signatureMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_INVALID_QR_SIGNATURE",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_invalid_signature), signatureMsg)

        val notFoundMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_RESERVATION_NOT_FOUND",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_reservation_not_found), notFoundMsg)

        val notApprovedMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_RESERVATION_NOT_APPROVED",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_not_approved), notApprovedMsg)

        val outsideWindowMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_OUTSIDE_OPERATIONAL_WINDOW",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_outside_window), outsideWindowMsg)

        val malformedMsg = ScannerRejectionDialog.mapErrorCodeToMessage(
            activity,
            "ERR_MALFORMED_QR",
            "fallback"
        )
        assertEquals(activity.getString(R.string.err_malformed_qr), malformedMsg)
    }

    /**
     * Asserts that an unknown error code defaults to the provided fallback message.
     */
    @Test
    fun mapErrorCodeToMessage_unknownCode_returnsFallbackMessage() {
        val fallback = "Custom server error occurred."
        val result = ScannerRejectionDialog.mapErrorCodeToMessage(activity, "UNKNOWN_CODE", fallback)
        assertEquals(fallback, result)
    }

    /**
     * Asserts that show displays an alert dialog and positive button invokes dismissal callback.
     */
    @Test
    fun show_displaysDialogAndDismissTriggersCallback() {
        var dismissed = false
        val dialog = ScannerRejectionDialog.show(
            context = activity,
            errorCode = "ERR_RESERVATION_ALREADY_COMPLETED",
            message = "Fallback",
            onDismiss = { dismissed = true }
        )

        assertNotNull(dialog)
        assertTrue(dialog.isShowing)

        dialog.dismiss()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(dismissed)
    }
}
