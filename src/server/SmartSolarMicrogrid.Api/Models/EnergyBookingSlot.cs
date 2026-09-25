/*
 * Student Role: Member 2 & Member 3
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Energy Reservation Workflow
 * Description: Data model representing a bookable energy slot at a microgrid station mapped to 'EnergyBookingSlots'.
 */

using System;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Description: Data model representing a bookable time slot at a microgrid station persisted in MongoDB.
/// </summary>
[BsonIgnoreExtraElements]
public class EnergyBookingSlot
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonRepresentation(BsonType.ObjectId)]
    [BsonElement("stationId")]
    public string StationId { get; set; } = string.Empty;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    [BsonElement("slotDate")]
    public DateTime SlotDate { get; set; }

    [BsonElement("startTime")]
    public TimeSpan StartTime { get; set; }

    [BsonElement("endTime")]
    public TimeSpan EndTime { get; set; }

    [BsonElement("batterySlotId")]
    public string BatterySlotId { get; set; } = string.Empty;

    [BsonElement("status")]
    public string Status { get; set; } = "Open"; // Open, Reserved, Closed

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
