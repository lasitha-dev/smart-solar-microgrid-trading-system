/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Interface defining business logic operations for solar microgrid stations, scheduling, and geospatial discovery.
 * Author: Member 2
 */

using System.Collections.Generic;
using System.Threading.Tasks;
using SmartSolarMicrogrid.Api.DTOs;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Contract for solar microgrid station management, lifecycle state enforcement, and proximity calculations.
/// Author: Member 2
/// </summary>
public interface IStationService
{
    /// <summary>
    /// Registers a new solar microgrid station node with initialized battery bays and schedules.
    /// </summary>
    /// <param name="dto">The creation payload with station attributes.</param>
    /// <returns>The newly created station response DTO.</returns>
    Task<StationResponseDto> CreateStationAsync(StationCreateDto dto);

    /// <summary>
    /// Retrieves a single microgrid station by its unique identifier.
    /// </summary>
    /// <param name="id">The unique MongoDB ObjectId string of the station.</param>
    /// <returns>The station response DTO if found; otherwise null.</returns>
    Task<StationResponseDto?> GetStationByIdAsync(string id);

    /// <summary>
    /// Retrieves all microgrid stations sorted by latest creation.
    /// </summary>
    /// <returns>A list of all station records.</returns>
    Task<List<StationResponseDto>> GetAllStationsAsync();

    /// <summary>
    /// Updates attributes of an existing station with support for partial updating.
    /// </summary>
    /// <param name="id">The unique identifier of the station.</param>
    /// <param name="dto">The partial update payload.</param>
    /// <returns>The updated station response DTO if found; otherwise null.</returns>
    Task<StationResponseDto?> UpdateStationAsync(string id, StationUpdateDto dto);

    /// <summary>
    /// Discovers nearby microgrid stations within a specified radius using the Haversine formula.
    /// </summary>
    /// <param name="lat">User GPS latitude coordinate.</param>
    /// <param name="lng">User GPS longitude coordinate.</param>
    /// <param name="radiusKm">Maximum search radius in kilometers.</param>
    /// <returns>A list of nearby stations enriched with distance in kilometers, ordered by proximity.</returns>
    Task<List<NearbyStationDto>> GetNearbyStationsAsync(double lat, double lng, double radiusKm);

    /// <summary>
    /// Deactivates a microgrid station enforcing the strict FAT Service rule:
    /// Station cannot be deactivated if it has any Pending or Approved reservations.
    /// </summary>
    /// <param name="id">The unique identifier of the station to deactivate.</param>
    /// <returns>True if successfully deactivated; false if station was not found.</returns>
    /// <exception cref="System.InvalidOperationException">Thrown when active reservations exist for this station.</exception>
    Task<bool> DeactivateStationAsync(string id);
}
