/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Domain repository interface for fetching microgrid stations and proximity searches.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.domain.repository

import com.sliit.ssmts.microgrid_nodes.domain.model.MicrogridStation
import com.sliit.ssmts.util.NetworkResult

/**
 * Description: Abstraction contract for microgrid node operations following Clean Architecture.
 * Author: Member 2
 */
interface IStationRepository {

    /**
     * Fetches solar microgrid stations situated within a radial boundary of the provided coordinates.
     *
     * @param lat Geographic latitude coordinate.
     * @param lng Geographic longitude coordinate.
     * @param radiusKm Search radius boundary in kilometers.
     * @return [NetworkResult] containing list of [MicrogridStation] on success or error details on failure.
     */
    suspend fun fetchNearbyStations(
        lat: Double,
        lng: Double,
        radiusKm: Double = 15.0
    ): NetworkResult<List<MicrogridStation>>
}
