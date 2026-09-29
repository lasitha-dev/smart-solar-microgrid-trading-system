/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Interactive OpenStreetMap (osmdroid) Activity plotting active solar microgrid nodes and available battery slots relative to the Prosumer's registered solar grid location.
 * Author: Member 2
 */

@file:Suppress("DEPRECATION")

package com.sliit.ssmts.microgrid_nodes.ui.map

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityNearbyStationsBinding
import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import com.sliit.ssmts.util.SessionManager
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * Description: Map view activity displaying physical microgrid substations and real-time bay availability
 * using OpenStreetMap (osmdroid), calculated relative to the logged-in prosumer's registered home solar grid coordinates.
 * Author: Member 2
 */
class NearbyStationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearbyStationsBinding
    private val viewModel: StationMapViewModel by viewModels { StationMapViewModelFactory(this) }
    private lateinit var sessionManager: SessionManager

    // Default fallback coordinates (Colombo, Sri Lanka)
    companion object {
        private const val DEFAULT_LAT = 6.9271
        private const val DEFAULT_LNG = 79.8612
        private const val SEARCH_RADIUS_KM = 15.0
        private const val DEFAULT_ZOOM_LEVEL = 14.0
    }

    private var homeLocation: GeoPoint = GeoPoint(DEFAULT_LAT, DEFAULT_LNG)
    private var homeMarker: Marker? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityNearbyStationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(applicationContext)

        setupUI()
        setupMapView()
        observeViewModel()
        fetchSavedLocationAndLoadStations()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRecenter.setOnClickListener {
            binding.map.controller.animateTo(homeLocation)
            viewModel.loadNearbyStations(homeLocation.latitude, homeLocation.longitude, SEARCH_RADIUS_KM)
        }
    }

    private fun setupMapView() {
        binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM_LEVEL)
            controller.setCenter(homeLocation)
        }

        // Tap on map background to dismiss bottom details card
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                binding.cardStationDetails.visibility = View.GONE
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        binding.map.overlays.add(mapEventsOverlay)
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    /**
     * Retrieves the prosumer's registered solar grid GPS coordinates from local SQLite session,
     * centers the map on the prosumer's home node, and queries nearby microgrid substations.
     */
    private fun fetchSavedLocationAndLoadStations() {
        lifecycleScope.launch {
            val session = sessionManager.getActiveSession()
            val savedLat = session?.latitude
            val savedLng = session?.longitude

            val targetLat = if (savedLat != null && savedLat != 0.0) savedLat else DEFAULT_LAT
            val targetLng = if (savedLng != null && savedLng != 0.0) savedLng else DEFAULT_LNG

            homeLocation = GeoPoint(targetLat, targetLng)

            binding.map.controller.setCenter(homeLocation)
            binding.map.controller.animateTo(homeLocation)
            renderHomeMarker()

            viewModel.loadNearbyStations(targetLat, targetLng, SEARCH_RADIUS_KM)
        }
    }

    /**
     * Renders a distinct Blue pin representing the prosumer's registered solar home facility.
     */
    private fun renderHomeMarker() {
        homeMarker?.let { binding.map.overlays.remove(it) }

        val marker = Marker(binding.map).apply {
            position = homeLocation
            title = getString(R.string.my_solar_grid)
            snippet = getString(R.string.my_solar_grid_desc)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            ContextCompat.getDrawable(this@NearbyStationsActivity, R.drawable.ic_home_marker)?.let {
                icon = it
            }
            setOnMarkerClickListener { m, _ ->
                binding.cardStationDetails.visibility = View.GONE
                m.showInfoWindow()
                true
            }
        }

        homeMarker = marker
        binding.map.overlays.add(marker)
        binding.map.invalidate()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StationMapUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        is StationMapUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvStationsCount.text = getString(R.string.loading_stations)
                        }
                        is StationMapUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            renderStationsOnMap(state.stations)
                        }
                        is StationMapUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStationsCount.text = state.message
                            Toast.makeText(this@NearbyStationsActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderStationsOnMap(stations: List<MicrogridStation>) {
        // Clear previous overlays while keeping background tap listener
        binding.map.overlays.clear()

        // Re-add tap overlay to dismiss card on background click
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                binding.cardStationDetails.visibility = View.GONE
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        binding.map.overlays.add(mapEventsOverlay)

        // Always re-add the prosumer home solar grid marker
        renderHomeMarker()

        binding.tvStationsCount.text = getString(R.string.stations_found_format, stations.size)

        if (stations.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_stations_found, SEARCH_RADIUS_KM), Toast.LENGTH_SHORT).show()
            binding.map.invalidate()
            return
        }

        val stationIcon = ContextCompat.getDrawable(this, R.drawable.ic_station_marker)

        for (station in stations) {
            val stationPoint = GeoPoint(station.lat, station.lng)
            val snippetText = getString(
                R.string.station_distance_snippet,
                station.distanceKm,
                station.availableBatterySlots
            )

            val marker = Marker(binding.map).apply {
                position = stationPoint
                title = station.stationName
                snippet = snippetText
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (stationIcon != null) {
                    icon = stationIcon
                }

                // CRITICAL UI FIX: Set an setOnMarkerClickListener for the station markers.
                // When clicked, populate the cardStationDetails TextViews and set the card to View.VISIBLE.
                // Return true to consume the click.
                setOnMarkerClickListener { clickedMarker, _ ->
                    showStationDetails(station)
                    clickedMarker.showInfoWindow()
                    true
                }
            }

            binding.map.overlays.add(marker)
        }

        binding.map.invalidate()
    }

    private fun showStationDetails(station: MicrogridStation) {
        binding.cardStationDetails.visibility = View.VISIBLE
        binding.tvSelectedStationName.text = station.stationName
        binding.tvSelectedStationDistance.text = String.format(Locale.US, "Distance: %.2f km", station.distanceKm)
        binding.tvSelectedStationCapacity.text = String.format(Locale.US, "Capacity: %.1f kWh", station.capacityKwh)
        binding.tvSelectedStationBays.text = String.format(
            Locale.US,
            "Available Bays: %d / %d",
            station.availableBatterySlots,
            station.totalBatterySlots
        )
        binding.tvSelectedStationStatus.text = station.status.uppercase()

        if (station.status.equals("Active", ignoreCase = true)) {
            binding.tvSelectedStationStatus.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvSelectedStationStatus.setTextColor(getColor(R.color.status_active_text))
            binding.btnBookAtStation.visibility = View.VISIBLE
            binding.btnBookAtStation.setOnClickListener {
                val intent = Intent(this, com.sliit.ssmts.reservation_workflow.ui.booking.SlotSelectionActivity::class.java).apply {
                    putExtra(com.sliit.ssmts.reservation_workflow.ui.booking.SlotSelectionActivity.EXTRA_STATION_ID, station.id)
                    putExtra(com.sliit.ssmts.reservation_workflow.ui.booking.SlotSelectionActivity.EXTRA_STATION_NAME, station.stationName)
                }
                startActivity(intent)
            }
        } else {
            binding.tvSelectedStationStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvSelectedStationStatus.setTextColor(getColor(R.color.status_pending_text))
            binding.btnBookAtStation.visibility = View.GONE
        }
    }
}
