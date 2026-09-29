/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity displaying a list of all user's reservations with Room Flow, network sync, and filtering.
 */

package com.sliit.ssmts.reservation_workflow.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sliit.ssmts.R
import com.sliit.ssmts.util.SessionManager
import com.sliit.ssmts.databinding.ActivityProsumerBookingHistoryBinding
import com.sliit.ssmts.reservation_workflow.di.DependencyProvider
import com.sliit.ssmts.reservation_workflow.di.ViewModelFactory
import com.sliit.ssmts.reservation_workflow.ui.summary.BookingSummaryActivity
import kotlinx.coroutines.launch

class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProsumerBookingHistoryBinding
    private lateinit var viewModel: BookingHistoryViewModel
    private lateinit var adapter: BookingHistoryAdapter
    private var prosumerId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProsumerBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Retrieve logged-in prosumer NIC from SessionManager
        lifecycleScope.launch {
            val session = SessionManager(this@BookingHistoryActivity).getActiveSession()
            prosumerId = session?.nic ?: ""
            viewModel.loadHistory(prosumerId)
        }

        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Setup ViewModel
        val repository = DependencyProvider.getReservationRepository(this)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[BookingHistoryViewModel::class.java]

        // Setup RecyclerView with click listener
        adapter = BookingHistoryAdapter { reservation ->
            val intent = Intent(this, BookingSummaryActivity::class.java).apply {
                putExtra("RESERVATION_ID", reservation.id)
            }
            startActivity(intent)
        }
        binding.rvBookingHistory.layoutManager = LinearLayoutManager(this)
        binding.rvBookingHistory.adapter = adapter
        
        // Setup Filter Chips
        binding.cgFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chipPending -> viewModel.filterByStatus("Pending")
                R.id.chipApproved -> viewModel.filterByStatus("Approved")
                R.id.chipCompleted -> viewModel.filterByStatus("Completed")
                R.id.chipCancelled -> viewModel.filterByStatus("Cancelled")
                else -> viewModel.filterByStatus("All")
            }
        }

        // Observe Data
        lifecycleScope.launch {
            viewModel.reservations.collect { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvBookingHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvBookingHistory.visibility = View.VISIBLE
                }
            }
        }

        // Observe Loading State
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload and sync latest data from server
        viewModel.loadHistory(prosumerId)
    }
}
