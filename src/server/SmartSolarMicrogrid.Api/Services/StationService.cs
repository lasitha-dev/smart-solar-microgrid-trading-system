/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Core service implementing FAT service business rules, station CRUD, proximity queries, and reservation-guarded deactivation.
 * Author: Member 2
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Authoritative service implementation managing microgrid stations, scheduling, proximity calculations,
/// and lifecycle state transitions guarding against active reservation disruption.
/// Author: Member 2
/// </summary>
public class StationService : IStationService
{
    private readonly IMongoDbContext _dbContext;

    /// <summary>
    /// Initializes a new instance of the <see cref="StationService"/> class.
    /// </summary>
    /// <param name="dbContext">The central MongoDB database context.</param>
    /// <exception cref="ArgumentNullException">Thrown when dbContext is null.</exception>
    public StationService(IMongoDbContext dbContext)
    {
        _dbContext = dbContext ?? throw new ArgumentNullException(nameof(dbContext));
    }

    /// <summary>
    /// Registers a new microgrid station with default battery bay allocations and operating hours.
    /// </summary>
    /// <param name="dto">The station creation payload.</param>
    /// <returns>The newly created station response DTO.</returns>
    public async Task<StationResponseDto> CreateStationAsync(StationCreateDto dto)
    {
        ArgumentNullException.ThrowIfNull(dto);

        var totalSlots = dto.TotalBatterySlots > 0 ? dto.TotalBatterySlots : 4;
        var batterySlots = new List<BatterySlotInfo>();

        for (var i = 1; i <= totalSlots; i++)
        {
            batterySlots.Add(new BatterySlotInfo
            {
                SlotId = $"BAY-{i:D2}",
                IsAvailable = true
            });
        }

        var station = new SolarStationInfo
        {
            StationName = dto.StationName.Trim(),
            Location = new StationLocation
            {
                Lat = dto.Location?.Lat ?? 0.0,
                Lng = dto.Location?.Lng ?? 0.0,
                Address = dto.Location?.Address?.Trim() ?? string.Empty
            },
            CapacityKwh = dto.CapacityKwh,
            BatterySlots = batterySlots,
            Schedule = new StationSchedule
            {
                OpenTime = !string.IsNullOrWhiteSpace(dto.Schedule?.OpenTime) ? dto.Schedule.OpenTime.Trim() : "06:00",
                CloseTime = !string.IsNullOrWhiteSpace(dto.Schedule?.CloseTime) ? dto.Schedule.CloseTime.Trim() : "20:00",
                DaysActive = dto.Schedule?.DaysActive != null && dto.Schedule.DaysActive.Count > 0
                    ? dto.Schedule.DaysActive
                    : new List<string> { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" }
            },
            Status = "Active",
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _dbContext.SolarStations.InsertOneAsync(station);
        return MapToResponse(station);
    }

    /// <summary>
    /// Retrieves a station record by its unique MongoDB document identifier.
    /// </summary>
    /// <param name="id">The unique identifier of the station.</param>
    /// <returns>The formatted response DTO if found; otherwise null.</returns>
    public async Task<StationResponseDto?> GetStationByIdAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return null;

        var station = await _dbContext.SolarStations
            .Find(s => s.Id == id)
            .FirstOrDefaultAsync();

        return station != null ? MapToResponse(station) : null;
    }

    /// <summary>
    /// Retrieves all microgrid stations, sorted by creation timestamp descending.
    /// </summary>
    /// <returns>A list of formatted station response DTOs.</returns>
    public async Task<List<StationResponseDto>> GetAllStationsAsync()
    {
        var stations = await _dbContext.SolarStations
            .Find(_ => true)
            .SortByDescending(s => s.CreatedAt)
            .ToListAsync();

        return stations.Select(MapToResponse).ToList();
    }

    /// <summary>
    /// Updates attributes of an existing station with support for partial updating.
    /// </summary>
    /// <param name="id">The unique identifier of the station to update.</param>
    /// <param name="dto">The update payload containing modified fields.</param>
    /// <returns>The updated station response DTO if successful; otherwise null.</returns>
    public async Task<StationResponseDto?> UpdateStationAsync(string id, StationUpdateDto dto)
    {
        if (string.IsNullOrWhiteSpace(id) || dto == null) return null;

        var station = await _dbContext.SolarStations
            .Find(s => s.Id == id)
            .FirstOrDefaultAsync();

        if (station == null) return null;

        if (!string.IsNullOrWhiteSpace(dto.StationName))
        {
            station.StationName = dto.StationName.Trim();
        }

        if (dto.Location != null)
        {
            station.Location.Lat = dto.Location.Lat;
            station.Location.Lng = dto.Location.Lng;
            if (!string.IsNullOrWhiteSpace(dto.Location.Address))
            {
                station.Location.Address = dto.Location.Address.Trim();
            }
        }

        if (dto.CapacityKwh.HasValue && dto.CapacityKwh.Value > 0)
        {
            station.CapacityKwh = dto.CapacityKwh.Value;
        }

        if (dto.TotalBatterySlots.HasValue && dto.TotalBatterySlots.Value > 0)
        {
            var targetCount = dto.TotalBatterySlots.Value;
            var currentSlots = station.BatterySlots ?? new List<BatterySlotInfo>();

            if (targetCount > currentSlots.Count)
            {
                for (var i = currentSlots.Count + 1; i <= targetCount; i++)
                {
                    currentSlots.Add(new BatterySlotInfo
                    {
                        SlotId = $"BAY-{i:D2}",
                        IsAvailable = true
                    });
                }
            }
            else if (targetCount < currentSlots.Count)
            {
                currentSlots = currentSlots.Take(targetCount).ToList();
            }

            station.BatterySlots = currentSlots;
        }

        if (dto.Schedule != null)
        {
            if (!string.IsNullOrWhiteSpace(dto.Schedule.OpenTime))
                station.Schedule.OpenTime = dto.Schedule.OpenTime.Trim();

            if (!string.IsNullOrWhiteSpace(dto.Schedule.CloseTime))
                station.Schedule.CloseTime = dto.Schedule.CloseTime.Trim();

            if (dto.Schedule.DaysActive != null && dto.Schedule.DaysActive.Count > 0)
                station.Schedule.DaysActive = dto.Schedule.DaysActive;
        }

        if (!string.IsNullOrWhiteSpace(dto.Status))
        {
            var targetStatus = dto.Status.Trim();
            if (string.Equals(targetStatus, "Inactive", StringComparison.OrdinalIgnoreCase))
            {
                await ValidateDeactivationEligibilityAsync(id);
            }

            station.Status = targetStatus;
        }

        station.UpdatedAt = DateTime.UtcNow;

        await _dbContext.SolarStations.ReplaceOneAsync(s => s.Id == id, station);
        return MapToResponse(station);
    }

    /// <summary>
    /// Calculates straight-line distance from reference coordinates using the Haversine formula
    /// and returns nearby stations within the specified radius sorted by closest first.
    /// </summary>
    /// <param name="lat">Reference latitude coordinate.</param>
    /// <param name="lng">Reference longitude coordinate.</param>
    /// <param name="radiusKm">Maximum radial distance in kilometers (defaults to 50 km if &lt;= 0).</param>
    /// <returns>A list of nearby stations with calculated distances.</returns>
    public async Task<List<NearbyStationDto>> GetNearbyStationsAsync(double lat, double lng, double radiusKm)
    {
        var effectiveRadius = radiusKm > 0 ? radiusKm : 50.0;

        // Retrieve active stations (or all if none active)
        var stations = await _dbContext.SolarStations
            .Find(s => s.Status == "Active")
            .ToListAsync();

        if (stations.Count == 0)
        {
            stations = await _dbContext.SolarStations.Find(_ => true).ToListAsync();
        }

        var nearbyList = new List<NearbyStationDto>();

        foreach (var station in stations)
        {
            var distance = CalculateHaversineDistanceKm(lat, lng, station.Location.Lat, station.Location.Lng);

            if (distance <= effectiveRadius)
            {
                nearbyList.Add(MapToNearby(station, distance));
            }
        }

        return nearbyList.OrderBy(s => s.DistanceKm).ToList();
    }

    /// <summary>
    /// Deactivates a microgrid station enforcing the strict FAT Service rule:
    /// Station cannot be deactivated if it has any Pending or Approved reservations.
    /// </summary>
    /// <param name="id">The unique identifier of the station.</param>
    /// <returns>True if deactivated; false if station not found.</returns>
    /// <exception cref="InvalidOperationException">Thrown when active reservations exist for this station.</exception>
    public async Task<bool> DeactivateStationAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return false;

        var station = await _dbContext.SolarStations
            .Find(s => s.Id == id)
            .FirstOrDefaultAsync();

        if (station == null) return false;

        // Enforce FAT Service Invariant: Validate that zero Pending or Approved reservations exist
        await ValidateDeactivationEligibilityAsync(id);

        var update = Builders<SolarStationInfo>.Update
            .Set(s => s.Status, "Inactive")
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        var result = await _dbContext.SolarStations.UpdateOneAsync(s => s.Id == id, update);
        return result.ModifiedCount > 0;
    }

