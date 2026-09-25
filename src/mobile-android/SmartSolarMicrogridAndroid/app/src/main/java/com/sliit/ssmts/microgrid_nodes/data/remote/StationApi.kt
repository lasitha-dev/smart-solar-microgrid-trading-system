/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Retrofit REST API interface for querying nearby microgrid station nodes and availability.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.data.remote

import com.sliit.ssmts.data.remote.dto.ApiResponse
import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Description: Retrofit contract for microgrid node discovery and proximity search endpoints.
 * Author: Member 2
 */
interface StationApi {

    /**
     * Queries solar microgrid stations within a radial distance from reference GPS coordinates.
     *
     * @param lat Geographic latitude coordinate of user/device.
     * @param lng Geographic longitude coordinate of user/device.
     * @param radiusKm Radial search distance threshold in kilometers (default 15.0 km).
     * @return Retrofit [Response] wrapping standard [ApiResponse] with a list of [MicrogridStation] objects sorted by distance.
     */
    @GET("api/stations/nearby")
    suspend fun getNearbyStations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 15.0
    ): Response<ApiResponse<List<MicrogridStation>>>
}
