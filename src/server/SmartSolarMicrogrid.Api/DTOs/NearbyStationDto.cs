/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Data transfer object extending StationResponseDto with geographic distance in kilometers for map plotting.
 * Author: Member 2
 */

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Represents a station node enriched with calculated radial distance from user or reference GPS coordinates.
/// Author: Member 2
/// </summary>
public class NearbyStationDto : StationResponseDto
{
    /// <summary>
    /// Gets or sets the calculated straight-line (great-circle) distance in kilometers from the user's current GPS position.
    /// </summary>
    public double DistanceKm { get; set; }
}
