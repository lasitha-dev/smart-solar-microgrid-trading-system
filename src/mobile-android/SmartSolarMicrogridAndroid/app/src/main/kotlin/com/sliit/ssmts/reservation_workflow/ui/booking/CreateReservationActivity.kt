/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity for filling out booking details and confirming the reservation.
 */

package com.sliit.ssmts.reservation_workflow.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sliit.ssmts.R
import com.sliit.ssmts.util.SessionManager
import com.sliit.ssmts.databinding.ActivityCreateReservationBinding
import com.sliit.ssmts.reservation_workflow.di.DependencyProvider
import com.sliit.ssmts.reservation_workflow.di.ViewModelFactory
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.ui.summary.BookingSummaryActivity
import kotlinx.coroutines.launch
import java.util.Date

class CreateReservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateReservationBinding
    private lateinit var viewModel: ReservationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCreateReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Show selected slot info from intent extras
        val slotInfo = intent.getStringExtra("SLOT_INFO") ?: "No slot selected"
        binding.tvSlotInfo.text = slotInfo

        // Pre-fill logged-in prosumer NIC if available
        lifecycleScope.launch {
            val session = SessionManager(this@CreateReservationActivity).getActiveSession()
            if (!session?.nic.isNullOrBlank()) {
                binding.etProsumerNic.setText(session.nic)
            }
        }
        
        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Confirm Booking button
        binding.btnConfirmBooking.setOnClickListener {
            if (validateForm()) {
                submitReservation()
            }
        }
        
        // Initialize ViewModel
        val repository = DependencyProvider.getReservationRepository(this)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ReservationViewModel::class.java]
        
        // Observe State
        lifecycleScope.launch {
            viewModel.createState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        binding.progressLoading.visibility = View.GONE
                        binding.btnConfirmBooking.isEnabled = true
                    }
                    is UiState.Loading -> {
                        binding.progressLoading.visibility = View.VISIBLE
                        binding.btnConfirmBooking.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.progressLoading.visibility = View.GONE
                        binding.btnConfirmBooking.isEnabled = true
                        
                        Toast.makeText(this@CreateReservationActivity, getString(R.string.msg_booking_success), Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@CreateReservationActivity, BookingSummaryActivity::class.java).apply {
                            putExtra("RESERVATION_ID", state.data.id)
                            putExtra("STATION", state.data.stationId)
                            putExtra("SCHEDULED_TIME", intent.getStringExtra("SLOT_INFO") ?: "")
                            putExtra("PROSUMER_NIC", state.data.prosumerId)
                            putExtra("STATUS", state.data.status.name)
                        }
                        startActivity(intent)
                        finish()
                        
                        viewModel.resetState()
                    }
                    is UiState.Error -> {
                        binding.progressLoading.visibility = View.GONE
                        binding.btnConfirmBooking.isEnabled = true
                        Toast.makeText(this@CreateReservationActivity, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
    
    /**
     * Validates form inputs: NIC must not be empty, kWh must be 0.1–500.0
     */
    private fun validateForm(): Boolean {
        val nic = binding.etProsumerNic.text.toString().trim()
        val kwhText = binding.etEstimatedKwh.text.toString().trim()
        
        if (nic.isEmpty()) {
            binding.tilProsumerNic.error = getString(R.string.err_invalid_nic)
            return false
        }
        binding.tilProsumerNic.error = null
        
        val kwh = kwhText.toDoubleOrNull()
        if (kwh == null || kwh < 0.1 || kwh > 500.0) {
            binding.tilEstimatedKwh.error = getString(R.string.err_invalid_kwh)
            return false
        }
        binding.tilEstimatedKwh.error = null
        
        return true
    }
    
    /**
     * Submits the reservation using the ViewModel.
     */
    private fun submitReservation() {
        val nic = binding.etProsumerNic.text.toString().trim()
        val stationId = intent.getStringExtra("STATION") ?: "Unknown"
        val slotId = intent.getStringExtra("SLOT_ID") ?: "Unknown"
        
        val slotDateIso = intent.getStringExtra("SLOT_DATE") ?: ""
        val timeRange = intent.getStringExtra("TIME_RANGE") ?: ""
        
        // Parse date from ISO format string "2026-09-17T00:00:00.000Z"
        // and add the start time from "08:00 - 09:00"
        var scheduledDate = Date()
        try {
            // Take only the "yyyy-MM-dd'T'HH:mm:ss" part (first 19 chars)
            val cleanIso = if (slotDateIso.length >= 19) slotDateIso.substring(0, 19) else slotDateIso
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val baseDate = isoFormat.parse(cleanIso)
            
            if (baseDate != null && timeRange.contains("-")) {
                val startTimeStr = timeRange.split("-")[0].trim() // "08:00"
                val hour = startTimeStr.split(":")[0].toInt()
                val minute = startTimeStr.split(":")[1].toInt()
                
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                cal.time = baseDate
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, minute)
                
                scheduledDate = cal.time
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        viewModel.createReservation(nic, stationId, slotId, scheduledDate)
    }
}