    /// <summary>
    /// Helper method querying EnergyReservations to guard against node deactivation when active bookings exist.
    /// </summary>
    /// <param name="stationId">The station identifier to inspect.</param>
    /// <exception cref="InvalidOperationException">Thrown with standardized message if active bookings exist.</exception>
    private async Task ValidateDeactivationEligibilityAsync(string stationId)
    {
        var filter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.StationId, stationId),
            Builders<EnergyReservation>.Filter.In(r => r.Status, new[] { "Pending", "Approved" })
        );

        var hasActiveReservations = await _dbContext.EnergyReservations
            .Find(filter)
            .AnyAsync();

        if (hasActiveReservations)
        {
            throw new InvalidOperationException("Cannot deactivate node: Active reservations exist.");
        }
    }

    /// <summary>
    /// Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
    /// </summary>
    /// <param name="lat1">Latitude of point 1 in degrees.</param>
    /// <param name="lon1">Longitude of point 1 in degrees.</param>
    /// <param name="lat2">Latitude of point 2 in degrees.</param>
    /// <param name="lon2">Longitude of point 2 in degrees.</param>
    /// <returns>Distance in kilometers.</returns>
    public static double CalculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2)
    {
        const double earthRadiusKm = 6371.0;

        var dLat = ToRadians(lat2 - lat1);
        var dLon = ToRadians(lon2 - lon1);

        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(ToRadians(lat1)) * Math.Cos(ToRadians(lat2)) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);

        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private static double ToRadians(double degrees) => degrees * Math.PI / 180.0;

    /// <summary>
    /// Maps a SolarStationInfo database document to a client-facing StationResponseDto.
    /// </summary>
    private static StationResponseDto MapToResponse(SolarStationInfo station)
    {
        return new StationResponseDto
        {
            Id = station.Id ?? string.Empty,
            StationName = station.StationName,
            Location = new StationLocationDto
            {
                Lat = station.Location?.Lat ?? 0.0,
                Lng = station.Location?.Lng ?? 0.0,
                Address = station.Location?.Address ?? string.Empty
            },
            CapacityKwh = station.CapacityKwh,
            BatterySlots = station.BatterySlots?.Select(b => new BatterySlotDto
            {
                SlotId = b.SlotId,
                IsAvailable = b.IsAvailable
            }).ToList() ?? new List<BatterySlotDto>(),
            Schedule = new StationScheduleDto
            {
                OpenTime = station.Schedule?.OpenTime ?? "06:00",
                CloseTime = station.Schedule?.CloseTime ?? "20:00",
                DaysActive = station.Schedule?.DaysActive ?? new List<string>()
            },
            Status = station.Status,
            CreatedAt = station.CreatedAt,
            UpdatedAt = station.UpdatedAt
        };
    }

    /// <summary>
    /// Maps a SolarStationInfo database document to a NearbyStationDto with calculated distance.
    /// </summary>
    private static NearbyStationDto MapToNearby(SolarStationInfo station, double distanceKm)
    {
        return new NearbyStationDto
        {
            Id = station.Id ?? string.Empty,
            StationName = station.StationName,
            Location = new StationLocationDto
            {
                Lat = station.Location?.Lat ?? 0.0,
                Lng = station.Location?.Lng ?? 0.0,
                Address = station.Location?.Address ?? string.Empty
            },
            CapacityKwh = station.CapacityKwh,
            BatterySlots = station.BatterySlots?.Select(b => new BatterySlotDto
            {
                SlotId = b.SlotId,
                IsAvailable = b.IsAvailable
            }).ToList() ?? new List<BatterySlotDto>(),
            Schedule = new StationScheduleDto
            {
                OpenTime = station.Schedule?.OpenTime ?? "06:00",
                CloseTime = station.Schedule?.CloseTime ?? "20:00",
                DaysActive = station.Schedule?.DaysActive ?? new List<string>()
            },
            Status = station.Status,
            CreatedAt = station.CreatedAt,
            UpdatedAt = station.UpdatedAt,
            DistanceKm = Math.Round(distanceKm, 2)
        };
    }
}
