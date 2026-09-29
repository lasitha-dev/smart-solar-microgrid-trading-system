/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Data transfer object representing the formatted response returned for station queries.
 * Author: Member 2
 */

using System;
using System.Collections.Generic;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Data transfer object representing an individual physical battery slot status.
/// Author: Member 2
/// </summary>
public class BatterySlotDto
{
    /// <summary>
    /// Gets or sets the slot identifier (e.g., 'BAY-01').
    /// </summary>
    public string SlotId { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets a value indicating whether this battery bay is currently free and available for trading reservations.
    /// </summary>
    public bool IsAvailable { get; set; } = true;
}

/// <summary>
/// Description: Data transfer object defining the full microgrid station record returned to clients.
/// Author: Member 2
/// </summary>
public class StationResponseDto
{
    /// <summary>
    /// Gets or sets the primary unique identifier of the station.
    /// </summary>
    public string Id { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the display name of the station.
    /// </summary>
    public string StationName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the geographic coordinates and street address.
    /// </summary>
    public StationLocationDto Location { get; set; } = new();

    /// <summary>
    /// Gets or sets the total energy capacity in kilowatt-hours (kWh).
    /// </summary>
    public double CapacityKwh { get; set; }

    /// <summary>
    /// Gets or sets the list of battery charging slots and their availability.
    /// </summary>
    public List<BatterySlotDto> BatterySlots { get; set; } = new();

    /// <summary>
    /// Gets or sets the total number of battery slots.
    /// </summary>
    public int TotalBatterySlots => BatterySlots?.Count ?? 0;

    /// <summary>
    /// Gets or sets the count of currently available battery slots.
    /// </summary>
    public int AvailableBatterySlots => BatterySlots?.FindAll(s => s.IsAvailable).Count ?? 0;

    /// <summary>
    /// Gets or sets operational trading hours and active days.
    /// </summary>
    public StationScheduleDto Schedule { get; set; } = new();

    /// <summary>
    /// Gets or sets current station lifecycle status (Active, Inactive, Maintenance).
    /// </summary>
    public string Status { get; set; } = "Active";

    /// <summary>
    /// Gets or sets the unique MongoDB user identifier of the assigned Grid Operator.
    /// </summary>
    public string? AssignedOperatorId { get; set; }

    /// <summary>
    /// Gets or sets the full name of the assigned Grid Operator.
    /// </summary>
    public string? AssignedOperatorName { get; set; }

    /// <summary>
    /// Gets or sets the NIC of the assigned Grid Operator.
    /// </summary>
    public string? AssignedOperatorNic { get; set; }

    /// <summary>
    /// Gets or sets the UTC creation timestamp.
    /// </summary>
    public DateTime CreatedAt { get; set; }

    /// <summary>
    /// Gets or sets the UTC last updated timestamp.
    /// </summary>
    public DateTime UpdatedAt { get; set; }
}
