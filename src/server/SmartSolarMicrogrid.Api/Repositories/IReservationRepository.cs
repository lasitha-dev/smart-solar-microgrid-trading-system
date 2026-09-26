/*
 * Student Role: Member 3 & Member 4
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Energy Reservation Workflow & Operator Dashboard
 * Description: Repository abstraction defining data access queries, aggregations, and mutations for EnergyReservation records.
 */

using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories;

/// <summary>
/// Description: Repository contract for managing EnergyReservation MongoDB persistence, dashboard metrics, and history feeds.
/// </summary>
public interface IReservationRepository
{
    /// <summary>
    /// Retrieves a reservation by its unique identifier.
    /// </summary>
    Task<EnergyReservation?> GetByIdAsync(string id);

    /// <summary>
    /// Retrieves reservations filtered optionally by prosumer ID and status.
    /// </summary>
    Task<IEnumerable<EnergyReservation>> GetAllAsync(string? prosumerId = null, string? status = null);

    /// <summary>
    /// Retrieves a reservation matching the encoded QR payload string.
    /// </summary>
    Task<EnergyReservation?> GetByQrCodeAsync(string qrCode);

    /// <summary>
    /// Atomically updates a reservation state to Completed and records metered energy and operator audit information.
    /// </summary>
    Task<bool> FinalizeTransferAsync(string id, double meteredKwh, string operatorId, string? notes, DateTime finalizedAt);

    /// <summary>
    /// Inserts a new reservation document into MongoDB.
    /// </summary>
    Task CreateAsync(EnergyReservation reservation);

    /// <summary>
    /// Updates an existing reservation document in MongoDB.
    /// </summary>
    Task UpdateAsync(EnergyReservation reservation);

    /// <summary>
    /// Deletes a reservation by its unique identifier.
    /// </summary>
    Task DeleteAsync(string id);

    /// <summary>
    /// Retrieves a booking slot by its unique identifier.
    /// </summary>
    Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId);

    /// <summary>
    /// Retrieves available energy booking slots for a given station and date.
    /// </summary>
    Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date);

    /// <summary>
    /// Updates the status of an energy booking slot (e.g. Open, Reserved, Closed).
    /// </summary>
    Task UpdateSlotStatusAsync(string slotId, string status);

    /// <summary>
    /// Atomically attempts to transition an Open slot to Reserved.
    /// </summary>
    Task<bool> TryReserveSlotAsync(string slotId);

    /// <summary>
    /// Seeds sample slots for development and demonstration.
    /// </summary>
    Task SeedSlotsAsync(string stationId);

    /// <summary>
    /// Retrieves a solar station record by its unique identifier.
    /// </summary>
    Task<SolarStationInfo?> GetStationByIdAsync(string stationId);

    /// <summary>
    /// Computes aggregated metrics for the operational dashboard: pending, approved future (7-day window), completed today, and active spotlight.
    /// Optionally scoped to a specific Grid Operator.
    /// </summary>
    Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync(string? operatorId = null);

    /// <summary>
    /// Queries reservations with multi-criteria filtering by status, debounced search query, calendar date, and optional operator ID.
    /// </summary>
    Task<List<ReservationItemDto>> GetFilteredReservationsAsync(string? status, string? search, DateTime? date, string? operatorId = null);
}
