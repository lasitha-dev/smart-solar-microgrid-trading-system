/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Interactive Leaflet-powered Location Picker Activity providing real-time map canvas and GPS integration.
 */

package com.sliit.ssmts.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityLocationPickerBinding
import com.sliit.ssmts.util.Constants
import java.util.Locale

/**
 * Interactive map screen for pinning solar microgrid facility coordinates.
 * Features an embedded Leaflet.js canvas inside Android WebView, bidirectional JS interface,
 * device GPS location acquisition, and regional Sri Lankan microgrid cluster presets.
 */
class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding

    private var currentLatitude: Double = DEFAULT_LAT
    private var currentLongitude: Double = DEFAULT_LON
    private var isMapLoaded: Boolean = false
    private var isReadOnly: Boolean = false

    // Sri Lankan Microgrid Regional Clusters for rapid zone selection
    private val microgridClusters = listOf(
        ClusterZone("Colombo Central Microgrid Node", 6.9271, 79.8612),
        ClusterZone("Kandy Valley Solar Grid", 7.2906, 80.6337),
        ClusterZone("Galle Southern Coastal Solar Cluster", 6.0535, 80.2210),
        ClusterZone("Jaffna Peninsula Solar Array", 9.6615, 80.0255),
        ClusterZone("Kurunegala Solar Microgrid Hub", 7.4863, 80.3623),
        ClusterZone("Anuradhapura Solar Generation Park", 8.3114, 80.4037),
        ClusterZone("Hambantota Solar Power Zone", 6.1429, 81.1212)
    )

    private data class ClusterZone(val name: String, val lat: Double, val lon: Double)

    // Runtime Permission Launcher for GPS Location
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            fetchDeviceGpsLocation()
        } else {
            Toast.makeText(this, getString(R.string.permission_location_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read initial coordinates and mode from Intent if provided
        val initialLat = intent.getDoubleExtra(Constants.EXTRA_LATITUDE, Double.NaN)
        val initialLon = intent.getDoubleExtra(Constants.EXTRA_LONGITUDE, Double.NaN)
        isReadOnly = intent.getBooleanExtra(Constants.EXTRA_READ_ONLY, false)

        if (!initialLat.isNaN() && !initialLon.isNaN() && initialLat in -90.0..90.0 && initialLon in -180.0..180.0) {
            currentLatitude = initialLat
            currentLongitude = initialLon
        }

        updateCoordinatesDisplay(currentLatitude, currentLongitude)
        setupWebView()
        setupListeners()
        applyModeUi()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(binding.wvMapPicker.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        binding.wvMapPicker.webChromeClient = WebChromeClient()
        binding.wvMapPicker.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                sendLocationToMap(currentLatitude, currentLongitude, 14)
            }
        }

        // Register bidirectional JavaScript bridge
        binding.wvMapPicker.addJavascriptInterface(
            AndroidLocationBridge { lat, lon ->
                runOnUiThread {
                    currentLatitude = lat
                    currentLongitude = lon
                    updateCoordinatesDisplay(lat, lon)
                }
            },
            "AndroidLocationBridge"
        )

        binding.wvMapPicker.loadUrl("file:///android_asset/map_picker.html")
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.fabFindMe.setOnClickListener {
            checkAndRequestLocationPermissions()
        }

        binding.fabClusters.setOnClickListener {
            showClusterZonesDialog()
        }

        binding.btnConfirmLocation.setOnClickListener {
            confirmLocationAndReturn()
        }
    }

    private fun applyModeUi() {
        if (isReadOnly) {
            binding.tvLocationPickerTitle.text = "Solar Facility Location"
            binding.tvLocationPickerSubtitle.text = "Registered microgrid node coordinates on map"
            binding.cardBottomActions.visibility = View.GONE
            binding.fabClusters.visibility = View.GONE
            binding.fabFindMe.visibility = View.GONE
            binding.tvCoordinatesRegion.text = "Registered Solar Facility Node (Locked)"
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchDeviceGpsLocation()
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
    private fun fetchDeviceGpsLocation() {
        binding.tvGpsStatus.text = getString(R.string.msg_gps_searching)
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Toast.makeText(this, getString(R.string.msg_gps_failed), Toast.LENGTH_SHORT).show()
            binding.tvGpsStatus.text = "GPS UNAVAILABLE"
            return
        }

        var bestLocation: Location? = null

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLoc != null) bestLocation = gpsLoc
            }

            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (netLoc != null) bestLocation = netLoc
            }

            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                val passiveLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (passiveLoc != null) bestLocation = passiveLoc
            }
        } catch (_: Exception) {
            // Handled gracefully below
        }

        if (bestLocation != null) {
            currentLatitude = bestLocation.latitude
            currentLongitude = bestLocation.longitude
            updateCoordinatesDisplay(currentLatitude, currentLongitude)
            sendLocationToMap(currentLatitude, currentLongitude, 16)
            binding.tvGpsStatus.text = "GPS LOCKED"
            Toast.makeText(this, getString(R.string.msg_gps_success), Toast.LENGTH_SHORT).show()
        } else {
            // Fallback: If emulator does not have immediate fix, focus on default cluster
            currentLatitude = DEFAULT_LAT
            currentLongitude = DEFAULT_LON
            updateCoordinatesDisplay(currentLatitude, currentLongitude)
            sendLocationToMap(currentLatitude, currentLongitude, 14)
            binding.tvGpsStatus.text = "REGIONAL BASE"
            Toast.makeText(this, getString(R.string.msg_gps_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showClusterZonesDialog() {
        val clusterItems = microgridClusters.map {
            "${it.name}\n(${String.format(Locale.US, "%.4f", it.lat)}, ${String.format(Locale.US, "%.4f", it.lon)})"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_location_picker_title))
            .setItems(clusterItems) { _, which ->
                if (which in microgridClusters.indices) {
                    val cluster = microgridClusters[which]
                    currentLatitude = cluster.lat
                    currentLongitude = cluster.lon
                    updateCoordinatesDisplay(currentLatitude, currentLongitude)
                    sendLocationToMap(currentLatitude, currentLongitude, 14)
                    binding.tvCoordinatesRegion.text = "Microgrid Zone: ${cluster.name}"
                    Toast.makeText(this, "Centered: ${cluster.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun sendLocationToMap(lat: Double, lon: Double, zoom: Int) {
        if (isMapLoaded) {
            val script = String.format(Locale.US, "setLocation(%.6f, %.6f, %d);", lat, lon, zoom)
            binding.wvMapPicker.evaluateJavascript(script, null)
        }
    }

    private fun updateCoordinatesDisplay(lat: Double, lon: Double) {
        binding.tvCoordinatesLive.text = String.format(
            Locale.US,
            "Lat: %.5f°   Lon: %.5f°",
            lat,
            lon
        )
    }

    private fun confirmLocationAndReturn() {
        val resultIntent = Intent().apply {
            putExtra(Constants.EXTRA_LATITUDE, currentLatitude)
            putExtra(Constants.EXTRA_LONGITUDE, currentLongitude)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        Toast.makeText(this, getString(R.string.msg_location_confirmed_toast), Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        binding.wvMapPicker.removeJavascriptInterface("AndroidLocationBridge")
        binding.wvMapPicker.destroy()
        super.onDestroy()
    }

    /**
     * JavaScript Interface bridge receiving real-time coordinates from Leaflet map touch/drag events.
     */
    class AndroidLocationBridge(private val onLocationChanged: (Double, Double) -> Unit) {
        @JavascriptInterface
        fun onLocationSelected(lat: Double, lon: Double) {
            onLocationChanged(lat, lon)
        }
    }

    companion object {
        private const val DEFAULT_LAT = 6.9271 // Colombo, Sri Lanka
        private const val DEFAULT_LON = 79.8612
    }
}
