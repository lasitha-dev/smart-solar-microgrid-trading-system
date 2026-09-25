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

    public ReservationRepository(IMongoDbContext context)
    {
        _reservations = context.EnergyReservations;
        _slots = context.EnergyBookingSlots;
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

        return await _slots.Find(filter)
            .SortBy(s => s.StartTime)
            .ToListAsync();
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
    /// Computes aggregated metrics for the operational dashboard: pending, approved future (7-day window), completed today, and active spotlight.
    /// </summary>
    public async Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync()
    {
        var nowUtc = DateTime.UtcNow;
        var todayStartUtc = nowUtc.Date;
        var todayEndUtc = todayStartUtc.AddDays(1);
        var sevenDaysFuture = nowUtc.AddDays(7);

        // 1. Pending reservations count
        var pendingFilter = Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Pending");
        var pendingCount = (int)await _reservations.CountDocumentsAsync(pendingFilter);

        // 2. Approved future reservations count (scheduled within 7-day operational window)
        var approvedFutureFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved"),
            Builders<EnergyReservation>.Filter.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30)),
            Builders<EnergyReservation>.Filter.Lte(r => r.ScheduledDateTime, sevenDaysFuture)
        );
        var approvedFutureCount = (int)await _reservations.CountDocumentsAsync(approvedFutureFilter);

        // 3. Completed today count
        var completedTodayFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Completed"),
            Builders<EnergyReservation>.Filter.Or(
                Builders<EnergyReservation>.Filter.And(
                    Builders<EnergyReservation>.Filter.Gte(r => r.FinalizedAt, todayStartUtc),
                    Builders<EnergyReservation>.Filter.Lt(r => r.FinalizedAt, todayEndUtc)
                ),
                Builders<EnergyReservation>.Filter.And(
                    Builders<EnergyReservation>.Filter.Gte(r => r.ScheduledDateTime, todayStartUtc),
                    Builders<EnergyReservation>.Filter.Lt(r => r.ScheduledDateTime, todayEndUtc)
                )
            )
        );
        var completedTodayCount = (int)await _reservations.CountDocumentsAsync(completedTodayFilter);

        // 4. Nearest active spotlight reservation in Approved status
        var spotlightFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved"),
            Builders<EnergyReservation>.Filter.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30))
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
    /// Queries reservations with multi-criteria filtering by status, debounced search query, and calendar date.
    /// </summary>
    public async Task<List<ReservationItemDto>> GetFilteredReservationsAsync(string? status, string? search, DateTime? date)
    {
        var filterBuilder = Builders<EnergyReservation>.Filter;
        var filter = filterBuilder.Empty;

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
            ScheduledDateTime = r.ScheduledDateTime,
            AllocatedBayId = r.AllocatedBayId,
            EstimatedKwh = r.EstimatedKwh,
            MeteredEnergyKwh = r.MeteredEnergyKwh,
            Status = r.Status,
            QrCode = r.QrCode
        }).ToList();
    }
}
