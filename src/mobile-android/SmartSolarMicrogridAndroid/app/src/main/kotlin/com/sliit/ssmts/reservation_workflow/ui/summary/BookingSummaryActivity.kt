/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity displaying the reservation summary receipt with QR support and viva demo options.
 */

package com.sliit.ssmts.reservation_workflow.ui.summary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityBookingSummaryBinding
import com.sliit.ssmts.operator_dashboard.util.QrCodeGenerator
import com.sliit.ssmts.reservation_workflow.di.DependencyProvider
import com.sliit.ssmts.reservation_workflow.di.ViewModelFactory
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.ui.history.BookingHistoryActivity
import com.sliit.ssmts.reservation_workflow.ui.manage.CancelReservationDialog
import com.sliit.ssmts.reservation_workflow.ui.manage.UpdateReservationDialog
import com.sliit.ssmts.reservation_workflow.ui.manage.ManageReservationViewModel
import com.sliit.ssmts.reservation_workflow.util.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class BookingSummaryActivity : AppCompatActivity(), 
    CancelReservationDialog.OnCancelConfirmedListener, 
    UpdateReservationDialog.OnUpdateConfirmedListener {

    private lateinit var binding: ActivityBookingSummaryBinding
    private lateinit var viewModel: BookingSummaryViewModel
    private lateinit var manageViewModel: ManageReservationViewModel
    private var currentReservation: Reservation? = null
    private var currentReservationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBookingSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Extract reservation data from Intent
        val reservationId = intent.getStringExtra("RESERVATION_ID") ?: return
        currentReservationId = reservationId
        
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
                        binding.tvStatusBadge.text = getString(R.string.status_pending)
                    }
                    is UiState.Success -> {
                        val reservation = state.data
                        currentReservation = reservation
                        binding.tvReservationId.text = reservation.id
                        binding.tvStation.text = reservation.stationId
                        binding.tvScheduledTime.text = DateTimeFormatter.toDisplayString(reservation.scheduledDateTime)
                        binding.tvProsumerNic.text = reservation.prosumerId
                        
                        applyStatusBadge(reservation.status)
                        applyApprovalAlert(reservation.status)

                        // QR Code display & bitmap generation
                        if (reservation.status == ReservationStatus.APPROVED && !reservation.qrCode.isNullOrEmpty()) {
                            binding.cardQrCode.visibility = View.VISIBLE
                            binding.tvQrToken.text = reservation.qrCode
                            try {
                                val bitmap = QrCodeGenerator.generateQrBitmap(
                                    payload = reservation.qrCode,
                                    dimensionPx = 512
                                )
                                binding.ivQrCode.setImageBitmap(bitmap)
                                binding.ivQrCode.visibility = View.VISIBLE
                            } catch (_: Exception) {
                                binding.ivQrCode.visibility = View.GONE
                            }

                            binding.btnCopyQrToken.setOnClickListener {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("QR Token", reservation.qrCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(this@BookingSummaryActivity, R.string.msg_token_copied, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            binding.cardQrCode.visibility = View.GONE
                        }
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
        
        // Reschedule button with scheduled time passed for 12-hour rule check
        binding.btnReschedule.setOnClickListener {
            val millis = currentReservation?.scheduledDateTime?.time ?: System.currentTimeMillis()
            val dialog = UpdateReservationDialog.newInstance(millis)
            dialog.show(supportFragmentManager, UpdateReservationDialog.TAG)
        }
        
        // Cancel button with scheduled time passed for 12-hour rule check
        binding.btnCancel.setOnClickListener {
            val millis = currentReservation?.scheduledDateTime?.time ?: System.currentTimeMillis()
            val dialog = CancelReservationDialog.newInstance(millis)
            dialog.show(supportFragmentManager, CancelReservationDialog.TAG)
        }

        // View History button
        binding.btnViewHistory.setOnClickListener {
            val intent = Intent(this, BookingHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_booking_summary, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_seed_slots -> {
                lifecycleScope.launch {
                    try {
                        val api = DependencyProvider.getReservationApi()
                        val response = withContext(Dispatchers.IO) { api.seedSlots() }
                        if (response.isSuccessful) {
                            Toast.makeText(this@BookingSummaryActivity, "Demo slots successfully seeded in database!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@BookingSummaryActivity, "Seed failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@BookingSummaryActivity, "Seed error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                true
            }
            R.id.action_demo_blocked -> {
                // Simulate a booking only 2 hours away to demonstrate the 12-hour block UI
                val blockedMillis = System.currentTimeMillis() + (2 * 3600 * 1000L)
                val dialog = CancelReservationDialog.newInstance(blockedMillis)
                dialog.show(supportFragmentManager, CancelReservationDialog.TAG)
                true
            }
            R.id.action_demo_allowed -> {
                // Simulate a booking 25 hours away to demonstrate normal cancellation allowed UI
                val allowedMillis = System.currentTimeMillis() + (25 * 3600 * 1000L)
                val dialog = CancelReservationDialog.newInstance(allowedMillis)
                dialog.show(supportFragmentManager, CancelReservationDialog.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    private fun applyApprovalAlert(status: ReservationStatus) {
        when (status) {
            ReservationStatus.PENDING -> {
                binding.cardApprovalAlert.visibility = View.VISIBLE
                binding.cardApprovalAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_container))
                binding.cardApprovalAlert.strokeColor = ContextCompat.getColor(this, R.color.status_pending_stroke)
                binding.ivAlertIcon.setImageResource(R.drawable.ic_status_pending)
                binding.ivAlertIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_pending_text))
                binding.tvAlertMessage.text = getString(R.string.alert_pending_approval)
                binding.tvAlertMessage.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text))
            }
            ReservationStatus.APPROVED -> {
                binding.cardApprovalAlert.visibility = View.VISIBLE
                binding.cardApprovalAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_completed_container))
                binding.cardApprovalAlert.strokeColor = ContextCompat.getColor(this, R.color.status_completed_stroke)
                binding.ivAlertIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivAlertIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_completed_text))
                binding.tvAlertMessage.text = getString(R.string.alert_approved_confirmed)
                binding.tvAlertMessage.setTextColor(ContextCompat.getColor(this, R.color.status_completed_text))
            }
            ReservationStatus.COMPLETED -> {
                binding.cardApprovalAlert.visibility = View.VISIBLE
                binding.cardApprovalAlert.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_completed_container))
                binding.cardApprovalAlert.strokeColor = ContextCompat.getColor(this, R.color.status_completed_stroke)
                binding.ivAlertIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivAlertIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_completed_text))
                binding.tvAlertMessage.text = getString(R.string.alert_completed_finalized)
                binding.tvAlertMessage.setTextColor(ContextCompat.getColor(this, R.color.status_completed_text))
            }
            else -> {
                binding.cardApprovalAlert.visibility = View.GONE
            }
        }
    }

    override fun onCancelConfirmed(reason: String?) {
        val reservation = currentReservation
        if (reservation != null) {
            manageViewModel.cancelReservation(reservation, reason ?: "User Requested Cancellation")
        }
    }

    override fun onUpdateConfirmed(newDate: Date) {
        val reservation = currentReservation
        if (reservation != null) {
            manageViewModel.updateReservation(reservation, reservation.bookingSlotId, newDate)
        }
    }
}
