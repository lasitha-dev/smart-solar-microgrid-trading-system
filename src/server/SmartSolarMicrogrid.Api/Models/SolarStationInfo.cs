/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: MongoDB document model representing the SolarStationInfo collection.
 * Author: Member 2
 */

using System;
using System.Collections.Generic;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Description: Represents geographic latitude and longitude coordinates and physical address of a microgrid station.
/// Author: Member 2
/// </summary>
public class StationLocation
{
    [BsonElement("lat")]
    public double Lat { get; set; }

    [BsonElement("lng")]
    public double Lng { get; set; }

    [BsonElement("address")]
    public string Address { get; set; } = string.Empty;
}

/// <summary>
/// Description: Represents an individual physical battery charging/discharging slot at a station.
/// Author: Member 2
/// </summary>
public class BatterySlotInfo
{
    [BsonElement("slotId")]
    public string SlotId { get; set; } = string.Empty;

    [BsonElement("isAvailable")]
    public bool IsAvailable { get; set; } = true;
}

/// <summary>
/// Description: Defines operational working hours and active operational days for a microgrid station.
/// Author: Member 2
/// </summary>
public class StationSchedule
{
    [BsonElement("openTime")]
    public string OpenTime { get; set; } = "06:00";

    [BsonElement("closeTime")]
    public string CloseTime { get; set; } = "20:00";

    [BsonElement("daysActive")]
    public List<string> DaysActive { get; set; } = new()
    {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
}

/// <summary>
/// Description: Entity model mapping to the 'SolarStationInfo' collection in MongoDB.
/// Author: Member 2
/// </summary>
[BsonIgnoreExtraElements]
public class SolarStationInfo
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("stationName")]
    public string StationName { get; set; } = string.Empty;

    [BsonElement("location")]
    public StationLocation Location { get; set; } = new();

    [BsonElement("capacityKwh")]
    public double CapacityKwh { get; set; }

    [BsonElement("batterySlots")]
    public List<BatterySlotInfo> BatterySlots { get; set; } = new();

    [BsonElement("schedule")]
    public StationSchedule Schedule { get; set; } = new();

    [BsonElement("status")]
    public string Status { get; set; } = "Active";

    [BsonRepresentation(BsonType.ObjectId)]
    [BsonElement("assignedOperatorId")]
    public string? AssignedOperatorId { get; set; }

    [BsonElement("assignedOperatorName")]
    public string? AssignedOperatorName { get; set; }

    [BsonElement("assignedOperatorNic")]
    public string? AssignedOperatorNic { get; set; }

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
