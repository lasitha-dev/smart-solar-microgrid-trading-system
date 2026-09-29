/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Domain and network data model representing a microgrid station node with GPS coordinates and distance.
 * Author: Member 2
 */

package com.sliit.ssmts.microgrid_nodes.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Description: Represents geographic GPS coordinates and address of a solar substation.
 * Author: Member 2
 */
data class StationLocation(
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lng") val lng: Double = 0.0,
    @SerializedName("address") val address: String = ""
)

/**
 * Description: Domain and DTO entity mapping to the C# API NearbyStationDto for map plotting and discovery.
 * Author: Member 2
 */
data class MicrogridStation(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("stationName")
    val stationName: String = "",

    @SerializedName("location")
    val location: StationLocation? = null,

    @SerializedName("lat")
    private val rawLat: Double? = null,

    @SerializedName("lng")
    private val rawLng: Double? = null,

    @SerializedName("capacityKwh")
    val capacityKwh: Double = 0.0,

    @SerializedName("availableBatterySlots")
    val availableBatterySlots: Int = 0,

    @SerializedName("totalBatterySlots")
    val totalBatterySlots: Int = 0,

    @SerializedName("status")
    val status: String = "Active",

    @SerializedName("distanceKm")
    val distanceKm: Double = 0.0
) {
    /**
     * Gets latitude coordinate, resolving from root field or nested location object.
     */
    val lat: Double
        get() = rawLat ?: location?.lat ?: 0.0

    /**
     * Gets longitude coordinate, resolving from root field or nested location object.
     */
    val lng: Double
        get() = rawLng ?: location?.lng ?: 0.0

    /**
     * Secondary convenience constructor for manual instantiation.
     */
    constructor(
        id: String,
        stationName: String,
        lat: Double,
        lng: Double,
        capacityKwh: Double,
        availableBatterySlots: Int,
        status: String,
        distanceKm: Double
    ) : this(
        id = id,
        stationName = stationName,
        location = StationLocation(lat, lng),
        rawLat = lat,
        rawLng = lng,
        capacityKwh = capacityKwh,
        availableBatterySlots = availableBatterySlots,
        status = status,
        distanceKm = distanceKm
    )
}
