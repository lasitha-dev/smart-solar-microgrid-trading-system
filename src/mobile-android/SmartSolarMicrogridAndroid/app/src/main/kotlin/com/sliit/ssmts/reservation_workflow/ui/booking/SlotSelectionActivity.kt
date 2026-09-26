/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity for selecting an available energy slot (Date & Time) with dynamic station selector.
 */

package com.sliit.ssmts.reservation_workflow.ui.booking

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.sliit.ssmts.R
import com.sliit.ssmts.data.remote.RetrofitClient
import com.sliit.ssmts.databinding.ActivitySlotSelectionBinding
import com.sliit.ssmts.reservation_workflow.di.DependencyProvider
import com.sliit.ssmts.reservation_workflow.di.ViewModelFactory
import com.sliit.ssmts.reservation_workflow.domain.model.EnergySlot
import com.sliit.ssmts.reservation_workflow.ui.common.UiState
import com.sliit.ssmts.reservation_workflow.ui.history.BookingHistoryActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SlotSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATION_ID = "EXTRA_STATION_ID"
        const val EXTRA_STATION_NAME = "EXTRA_STATION_NAME"
    }

    private lateinit var binding: ActivitySlotSelectionBinding
    private lateinit var viewModel: ReservationViewModel
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private val stations = mutableListOf(
        StationItem("Station A – Solar Bay (Main Microgrid)", "60d5ec49f1b2c42d8c3b4a59"),
        StationItem("Station B – North Grid (Substation)", "60d5ec49f1b2c42d8c3b4a60")
    )
    private var selectedStationId = "60d5ec49f1b2c42d8c3b4a59"
    private lateinit var stationAdapter: ArrayAdapter<StationItem>

    data class StationItem(val name: String, val id: String) {
        override fun toString(): String = name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding
        binding = ActivitySlotSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)

        // Check if a station ID was passed via Map intent
        val passedStationId = intent.getStringExtra(EXTRA_STATION_ID) ?: intent.getStringExtra("STATION_ID")
        val passedStationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: intent.getStringExtra("STATION_NAME")
        if (!passedStationId.isNullOrBlank()) {
            if (stations.none { it.id == passedStationId }) {
                stations.add(0, StationItem(passedStationName ?: "Selected Station", passedStationId))
            }
            selectedStationId = passedStationId
        } else {
            // Automatically select the first available station if none was passed via Map intent
            selectedStationId = stations.first().id
        }

        // Setup Station Spinner with high-contrast readable layouts
        stationAdapter = ArrayAdapter(
            this,
            R.layout.spinner_station_item,
            stations
        ).apply {
            setDropDownViewResource(R.layout.spinner_station_dropdown_item)
        }
        binding.spinnerStation.adapter = stationAdapter
        val initialIndex = stations.indexOfFirst { it.id == selectedStationId }.coerceAtLeast(0)
        binding.spinnerStation.setSelection(initialIndex)

        binding.spinnerStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStationId = stations[position].id
                loadSlotsForDate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Asynchronously fetch live stations from backend if available
        fetchLiveStations()

        // Set up initial state
        updateDateLabel()
        
        // Setup RecyclerView
        binding.rvTimeSlots.layoutManager = LinearLayoutManager(this)
        
        // Initialize ViewModel
        val repository = DependencyProvider.getReservationRepository(this)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ReservationViewModel::class.java]
        
        // Observe State
        lifecycleScope.launch {
            viewModel.slotsState.collect { state ->
                when (state) {
                    is UiState.Idle -> { /* Nothing */ }
                    is UiState.Loading -> {
                        binding.tvEmptyState.text = "Loading slots..."
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvTimeSlots.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        val slots = state.data
                        if (slots.isEmpty()) {
                            binding.tvEmptyState.text = getString(R.string.msg_no_slots)
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.rvTimeSlots.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvTimeSlots.visibility = View.VISIBLE
                            binding.rvTimeSlots.adapter = SlotAdapter(slots) { slot ->
                                val intent = Intent(this@SlotSelectionActivity, CreateReservationActivity::class.java).apply {
                                    putExtra("SLOT_INFO", "${slot.stationId} | ${slot.batterySlotId} | ${slot.timeRange}")
                                    putExtra("STATION", slot.stationId)
                                    putExtra("SLOT_ID", slot.id)
                                    putExtra("SLOT_DATE", slot.date)
                                    putExtra("TIME_RANGE", slot.timeRange)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                    is UiState.Error -> {
                        binding.tvEmptyState.text = "Error: ${state.message}"
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvTimeSlots.visibility = View.GONE
                        Toast.makeText(this@SlotSelectionActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        // Setup DatePicker constrained to today -> today + 7 days
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }
        
        // Load demo slots for today
        loadSlotsForDate()
    }

    private fun fetchLiveStations() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getStationApi(this@SlotSelectionActivity)
                val response = api.getAllStations()
                val rawData = if (response.isSuccessful && !response.body()?.data.isNullOrEmpty()) {
                    response.body()?.data
                } else {
                    val fallbackResponse = api.getNearbyStations(lat = 6.9271, lng = 79.8612, radiusKm = 1000.0)
                    fallbackResponse.body()?.data
                }

                if (!rawData.isNullOrEmpty()) {
                    val liveList = rawData.filter { it.status != "Deactivated" }
                    if (liveList.isNotEmpty()) {
                        stations.clear()
                        liveList.forEach { s ->
                            stations.add(StationItem(s.stationName, s.id))
                        }
                        stationAdapter.notifyDataSetChanged()
                        val targetIndex = stations.indexOfFirst { it.id == selectedStationId }
                        if (targetIndex >= 0) {
                            binding.spinnerStation.setSelection(targetIndex)
                        } else {
                            // Automatically select first available station
                            selectedStationId = stations.first().id
                            binding.spinnerStation.setSelection(0)
                        }
                        loadSlotsForDate()
                    }
                }
            } catch (e: Exception) {
                // Fallback to local default stations gracefully
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 100, 0, "My Bookings")?.apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setIcon(android.R.drawable.ic_menu_agenda)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 100) {
            val intent = Intent(this, BookingHistoryActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onResume() {
        super.onResume()
        // Reload slots when returning to this screen to reflect backend changes
        loadSlotsForDate()
    }
    
    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateLabel()
                loadSlotsForDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Phase 4.1: Defensive 7-Day Rule Constraint
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis
        
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.DAY_OF_YEAR, 7)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        
        datePickerDialog.show()
    }
    
    private fun updateDateLabel() {
        val formattedDate = dateFormatter.format(calendar.time)
        binding.tvSelectedDate.text = getString(R.string.label_selected_date, formattedDate)
    }
    
    /**
     * Loads available energy slots for the selected station and date from API.
     */
    private fun loadSlotsForDate() {
        val date = calendar.time
        viewModel.loadSlots(selectedStationId, date)
    }
    
    /**
     * RecyclerView Adapter for energy time slots.
     */
    inner class SlotAdapter(
        private val slots: List<EnergySlot>,
        private val onSlotClick: (EnergySlot) -> Unit
    ) : RecyclerView.Adapter<SlotAdapter.SlotViewHolder>() {
        
        inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
            val tvStationBay: TextView = itemView.findViewById(R.id.tvStationBay)
            val chipAvailability: Chip = itemView.findViewById(R.id.chipAvailability)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_slot, parent, false)
            return SlotViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
            val slot = slots[position]
            holder.tvTimeRange.text = slot.timeRange
            holder.tvStationBay.text = "${slot.stationId} — ${slot.batterySlotId}"
            
            if (slot.isAvailable) {
                holder.chipAvailability.text = getString(R.string.label_available)
                holder.chipAvailability.setChipBackgroundColorResource(R.color.status_approved_container)
                holder.chipAvailability.setTextColor(getColor(R.color.status_approved_text))
                holder.itemView.setOnClickListener { onSlotClick(slot) }
                holder.itemView.alpha = 1.0f
            } else {
                holder.chipAvailability.text = "Booked"
                holder.chipAvailability.setChipBackgroundColorResource(R.color.status_cancelled_container)
                holder.chipAvailability.setTextColor(getColor(R.color.status_cancelled_text))
                holder.itemView.setOnClickListener(null)
                holder.itemView.alpha = 0.5f
            }
        }
        
        override fun getItemCount(): Int = slots.size
    }
}
