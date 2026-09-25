/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Interactive Google Maps Activity plotting active solar microgrid nodes, available battery slots, and proximity calculations.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityNearbyStationsBinding
import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Description: Map view activity displaying physical microgrid substations and real-time bay availability.
 * Author: Member 2
 */
class NearbyStationsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNearbyStationsBinding
    private val viewModel: StationMapViewModel by viewModels { StationMapViewModelFactory(this) }

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Default fallback coordinates (Colombo, Sri Lanka)
    private val defaultLocation = LatLng(6.9271, 79.8612)
    private var currentLocation: LatLng = defaultLocation
    private val markerStationMap = HashMap<Marker, MicrogridStation>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocationAndFetch()
        } else {
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
            useFallbackLocationAndFetch()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearbyStationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        initMap()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRecenter.setOnClickListener {
            checkPermissionsAndFetchLocation()
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Setup map UI
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true

        googleMap?.setOnMarkerClickListener { marker ->
            val station = markerStationMap[marker]
            if (station != null) {
                showStationDetails(station)
            }
            marker.showInfoWindow()
            false
        }

        googleMap?.setOnMapClickListener {
            binding.cardStationDetails.visibility = View.GONE
        }

        checkPermissionsAndFetchLocation()
    }

    private fun checkPermissionsAndFetchLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            enableMyLocationAndFetch()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndFetch() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) { }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                } else {
                    currentLocation = defaultLocation
                }
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 13f))
                viewModel.loadNearbyStations(currentLocation.latitude, currentLocation.longitude)
            }
            .addOnFailureListener {
                useFallbackLocationAndFetch()
            }
    }

    private fun useFallbackLocationAndFetch() {
        currentLocation = defaultLocation
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12f))
        viewModel.loadNearbyStations(currentLocation.latitude, currentLocation.longitude)
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
        googleMap?.clear()
        markerStationMap.clear()

        binding.tvStationsCount.text = getString(R.string.stations_found_format, stations.size)

        if (stations.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_stations_found, 15.0), Toast.LENGTH_SHORT).show()
            return
        }

        for (station in stations) {
            val stationLatLng = LatLng(station.lat, station.lng)
            val snippetText = getString(
                R.string.station_distance_snippet,
                station.distanceKm,
                station.availableBatterySlots
            )

            val markerOptions = MarkerOptions()
                .position(stationLatLng)
                .title(station.stationName)
                .snippet(snippetText)
                .icon(
                    if (station.availableBatterySlots > 0)
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    else
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )

            val marker = googleMap?.addMarker(markerOptions)
            if (marker != null) {
                markerStationMap[marker] = station
            }
        }
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
        } else {
            binding.tvSelectedStationStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvSelectedStationStatus.setTextColor(getColor(R.color.status_pending_text))
        }
    }
}
