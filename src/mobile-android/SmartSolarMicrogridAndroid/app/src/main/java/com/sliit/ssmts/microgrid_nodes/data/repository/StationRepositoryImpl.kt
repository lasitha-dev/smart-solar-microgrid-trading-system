/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Repository implementation managing remote microgrid station queries and network error translation.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.data.repository

import com.google.gson.Gson
import com.sliit.ssmts.microgrid_nodes.data.remote.StationApi
import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import com.sliit.ssmts.microgrid_nodes.domain.repository.IStationRepository
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Description: Concrete implementation of [IStationRepository] interfacing with [StationApi].
 * Author: Member 2
 */
class StationRepositoryImpl(
    private val stationApi: StationApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val gson: Gson = Gson()
) : IStationRepository {

    /**
     * Fetches solar microgrid stations situated within a radial boundary of the provided coordinates.
     *
     * @param lat Geographic latitude coordinate.
     * @param lng Geographic longitude coordinate.
     * @param radiusKm Radial search boundary in kilometers.
     * @return [NetworkResult] with list of nearby stations or error outcome.
     */
    override suspend fun fetchNearbyStations(
        lat: Double,
        lng: Double,
        radiusKm: Double
    ): NetworkResult<List<MicrogridStation>> = withContext(ioDispatcher) {
        try {
            val response = stationApi.getNearbyStations(lat = lat, lng = lng, radiusKm = radiusKm)
            if (response.isSuccessful) {
                val stations = response.body() ?: emptyList()
                NetworkResult.Success(stations)
            } else {
                val errorBody = response.errorBody()?.string()
                val message = parseErrorMessage(errorBody, response.code())
                NetworkResult.Error(response.code(), message)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    /**
     * Extracts descriptive error message from API response or HTTP status fallback.
     */
    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (errorBody.isNullOrBlank()) {
            return when (statusCode) {
                400 -> "Invalid location or radius parameters."
                401 -> "Unauthorized. Please authenticate again."
                404 -> "No microgrid stations found."
                else -> "Server error occurred ($statusCode)."
            }
        }

        return try {
            val json = JSONObject(errorBody)
            json.optString("message", json.optString("Message", "Server returned error ($statusCode)."))
        } catch (_: Exception) {
            errorBody
        }
    }
}
