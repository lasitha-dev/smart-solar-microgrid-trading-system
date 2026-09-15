// Description: Concrete MongoDB repository implementation for querying, aggregating, and mutating EnergyReservation records.

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories;

/// <summary>
/// Description: Implements data persistence operations targeting the EnergyReservation MongoDB collection.
/// </summary>
public class ReservationRepository : IReservationRepository
{
    private readonly IMongoCollection<EnergyReservation> _collection;

    public ReservationRepository(IMongoDbContext context)
    {
        _collection = context.EnergyReservations;
    }

    /// <summary>
    /// Retrieves a reservation by its unique identifier.
    /// </summary>
    public async Task<EnergyReservation?> GetByIdAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return null;
        return await _collection.Find(r => r.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Retrieves a reservation matching the encoded QR payload string.
    /// </summary>
    public async Task<EnergyReservation?> GetByQrCodeAsync(string qrCode)
    {
        if (string.IsNullOrWhiteSpace(qrCode)) return null;
        return await _collection.Find(r => r.QrCode == qrCode).FirstOrDefaultAsync();
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

        var result = await _collection.UpdateOneAsync(filter, update);
        return result.ModifiedCount > 0;
    }

    /// <summary>
    /// Inserts a new reservation document into MongoDB.
    /// </summary>
    public async Task CreateAsync(EnergyReservation reservation)
    {
        await _collection.InsertOneAsync(reservation);
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
        var pendingCount = (int)await _collection.CountDocumentsAsync(pendingFilter);

        // 2. Approved future reservations count (scheduled within 7-day operational window)
        var approvedFutureFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved"),
            Builders<EnergyReservation>.Filter.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30)),
            Builders<EnergyReservation>.Filter.Lte(r => r.ScheduledDateTime, sevenDaysFuture)
        );
        var approvedFutureCount = (int)await _collection.CountDocumentsAsync(approvedFutureFilter);

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
        var completedTodayCount = (int)await _collection.CountDocumentsAsync(completedTodayFilter);

        // 4. Nearest active spotlight reservation in Approved status
        var spotlightFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved"),
            Builders<EnergyReservation>.Filter.Gte(r => r.ScheduledDateTime, nowUtc.AddMinutes(-30))
        );
        var spotlightDoc = await _collection.Find(spotlightFilter)
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

        var reservations = await _collection.Find(filter)
            .SortByDescending(r => r.ScheduledDateTime)
            .ToListAsync();

        return reservations.Select(r => new ReservationItemDto
        {
            ReservationId = r.Id,
            ProsumerNic = r.ProsumerNic,
            StationName = r.StationName,
            ScheduledDateTime = r.ScheduledDateTime,
            AllocatedBayId = r.AllocatedBayId,
            EstimatedKwh = r.EstimatedKwh,
            MeteredEnergyKwh = r.MeteredEnergyKwh,
            Status = r.Status,
            QrCode = r.QrCode
        }).ToList();
    }
}
