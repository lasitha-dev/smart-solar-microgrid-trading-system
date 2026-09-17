/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity displaying a list of all user's reservations with search/filter.
 */

package com.sliit.ssmts.reservation_workflow.ui.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ActivityBookingHistoryBinding
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import java.util.Date

class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingHistoryBinding
    private lateinit var adapter: BookingHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // Setup RecyclerView
        adapter = BookingHistoryAdapter()
        binding.rvBookingHistory.layoutManager = LinearLayoutManager(this)
        binding.rvBookingHistory.adapter = adapter
        
        // Setup Filter Chips
        binding.cgFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chipPending -> filterReservations(ReservationStatus.PENDING)
                R.id.chipApproved -> filterReservations(ReservationStatus.APPROVED)
                else -> filterReservations(null) // All
            }
        }
        
        // Load mock data for now
        loadMockHistory()
    }
    
    private fun filterReservations(status: ReservationStatus?) {
        // Mock filtering
        loadMockHistory(status)
    }
    
    private fun loadMockHistory(statusFilter: ReservationStatus? = null) {
        val mockData = listOf(
            Reservation("RES-001", "200112345678", "Station A", "S1", Date(), ReservationStatus.APPROVED, null),
            Reservation("RES-002", "200112345678", "Station B", "S2", Date(System.currentTimeMillis() + 86400000), ReservationStatus.PENDING, null),
            Reservation("RES-003", "200112345678", "Station A", "S3", Date(System.currentTimeMillis() - 86400000), ReservationStatus.COMPLETED, null),
            Reservation("RES-004", "200112345678", "Station C", "S4", Date(System.currentTimeMillis() + 172800000), ReservationStatus.CANCELLED, null)
        )
        
        val filtered = if (statusFilter != null) {
            mockData.filter { it.status == statusFilter }
        } else {
            mockData
        }
        
        adapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvBookingHistory.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvBookingHistory.visibility = View.VISIBLE
        }
    }
}
