/*
 * Student Role: Member 3 & Member 4
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Energy Reservation Workflow & Operator Verification
 * Description: MongoDB document model representing the EnergyReservation collection.
 */

using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Description: Represents an energy slot trading reservation persisted in the central MongoDB database.
/// </summary>
[BsonIgnoreExtraElements]
public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    public string ProsumerId { get; set; } = string.Empty;

    public string ProsumerNic { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public string StationName { get; set; } = string.Empty;

    public string? AssignedOperatorId { get; set; }

    public string? AssignedOperatorNic { get; set; }

    public string BookingSlotId { get; set; } = string.Empty;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime ScheduledDateTime { get; set; }

    public string AllocatedBayId { get; set; } = string.Empty;

    public double EstimatedKwh { get; set; }

    public double? MeteredEnergyKwh { get; set; }

    /// <summary>
    /// Reservation state enum: Pending | Approved | Completed | Cancelled
    /// </summary>
    public string Status { get; set; } = "Pending";

    public string? QrCode { get; set; }

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime RequestedAt { get; set; } = DateTime.UtcNow;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime? CancelledAt { get; set; }

    public string? CancelReason { get; set; }

    public string? FinalizedBy { get; set; }

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    public DateTime? FinalizedAt { get; set; }

    public string? Notes { get; set; }
}
