/**
 * Description: UI unit test suite for TransferFinalizeDialog verifying defensive numeric validation
 * (0.01 - 999.99 kWh) and finalization callback dispatching under Robolectric (FR-M4-07.1).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.R
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

/**
 * Asserts defensive input validation and submission logic on TransferFinalizeDialog.
 */
@RunWith(RobolectricTestRunner::class)
class TransferFinalizeDialogTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.Theme_Ssmts_OperatorDashboard)
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().start().resume().visible().get()
    }

    /**
     * Asserts that dialog displays the target reservation identifier upon presentation.
     */
    @Test
    fun dialogInflation_populatesReservationId() {
        val dialog = TransferFinalizeDialog.newInstance("664fa10b9c3e2e1a4f001201")
        dialog.show(activity.supportFragmentManager, "FinalizeTest")
        activity.supportFragmentManager.executePendingTransactions()

        assertNotNull(dialog.dialog)
        assertTrue(dialog.dialog?.isShowing == true)
        assertTrue(dialog.binding.tvFinalizeReservationId.text.contains("664fa10b9c3e2e1a4f001201"))
    }

    /**
     * Asserts that invalid, negative, zero, or non-numeric inputs trigger inline validation errors.
     */
    @Test
    fun invalidInputs_showErrorAndBlockSubmission() {
        val dialog = TransferFinalizeDialog.newInstance("res-test")
        var callbackFired = false
        dialog.onFinalizeConfirmed = { _, _, _ -> callbackFired = true }

        dialog.show(activity.supportFragmentManager, "InvalidTest")
        activity.supportFragmentManager.executePendingTransactions()

        val invalidTestCases = listOf("", "0", "0.00", "-5.5", "abc", "1000.0", "99999")

        for (invalidInput in invalidTestCases) {
            dialog.binding.etMeteredKwh.setText(invalidInput)
            dialog.binding.btnConfirmFinalize.performClick()

            assertNotNull(dialog.binding.tilMeteredKwh.error)
            assertEquals(
                activity.getString(R.string.error_invalid_kwh_range),
                dialog.binding.tilMeteredKwh.error.toString()
            )
            assertFalse(callbackFired)
        }
    }

    /**
     * Asserts that valid decimal inputs within 0.01 to 999.99 kWh dispatch the confirmation callback.
     */
    @Test
    fun validInput_dispatchesCallbackWithAccurateValues() {
        val dialog = TransferFinalizeDialog.newInstance("res-test-99")
        var recordedResId: String? = null
        var recordedKwh: Double? = null
        var recordedNotes: String? = null

        dialog.onFinalizeConfirmed = { id, kwh, notes ->
            recordedResId = id
            recordedKwh = kwh
            recordedNotes = notes
        }

        dialog.show(activity.supportFragmentManager, "ValidTest")
        activity.supportFragmentManager.executePendingTransactions()

        dialog.binding.etMeteredKwh.setText("24.65")
        dialog.binding.etOperatorNotes.setText("Nominal voltage transfer.")
        dialog.binding.btnConfirmFinalize.performClick()

        assertNull(dialog.binding.tilMeteredKwh.error)
        assertEquals("res-test-99", recordedResId)
        assertEquals(24.65, recordedKwh!!, 0.001)
        assertEquals("Nominal voltage transfer.", recordedNotes)
    }

    /**
     * Asserts that clicking the cancel button dismisses without dispatching confirmation.
     */
    @Test
    fun cancelButton_dismissesWithoutConfirmation() {
        val dialog = TransferFinalizeDialog.newInstance("res-cancel")
        var confirmed = false
        var cancelled = false

        dialog.onFinalizeConfirmed = { _, _, _ -> confirmed = true }
        dialog.onCancelClicked = { cancelled = true }

        dialog.show(activity.supportFragmentManager, "CancelTest")
        activity.supportFragmentManager.executePendingTransactions()

        dialog.binding.btnCancelFinalize.performClick()

        assertFalse(confirmed)
        assertTrue(cancelled)
    }
}
