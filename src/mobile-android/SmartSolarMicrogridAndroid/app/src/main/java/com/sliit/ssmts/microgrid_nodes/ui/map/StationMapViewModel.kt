/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Android ViewModel managing GPS proximity searches, station map markers, and UI states.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.data.remote.RetrofitClient
import com.sliit.ssmts.microgrid_nodes.data.repository.StationRepositoryImpl
import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import com.sliit.ssmts.microgrid_nodes.domain.repository.IStationRepository
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Description: UI state hierarchy representing the lifecycle of nearby station map queries.
 * Author: Member 2
 */
sealed class StationMapUiState {
    object Idle : StationMapUiState()
    object Loading : StationMapUiState()
    data class Success(val stations: List<MicrogridStation>) : StationMapUiState()
    data class Error(val message: String) : StationMapUiState()
}

/**
 * Description: ViewModel orchestrating proximity queries and exposing state to NearbyStationsActivity.
 * Author: Member 2
 */
class StationMapViewModel(
    private val stationRepository: IStationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StationMapUiState>(StationMapUiState.Idle)
    val uiState: StateFlow<StationMapUiState> = _uiState.asStateFlow()

    /**
     * Queries stations located near the specified latitude and longitude within the radial boundary.
     *
     * @param lat Reference latitude coordinate.
     * @param lng Reference longitude coordinate.
     * @param radiusKm Search radius threshold in kilometers (defaults to 15.0 km).
     */
    fun loadNearbyStations(lat: Double, lng: Double, radiusKm: Double = 15.0) {
        _uiState.value = StationMapUiState.Loading

        viewModelScope.launch {
            when (val result = stationRepository.fetchNearbyStations(lat, lng, radiusKm)) {
                is NetworkResult.Success -> {
                    _uiState.value = StationMapUiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = StationMapUiState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    val fallbackMsg = result.throwable.localizedMessage ?: "Failed to connect to microgrid server."
                    _uiState.value = StationMapUiState.Error(fallbackMsg)
                }
            }
        }
    }
}

/**
 * Description: Factory class responsible for instantiating StationMapViewModel with repository dependency.
 * Author: Member 2
 */
class StationMapViewModelFactory(
    private val repository: IStationRepository
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(
        StationRepositoryImpl(RetrofitClient.getStationApi(context.applicationContext))
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationMapViewModel::class.java)) {
            return StationMapViewModel(stationRepository = repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
