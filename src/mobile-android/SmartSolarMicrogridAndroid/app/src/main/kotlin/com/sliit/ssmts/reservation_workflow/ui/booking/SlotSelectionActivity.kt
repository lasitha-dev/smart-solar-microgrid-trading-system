/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Activity for selecting an available energy slot (Date & Time) with dynamic station selector and live capacity filtering.
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

    private val stations = mutableListOf<StationItem>()
    private var selectedStationId: String = ""
    private lateinit var stationAdapter: ArrayAdapter<StationItem>

    data class StationItem(val name: String, val id: String) {
        override fun toString(): String = name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySlotSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)

        // Check if a station ID was passed via Map intent
        val passedStationId = intent.getStringExtra(EXTRA_STATION_ID) ?: intent.getStringExtra("STATION_ID")
        val passedStationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: intent.getStringExtra("STATION_NAME")
        if (!passedStationId.isNullOrBlank()) {
            selectedStationId = passedStationId
            stations.add(StationItem(passedStationName ?: "Selected Station", passedStationId))
        } else {
            stations.add(StationItem("Loading stations...", ""))
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

        binding.spinnerStation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in stations.indices && stations[position].id.isNotBlank()) {
                    val newStationId = stations[position].id
                    if (newStationId != selectedStationId) {
                        selectedStationId = newStationId
                        loadSlotsForDate()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set up initial date label
        updateDateLabel()
        
        // Setup RecyclerView
        binding.rvTimeSlots.layoutManager = LinearLayoutManager(this)
        
        // Initialize ViewModel
        val repository = DependencyProvider.getReservationRepository(this)
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ReservationViewModel::class.java]
        
        // Observe Slots State
        lifecycleScope.launch {
            viewModel.slotsState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        binding.progressSlotsLoading.visibility = View.GONE
                        binding.rvTimeSlots.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.GONE
                    }
                    is UiState.Loading -> {
                        binding.progressSlotsLoading.visibility = View.VISIBLE
                        binding.rvTimeSlots.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.GONE
                    }
                    is UiState.Success -> {
                        binding.progressSlotsLoading.visibility = View.GONE
                        val slots = state.data
                        if (slots.isEmpty()) {
                            binding.tvEmptyState.text = getString(R.string.msg_no_slots)
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.rvTimeSlots.visibility = View.GONE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                            binding.rvTimeSlots.visibility = View.VISIBLE
                            val currentStationName = stations.find { it.id == selectedStationId }?.name ?: "Solar Station"
                            binding.rvTimeSlots.adapter = SlotAdapter(slots, currentStationName) { slot ->
                                val intent = Intent(this@SlotSelectionActivity, CreateReservationActivity::class.java).apply {
                                    putExtra("SLOT_INFO", "${currentStationName} | ${slot.batterySlotId} | ${slot.timeRange}")
                                    putExtra("STATION", selectedStationId)
                                    putExtra("STATION_NAME", currentStationName)
                                    putExtra("SLOT_ID", slot.id)
                                    putExtra("SLOT_DATE", slot.date)
                                    putExtra("TIME_RANGE", slot.timeRange)
                                }
                                startActivity(intent)
                            }
                        }
                    }
                    is UiState.Error -> {
                        binding.progressSlotsLoading.visibility = View.GONE
                        binding.tvEmptyState.text = "No energy slots available or network error."
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvTimeSlots.visibility = View.GONE
                        Toast.makeText(this@SlotSelectionActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // Setup DatePicker constrained to today -> today + 7 days (Defensive Rule)
        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        // Asynchronously fetch live stations from backend
        fetchLiveStations()
    }

    /**
     * Queries active microgrid stations from the central API, populates the dropdown,
     * and automatically triggers energy slot loading for the selected station.
     */
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
                    val liveList = rawData.filter { it.status.equals("Active", ignoreCase = true) }
                    val effectiveList = if (liveList.isNotEmpty()) liveList else rawData.filter { it.status != "Deactivated" }

                    if (effectiveList.isNotEmpty()) {
                        stations.clear()
                        effectiveList.forEach { s ->
                            stations.add(StationItem(s.stationName, s.id))
                        }
                        stationAdapter.notifyDataSetChanged()

                        // Retain passed or selected station if present
                        val targetIndex = stations.indexOfFirst { it.id == selectedStationId }
                        if (targetIndex >= 0) {
                            binding.spinnerStation.setSelection(targetIndex)
                        } else {
                            selectedStationId = stations.first().id
                            binding.spinnerStation.setSelection(0)
                        }
                        loadSlotsForDate()
                    }
                }
            } catch (e: Exception) {
                // If network is offline, maintain current selection if valid
                if (selectedStationId.isNotBlank()) {
                    loadSlotsForDate()
                }
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
        if (selectedStationId.isNotBlank()) {
            loadSlotsForDate()
        }
    }
    
    /**
     * Displays a DatePickerDialog constrained strictly to the 7-day operational booking window.
     */
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
        
        // 7-Day Rule Constraint: min = today, max = today + 7 days
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
        if (selectedStationId.isBlank()) return
        val date = calendar.time
        viewModel.loadSlots(selectedStationId, date)
    }
    
    /**
     * RecyclerView Adapter for energy time slots with high-contrast card and chip styling.
     */
    inner class SlotAdapter(
        private val slots: List<EnergySlot>,
        private val stationName: String,
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
            holder.tvStationBay.text = "${stationName} • ${slot.batterySlotId}"
            
            if (slot.isAvailable) {
                holder.chipAvailability.text = getString(R.string.label_available)
                holder.chipAvailability.setChipBackgroundColorResource(R.color.status_active_bg)
                holder.chipAvailability.setTextColor(getColor(R.color.status_active_text))
                holder.chipAvailability.setChipStrokeColorResource(R.color.emerald_dark)
                holder.chipAvailability.chipStrokeWidth = 2f
                holder.itemView.setOnClickListener { onSlotClick(slot) }
                holder.itemView.alpha = 1.0f
            } else {
                holder.chipAvailability.text = "Booked"
                holder.chipAvailability.setChipBackgroundColorResource(R.color.status_deactivated_bg)
                holder.chipAvailability.setTextColor(getColor(R.color.status_deactivated_text))
                holder.chipAvailability.setChipStrokeColorResource(R.color.error_red_dark)
                holder.chipAvailability.chipStrokeWidth = 2f
                holder.itemView.setOnClickListener(null)
                holder.itemView.alpha = 0.55f
            }
        }
        
        override fun getItemCount(): Int = slots.size
    }
}
