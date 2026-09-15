// Description: Concrete MongoDB repository implementation for querying and finalizing EnergyReservation records.

using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Data;
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
}
