// Description: MongoDB document model representing the EnergyReservation collection.

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Description: Represents an energy slot trading reservation persisted in the central MongoDB database.
/// </summary>
public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    [BsonRepresentation(BsonType.ObjectId)]
    public string ProsumerId { get; set; } = string.Empty;

    public string ProsumerNic { get; set; } = string.Empty;

    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    public string StationName { get; set; } = string.Empty;

    [BsonRepresentation(BsonType.ObjectId)]
    public string BookingSlotId { get; set; } = string.Empty;

    public DateTime ScheduledDateTime { get; set; }

    public string AllocatedBayId { get; set; } = string.Empty;

    public double EstimatedKwh { get; set; }

    public double? MeteredEnergyKwh { get; set; }

    /// <summary>
    /// Reservation state enum: Pending | Approved | Completed | Cancelled
    /// </summary>
    public string Status { get; set; } = "Pending";

    public string? QrCode { get; set; }

    public DateTime RequestedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public string? FinalizedBy { get; set; }

    public DateTime? FinalizedAt { get; set; }

    public string? Notes { get; set; }
}
