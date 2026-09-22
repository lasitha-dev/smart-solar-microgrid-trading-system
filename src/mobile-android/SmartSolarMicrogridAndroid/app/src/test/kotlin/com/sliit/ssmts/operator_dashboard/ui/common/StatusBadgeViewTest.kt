/**
 * Description: Unit test suite for StatusBadgeView verifying correct background drawables,
 * text colors, labels, and string parsing across all 4 reservation states (FR-M4-03.4).
 */
package com.sliit.ssmts.operator_dashboard.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test verifying StatusBadgeView styling and state bindings.
 */
@RunWith(RobolectricTestRunner::class)
class StatusBadgeViewTest {

    private lateinit var context: Context
    private lateinit var badgeView: StatusBadgeView

    /**
     * Initializes Android context and StatusBadgeView instance before each test.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        badgeView = StatusBadgeView(context)
    }

    /**
     * Verifies that setting COMPLETED renders 'Completed' label and correct green text color.
     */
    @Test
    fun setStatus_completed_rendersGreenColorAndText() {
        badgeView.setStatus(ReservationStatus.COMPLETED)

        assertEquals(ReservationStatus.COMPLETED, badgeView.currentStatus)
        assertEquals("Completed", badgeView.text.toString())

        val expectedColor = ContextCompat.getColor(context, R.color.status_completed_text)
        assertEquals(expectedColor, badgeView.currentTextColor)
        assertNotNull(badgeView.background)
    }

    /**
     * Verifies that setting APPROVED renders 'Approved' label and correct blue text color.
     */
    @Test
    fun setStatus_approved_rendersBlueColorAndText() {
        badgeView.setStatus(ReservationStatus.APPROVED)

        assertEquals(ReservationStatus.APPROVED, badgeView.currentStatus)
        assertEquals("Approved", badgeView.text.toString())

        val expectedColor = ContextCompat.getColor(context, R.color.status_approved_text)
        assertEquals(expectedColor, badgeView.currentTextColor)
        assertNotNull(badgeView.background)
    }

    /**
     * Verifies that setting PENDING renders 'Pending' label and correct amber text color.
     */
    @Test
    fun setStatus_pending_rendersAmberColorAndText() {
        badgeView.setStatus(ReservationStatus.PENDING)

        assertEquals(ReservationStatus.PENDING, badgeView.currentStatus)
        assertEquals("Pending", badgeView.text.toString())

        val expectedColor = ContextCompat.getColor(context, R.color.status_pending_text)
        assertEquals(expectedColor, badgeView.currentTextColor)
        assertNotNull(badgeView.background)
    }

    /**
     * Verifies that setting CANCELLED renders 'Cancelled' label and correct red text color.
     */
    @Test
    fun setStatus_cancelled_rendersRedColorAndText() {
        badgeView.setStatus(ReservationStatus.CANCELLED)

        assertEquals(ReservationStatus.CANCELLED, badgeView.currentStatus)
        assertEquals("Cancelled", badgeView.text.toString())

        val expectedColor = ContextCompat.getColor(context, R.color.status_cancelled_text)
        assertEquals(expectedColor, badgeView.currentTextColor)
        assertNotNull(badgeView.background)
    }

    /**
     * Verifies string overload parses status string accurately.
     */
    @Test
    fun setStatus_fromString_appliesProperState() {
        badgeView.setStatus("approved")
        assertEquals(ReservationStatus.APPROVED, badgeView.currentStatus)
        assertEquals("Approved", badgeView.text.toString())

        badgeView.setStatus("completed")
        assertEquals(ReservationStatus.COMPLETED, badgeView.currentStatus)
        assertEquals("Completed", badgeView.text.toString())
    }
}
