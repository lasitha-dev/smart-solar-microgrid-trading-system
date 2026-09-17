/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity displaying the reservation summary receipt.
 */

package com.sliit.ssmts.reservation_workflow.ui.summary

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ActivityBookingSummaryBinding
import com.sliit.ssmts.reservation_workflow.di.DependencyProvider
import com.sliit.ssmts.reservation_workflow.di.ViewModelFactory
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.ui.manage.CancelReservationDialog
import com.sliit.ssmts.reservation_workflow.ui.manage.UpdateReservationDialog
import com.sliit.ssmts.reservation_workflow.ui.manage.ManageReservationViewModel
import kotlinx.coroutines.launch
import java.util.Date

class BookingSummaryActivity : AppCompatActivity(), 
    CancelReservationDialog.OnCancelConfirmedListener, 
    UpdateReservationDialog.OnUpdateConfirmedListener {

    private lateinit var binding: ActivityBookingSummaryBinding
    private lateinit var viewModel: BookingSummaryViewModel
    private lateinit var manageViewModel: ManageReservationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBookingSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Extract reservation data from Intent
        val reservationId = intent.getStringExtra("RESERVATION_ID") ?: return
        
        // Setup ViewModel
        val repository = DependencyProvider.getReservationRepository(this)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[BookingSummaryViewModel::class.java]
        manageViewModel = ViewModelProvider(this, factory)[ManageReservationViewModel::class.java]
        
        // Observe State
        lifecycleScope.launch {
            viewModel.summaryState.collect { state ->
                when (state) {
                    is UiState.Idle -> { /* do nothing */ }
                    is UiState.Loading -> {
                        binding.tvStatusBadge.text = "Loading..."
                    }
                    is UiState.Success -> {
                        val reservation = state.data
                        binding.tvReservationId.text = reservation.id
                        binding.tvStation.text = reservation.stationId
                        binding.tvScheduledTime.text = reservation.scheduledDateTime.toString()
                        binding.tvProsumerNic.text = reservation.prosumerId
                        
                        applyStatusBadge(reservation.status)
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@BookingSummaryActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            manageViewModel.cancelState.collect { state ->
                when(state) {
                    is UiState.Success -> {
                        Toast.makeText(this@BookingSummaryActivity, "Reservation Cancelled!", Toast.LENGTH_SHORT).show()
                        manageViewModel.resetState()
                        viewModel.loadReservation(reservationId) // Refresh UI
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@BookingSummaryActivity, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        manageViewModel.resetState()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            manageViewModel.manageState.collect { state ->
                when(state) {
                    is UiState.Success -> {
                        Toast.makeText(this@BookingSummaryActivity, "Reservation Rescheduled!", Toast.LENGTH_SHORT).show()
                        manageViewModel.resetState()
                        viewModel.loadReservation(reservationId) // Refresh UI
                    }
                    is UiState.Error -> {
                        Toast.makeText(this@BookingSummaryActivity, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        manageViewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
        
        viewModel.loadReservation(reservationId)
        
        // Reschedule button
        binding.btnReschedule.setOnClickListener {
            val dialog = com.sliit.ssmts.reservation_workflow.ui.manage.UpdateReservationDialog()
            dialog.show(supportFragmentManager, com.sliit.ssmts.reservation_workflow.ui.manage.UpdateReservationDialog.TAG)
        }
        
        // Cancel button
        binding.btnCancel.setOnClickListener {
            val dialog = com.sliit.ssmts.reservation_workflow.ui.manage.CancelReservationDialog()
            dialog.show(supportFragmentManager, com.sliit.ssmts.reservation_workflow.ui.manage.CancelReservationDialog.TAG)
        }
    }
    
    /**
     * Applies the correct background and text color to the status badge
     * based on the reservation status (Pending, Approved, Cancelled, Completed).
     */
    private fun applyStatusBadge(status: ReservationStatus) {
        val (bgColor, textColor, label) = when (status) {
            ReservationStatus.PENDING -> Triple(
                R.color.status_pending_container,
                R.color.status_pending_text,
                getString(R.string.status_pending)
            )
            ReservationStatus.APPROVED -> Triple(
                R.color.status_approved_container,
                R.color.status_approved_text,
                getString(R.string.status_approved)
            )
            ReservationStatus.CANCELLED -> Triple(
                R.color.status_cancelled_container,
                R.color.status_cancelled_text,
                getString(R.string.status_cancelled)
            )
            ReservationStatus.COMPLETED -> Triple(
                R.color.status_completed_container,
                R.color.status_completed_text,
                getString(R.string.status_completed)
            )
            else -> Triple(
                R.color.surface_card,
                R.color.text_primary,
                status.name
            )
        }
        
        binding.tvStatusBadge.text = label
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, textColor))
        binding.cardStatusBadge.setCardBackgroundColor(ContextCompat.getColor(this, bgColor))
    }

    override fun onCancelConfirmed() {
        val currentState = viewModel.summaryState.value
        if (currentState is UiState.Success) {
            manageViewModel.cancelReservation(currentState.data, "User Requested Cancellation")
        }
    }

    override fun onUpdateConfirmed(newDate: Date) {
        val currentState = viewModel.summaryState.value
        if (currentState is UiState.Success) {
            // For demo, re-using the same slotId but with new Date. 
            // In a real flow, they'd select a specific new Slot from the Date.
            manageViewModel.updateReservation(currentState.data, currentState.data.bookingSlotId, newDate)
        }
    }
}
