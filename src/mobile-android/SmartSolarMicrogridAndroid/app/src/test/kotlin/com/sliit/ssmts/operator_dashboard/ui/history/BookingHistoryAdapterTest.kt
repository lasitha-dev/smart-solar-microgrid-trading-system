/**
 * Description: Unit test suite for BookingHistoryAdapter verifying DiffUtil item identity and structural
 * equality, view binding, and conditional display of metered kWh on completed transactions (FR-M4-03.5).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests validating BookingHistoryAdapter and its ViewHolder interactions.
 */
@RunWith(RobolectricTestRunner::class)
class BookingHistoryAdapterTest {

    private lateinit var context: Context

    /**
     * Initializes themed application context before adapter tests.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(com.sliit.ssmts.R.style.Theme_Ssmts_OperatorDashboard)
    }

    /**
     * Asserts that DiffUtil areItemsTheSame checks entity identifier equality.
     */
    @Test
    fun diffUtil_areItemsTheSame_validatesById() {
        val res1 = createReservation(id = "RES-001", status = ReservationStatus.PENDING)
        val res1Updated = createReservation(id = "RES-001", status = ReservationStatus.COMPLETED)
        val res2 = createReservation(id = "RES-002", status = ReservationStatus.PENDING)

        assertTrue(BookingHistoryAdapter.ReservationDiffCallback.areItemsTheSame(res1, res1Updated))
        assertFalse(BookingHistoryAdapter.ReservationDiffCallback.areItemsTheSame(res1, res2))
    }

    /**
     * Asserts that DiffUtil areContentsTheSame checks complete data equality.
     */
    @Test
    fun diffUtil_areContentsTheSame_validatesStructuralEquality() {
        val res1 = createReservation(id = "RES-001", status = ReservationStatus.APPROVED, meteredKwh = null)
        val res1Identical = createReservation(id = "RES-001", status = ReservationStatus.APPROVED, meteredKwh = null)
        val res1Finalized = createReservation(id = "RES-001", status = ReservationStatus.COMPLETED, meteredKwh = 34.5)

        assertTrue(BookingHistoryAdapter.ReservationDiffCallback.areContentsTheSame(res1, res1Identical))
        assertFalse(BookingHistoryAdapter.ReservationDiffCallback.areContentsTheSame(res1, res1Finalized))
    }

    /**
     * Asserts that completed reservations display both estimated kWh and actual metered kWh per FR-M4-03.5.
     */
    @Test
    fun bind_completedReservation_displaysMeteredEnergy() {
        val parent = FrameLayout(context)
        val adapter = BookingHistoryAdapter()
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        val completedRes = createReservation(
            id = "RES-COMPLETED-101",
            status = ReservationStatus.COMPLETED,
            estimatedKwh = 35.0,
            meteredKwh = 34.85
        )

        viewHolder.bind(completedRes)

        assertEquals("Colombo Central Hub", viewHolder.binding.tvHistoryStationName.text.toString())
        assertTrue(viewHolder.binding.tvHistoryRefId.text.toString().contains("RES-COMPLETED-101"))
        assertTrue(viewHolder.binding.tvHistoryProsumerNic.text.toString().contains("199412345678"))
        assertTrue(viewHolder.binding.tvHistoryBay.text.toString().contains("BAY-03"))
        assertTrue(viewHolder.binding.tvHistoryEstimatedKwh.text.toString().contains("35.00"))

        // FR-M4-03.5: Metered energy must be visible on completed transactions
        assertEquals(View.VISIBLE, viewHolder.binding.layoutMeteredKwh.visibility)
        assertTrue(viewHolder.binding.tvHistoryMeteredKwh.text.toString().contains("34.85"))
        assertEquals("Completed", viewHolder.binding.badgeHistoryStatus.text.toString())
    }

    /**
     * Asserts that pending/approved reservations hide the metered energy view per FR-M4-03.5.
     */
    @Test
    fun bind_pendingReservation_hidesMeteredEnergy() {
        val parent = FrameLayout(context)
        val adapter = BookingHistoryAdapter()
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        val pendingRes = createReservation(
            id = "RES-PENDING-202",
            status = ReservationStatus.PENDING,
            estimatedKwh = 20.0,
            meteredKwh = null
        )

        viewHolder.bind(pendingRes)

        assertEquals(View.GONE, viewHolder.binding.layoutMeteredKwh.visibility)
        assertEquals("Pending", viewHolder.binding.badgeHistoryStatus.text.toString())
    }

    /**
     * Asserts that tapping a reservation card triggers the onItemClick callback.
     */
    @Test
    fun itemClick_triggersCallbackWithReservation() {
        val parent = FrameLayout(context)
        var clickedReservation: Reservation? = null
        val adapter = BookingHistoryAdapter { res ->
            clickedReservation = res
        }
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        val testRes = createReservation(id = "RES-CLICK-1")
        viewHolder.bind(testRes)

        viewHolder.itemView.performClick()

        assertEquals("RES-CLICK-1", clickedReservation?.id)
    }

    private fun createReservation(
        id: String,
        status: ReservationStatus = ReservationStatus.APPROVED,
        estimatedKwh: Double = 30.0,
        meteredKwh: Double? = null
    ): Reservation {
        return Reservation(
            id = id,
            prosumerNic = "199412345678",
            stationName = "Colombo Central Hub",
            scheduledTimeMillis = 1773720000000L,
            allocatedBay = "BAY-03",
            estimatedKwh = estimatedKwh,
            meteredKwh = meteredKwh,
            status = status,
            qrPayload = "SSMTS-QR|$id|199412345678|STATION|1000|SIG"
        )
    }
}
