// Description: Repository abstraction defining data access queries and atomic mutations for EnergyReservation records.

using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories;

/// <summary>
/// Description: Repository contract for managing EnergyReservation MongoDB persistence.
/// </summary>
public interface IReservationRepository
{
    /// <summary>
    /// Retrieves a reservation by its unique identifier.
    /// </summary>
    Task<EnergyReservation?> GetByIdAsync(string id);

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
}
