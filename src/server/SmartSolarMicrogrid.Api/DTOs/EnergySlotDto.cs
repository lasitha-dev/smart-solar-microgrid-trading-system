/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Data transfer object for energy booking time slots associated with a microgrid station.
 * Author: Member 2
 */

using System;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Data transfer object exposing bookable energy slot details to web and mobile clients.
/// Author: Member 2
/// </summary>
public class EnergySlotDto
{
    /// <summary>
    /// Gets or sets the unique identifier of the slot document.
    /// </summary>
    public string? Id { get; set; }

    /// <summary>
    /// Gets or sets the associated microgrid station document ID.
    /// </summary>
    public string StationId { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the target calendar date for the trading slot.
    /// </summary>
    public DateTime SlotDate { get; set; }

    /// <summary>
    /// Gets or sets the slot interval starting time.
    /// </summary>
    public TimeSpan StartTime { get; set; }

    /// <summary>
    /// Gets or sets the slot interval ending time.
    /// </summary>
    public TimeSpan EndTime { get; set; }

    /// <summary>
    /// Gets or sets the specific physical battery bay designated for this slot.
    /// </summary>
    public string BatterySlotId { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the slot reservation status (Open, Reserved, Closed).
    /// </summary>
    public string Status { get; set; } = "Open";
}
