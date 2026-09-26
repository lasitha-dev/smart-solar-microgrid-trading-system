/*
 * Student Role: Member 3 & Member 4
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Energy Reservation Workflow & Operator Dashboard
 * Description: Concrete MongoDB repository implementation for querying, aggregating, and mutating EnergyReservation records.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories;

/// <summary>
/// Description: Implements data persistence operations targeting the EnergyReservation and EnergyBookingSlots MongoDB collections.
/// </summary>
public class ReservationRepository : IReservationRepository
{
    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<EnergyBookingSlot> _slots;
    private readonly IMongoCollection<SolarStationInfo>? _solarStations;

    public ReservationRepository(IMongoDbContext context)
    {
        _reservations = context.EnergyReservations;
        _slots = context.EnergyBookingSlots;
        _solarStations = context.SolarStations;
    }

    /// <summary>
    /// Retrieves a reservation by its unique identifier.
    /// </summary>
    public async Task<EnergyReservation?> GetByIdAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return null;
        return await _reservations.Find(r => r.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Retrieves reservations filtered optionally by prosumer ID and status.
    /// </summary>
    public async Task<IEnumerable<EnergyReservation>> GetAllAsync(string? prosumerId = null, string? status = null)
    {
        var filterBuilder = Builders<EnergyReservation>.Filter;
        var filter = filterBuilder.Empty;

        if (!string.IsNullOrWhiteSpace(prosumerId) && !string.Equals(prosumerId, "all", StringComparison.OrdinalIgnoreCase))
        {
            filter &= filterBuilder.Eq(r => r.ProsumerId, prosumerId);
        }

        if (!string.IsNullOrWhiteSpace(status) && !string.Equals(status, "all", StringComparison.OrdinalIgnoreCase))
        {
            filter &= filterBuilder.Regex(r => r.Status, new BsonRegularExpression($"^{Regex.Escape(status)}$", "i"));
        }

        return await _reservations.Find(filter)
            .SortByDescending(r => r.ScheduledDateTime)
            .ToListAsync();
    }

    /// <summary>
    /// Retrieves a reservation matching the encoded QR payload string.
    /// </summary>
    public async Task<EnergyReservation?> GetByQrCodeAsync(string qrCode)
    {
        if (string.IsNullOrWhiteSpace(qrCode)) return null;
        return await _reservations.Find(r => r.QrCode == qrCode).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Atomically updates a reservation state to Completed and records metered energy and operator audit information.
    /// </summary>
    public async Task<bool> FinalizeTransferAsync(string id, double meteredKwh, string operatorId, string? notes, DateTime finalizedAt)
    {
        var filter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Id, id),
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved")
        );

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Completed")
            .Set(r => r.MeteredEnergyKwh, meteredKwh)
            .Set(r => r.FinalizedBy, operatorId)
            .Set(r => r.FinalizedAt, finalizedAt)
            .Set(r => r.Notes, notes)
            .Set(r => r.UpdatedAt, finalizedAt);

        var result = await _reservations.UpdateOneAsync(filter, update);
        return result.ModifiedCount > 0;
    }

    /// <summary>
    /// Inserts a new reservation document into MongoDB.
    /// </summary>
    public async Task CreateAsync(EnergyReservation reservation)
    {
        await _reservations.InsertOneAsync(reservation);
    }

    /// <summary>
    /// Updates an existing reservation document in MongoDB.
    /// </summary>
    public async Task UpdateAsync(EnergyReservation reservation)
    {
        await _reservations.ReplaceOneAsync(r => r.Id == reservation.Id, reservation);
    }

    /// <summary>
    /// Deletes a reservation by its unique identifier.
    /// </summary>
    public async Task DeleteAsync(string id)
    {
        await _reservations.DeleteOneAsync(r => r.Id == id);
    }

    /// <summary>
    /// Retrieves a booking slot by its unique identifier.
    /// </summary>
    public async Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId)
    {
        if (string.IsNullOrWhiteSpace(slotId)) return null;
        return await _slots.Find(s => s.Id == slotId).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Retrieves available energy booking slots for a given station and date.
    /// </summary>
    public async Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date)
    {
        var targetDateUtc = DateTime.SpecifyKind(date.Date, DateTimeKind.Utc);
        var nextDateUtc = targetDateUtc.AddDays(1);

        var filter = Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId) &
                     Builders<EnergyBookingSlot>.Filter.Gte(s => s.SlotDate, targetDateUtc) &
                     Builders<EnergyBookingSlot>.Filter.Lt(s => s.SlotDate, nextDateUtc);

        var existingSlots = await _slots.Find(filter)
            .SortBy(s => s.StartTime)
            .ToListAsync();

        if (existingSlots != null && existingSlots.Count > 0)
        {
            return existingSlots;
        }

        // On-demand slot generation: If no booking slots exist for this station & date, generate them based on the station's configuration
        var station = _solarStations != null
            ? await _solarStations.Find(s => s.Id == stationId).FirstOrDefaultAsync()
            : null;

        var bays = station?.BatterySlots?.Where(b => b.IsAvailable).Select(b => b.SlotId).ToList();
        if (bays == null || bays.Count == 0)
        {
            bays = new List<string> { "BAY-01", "BAY-02", "BAY-03", "BAY-04" };
        }

        var defaultIntervals = new (TimeSpan Start, TimeSpan End)[]
        {
            (new TimeSpan(8, 0, 0), new TimeSpan(9, 0, 0)),
            (new TimeSpan(10, 0, 0), new TimeSpan(11, 0, 0)),
            (new TimeSpan(13, 0, 0), new TimeSpan(14, 0, 0)),
            (new TimeSpan(15, 0, 0), new TimeSpan(16, 0, 0))
        };

        var slotsToInsert = new List<EnergyBookingSlot>();
        var now = DateTime.UtcNow;

        for (int i = 0; i < defaultIntervals.Length; i++)
        {
            var bayId = bays[i % bays.Count];
            slotsToInsert.Add(new EnergyBookingSlot
            {
                StationId = stationId,
                SlotDate = targetDateUtc,
                StartTime = defaultIntervals[i].Start,
                EndTime = defaultIntervals[i].End,
                BatterySlotId = bayId,
                Status = "Open",
                CreatedAt = now,
                UpdatedAt = now
            });
        }

        try
        {
            await _slots.InsertManyAsync(slotsToInsert);
            return slotsToInsert;
        }
        catch
        {
            return await _slots.Find(filter).SortBy(s => s.StartTime).ToListAsync();
        }
    }

    /// <summary>
    /// Updates the status of an energy booking slot (e.g. Open, Reserved, Closed).
    /// </summary>
    public async Task UpdateSlotStatusAsync(string slotId, string status)
    {
        var update = Builders<EnergyBookingSlot>.Update
            .Set(s => s.Status, status)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        await _slots.UpdateOneAsync(s => s.Id == slotId, update);
    }

    /// <summary>
    /// Atomically attempts to transition an Open slot to Reserved.
    /// </summary>
    public async Task<bool> TryReserveSlotAsync(string slotId)
    {
        var filter = Builders<EnergyBookingSlot>.Filter.And(
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.Id, slotId),
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.Status, "Open")
        );
        var update = Builders<EnergyBookingSlot>.Update
            .Set(s => s.Status, "Reserved")
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        var result = await _slots.FindOneAndUpdateAsync(filter, update);
        return result != null;
    }

    /// <summary>
    /// Seeds sample slots for development and demonstration across 10 days.
    /// </summary>
    public async Task SeedSlotsAsync(string stationId)
    {
        var today = DateTime.UtcNow.Date;
        var slots = new List<EnergyBookingSlot>();

        for (int i = 0; i <= 10; i++)
        {
            var date = today.AddDays(i);

            slots.Add(new EnergyBookingSlot
            {
                StationId = stationId,
                SlotDate = date,
                StartTime = new TimeSpan(8, 0, 0),
                EndTime = new TimeSpan(9, 0, 0),
                BatterySlotId = "Bay-1",
                Status = "Open",
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });

            slots.Add(new EnergyBookingSlot
            {
                StationId = stationId,
                SlotDate = date,
                StartTime = new TimeSpan(10, 0, 0),
                EndTime = new TimeSpan(11, 0, 0),
                BatterySlotId = "Bay-2",
                Status = "Open",
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });

            slots.Add(new EnergyBookingSlot
            {
                StationId = stationId,
                SlotDate = date,
                StartTime = new TimeSpan(14, 0, 0),
                EndTime = new TimeSpan(15, 0, 0),
                BatterySlotId = "Bay-3",
                Status = "Open",
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }

        await _slots.DeleteManyAsync(Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId));
        await _slots.InsertManyAsync(slots);
    }

    /// <summary>
    /// Retrieves a solar station record by its unique identifier.
    /// </summary>
    public async Task<SolarStationInfo?> GetStationByIdAsync(string stationId)
    {
        if (string.IsNullOrWhiteSpace(stationId) || _solarStations == null) return null;
        var station = await _solarStations.Find(s => s.Id == stationId).FirstOrDefaultAsync();
        if (station != null) return station;

        // Fallback: match by station name or find first active station with assigned operator
        station = await _solarStations.Find(s => s.StationName.Contains(stationId) || s.Id == "6ab775048627a459ef095e24").FirstOrDefaultAsync();
        if (station != null) return station;

        return await _solarStations.Find(s => s.Status == "Active" && s.AssignedOperatorId != null).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Computes aggregated metrics for the operational dashboard: pending, approved future (7-day window), completed today, and active spotlight.
    /// Optionally filtered by assigned operator ID.
    /// </summary>
    public async Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync(string? operatorId = null)
    {
        var nowUtc = DateTime.UtcNow;
        var todayStartUtc = nowUtc.Date;
        var todayEndUtc = todayStartUtc.AddDays(1);
        var sevenDaysFuture = nowUtc.AddDays(7);

        var filterBuilder = Builders<EnergyReservation>.Filter;
        FilterDefinition<EnergyReservation> opFilter = filterBuilder.Empty;
        if (!string.IsNullOrWhiteSpace(operatorId))
        {
            opFilter = filterBuilder.Or(
                filterBuilder.Eq(r => r.AssignedOperatorId, operatorId),
                filterBuilder.Eq(r => r.AssignedOperatorNic, operatorId)
            );
        }

        // 1. Pending reservations count
        var pendingFilter = filterBuilder.Eq(r => r.Status, "Pending") & opFilter;
        var pendingCount = (int)await _reservations.CountDocumentsAsync(pendingFilter);

        // 2. Approved future reservations count (scheduled within 7-day operational window)
        var approvedFutureFilter = filterBuilder.And(
            filterBuilder.Eq(r => r.Status, "Approved"),
            filterBuilder.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30)),
            filterBuilder.Lte(r => r.ScheduledDateTime, sevenDaysFuture),
            opFilter
        );
        var approvedFutureCount = (int)await _reservations.CountDocumentsAsync(approvedFutureFilter);

        // 3. Completed today count
        var completedTodayFilter = filterBuilder.And(
            filterBuilder.Eq(r => r.Status, "Completed"),
            filterBuilder.Or(
                filterBuilder.And(
                    filterBuilder.Gte(r => r.FinalizedAt, todayStartUtc),
                    filterBuilder.Lt(r => r.FinalizedAt, todayEndUtc)
                ),
                filterBuilder.And(
                    filterBuilder.Gte(r => r.ScheduledDateTime, todayStartUtc),
                    filterBuilder.Lt(r => r.ScheduledDateTime, todayEndUtc)
                )
            ),
            opFilter
        );
        var completedTodayCount = (int)await _reservations.CountDocumentsAsync(completedTodayFilter);

        // 4. Nearest active spotlight reservation in Approved status
        var spotlightFilter = filterBuilder.And(
            filterBuilder.Eq(r => r.Status, "Approved"),
            filterBuilder.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30)),
            opFilter
        );
        var spotlightDoc = await _reservations.Find(spotlightFilter)
            .SortBy(r => r.ScheduledDateTime)
            .FirstOrDefaultAsync();

        ActiveSpotlightDto? spotlight = null;
        if (spotlightDoc != null)
        {
            spotlight = new ActiveSpotlightDto
            {
                ReservationId = spotlightDoc.Id,
                StationName = spotlightDoc.StationName,
                AllocatedBayId = spotlightDoc.AllocatedBayId,
                ScheduledDateTime = spotlightDoc.ScheduledDateTime,
                Status = spotlightDoc.Status,
                EstimatedKwh = spotlightDoc.EstimatedKwh
            };
        }

        return new DashboardMetricsResponseDto
        {
            PendingReservationsCount = pendingCount,
            ApprovedFutureReservationsCount = approvedFutureCount,
            CompletedTodayCount = completedTodayCount,
            ActiveSpotlight = spotlight
        };
    }

    /// <summary>
    /// Queries reservations with multi-criteria filtering by status, debounced search query, calendar date, and optional operator ID.
    /// </summary>
    public async Task<List<ReservationItemDto>> GetFilteredReservationsAsync(string? status, string? search, DateTime? date, string? operatorId = null)
    {
        var filterBuilder = Builders<EnergyReservation>.Filter;
        var filter = filterBuilder.Empty;

        // Operator Filter
        if (!string.IsNullOrWhiteSpace(operatorId))
        {
            filter &= filterBuilder.Or(
                filterBuilder.Eq(r => r.AssignedOperatorId, operatorId),
                filterBuilder.Eq(r => r.AssignedOperatorNic, operatorId)
            );
        }

        // Status Filter Chip (Pending, Approved, Completed, Cancelled)
        if (!string.IsNullOrWhiteSpace(status) && !string.Equals(status, "All", StringComparison.OrdinalIgnoreCase))
        {
            filter &= filterBuilder.Regex(r => r.Status, new BsonRegularExpression($"^{Regex.Escape(status)}$", "i"));
        }

        // Search Query (Station Name, Prosumer NIC, or Reservation ID)
        if (!string.IsNullOrWhiteSpace(search))
        {
            var searchPattern = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
            var searchFilter = filterBuilder.Or(
                filterBuilder.Regex(r => r.StationName, searchPattern),
                filterBuilder.Regex(r => r.ProsumerNic, searchPattern),
                filterBuilder.Regex(r => r.Id, searchPattern)
            );
            filter &= searchFilter;
        }

        // Calendar Date Filter
        if (date.HasValue)
        {
            var targetDayUtc = date.Value.Date;
            var nextDayUtc = targetDayUtc.AddDays(1);
            filter &= filterBuilder.And(
                filterBuilder.Gte(r => r.ScheduledDateTime, targetDayUtc),
                filterBuilder.Lt(r => r.ScheduledDateTime, nextDayUtc)
            );
        }

        var reservations = await _reservations.Find(filter)
            .SortByDescending(r => r.ScheduledDateTime)
            .ToListAsync();

        return reservations.Select(r => new ReservationItemDto
        {
            ReservationId = r.Id,
            ProsumerNic = !string.IsNullOrWhiteSpace(r.ProsumerNic) ? r.ProsumerNic : r.ProsumerId,
            StationName = !string.IsNullOrWhiteSpace(r.StationName) ? r.StationName : r.StationId,
            StationId = r.StationId,
            AssignedOperatorId = r.AssignedOperatorId,
            ScheduledDateTime = r.ScheduledDateTime,
            AllocatedBayId = r.AllocatedBayId,
            EstimatedKwh = r.EstimatedKwh,
            MeteredEnergyKwh = r.MeteredEnergyKwh,
            Status = r.Status,
            QrCode = r.QrCode
        }).ToList();
    }
}
