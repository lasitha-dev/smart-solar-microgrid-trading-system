/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Data transfer object for creating a new solar microgrid station node.
 * Author: Member 2
 */

using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Geographic location payload containing GPS coordinates and physical address.
/// Author: Member 2
/// </summary>
public class StationLocationDto
{
    /// <summary>
    /// Gets or sets the geographic latitude coordinate (-90 to +90).
    /// </summary>
    [Range(-90.0, 90.0, ErrorMessage = "Latitude must be between -90 and 90 degrees.")]
    public double Lat { get; set; }

    /// <summary>
    /// Gets or sets the geographic longitude coordinate (-180 to +180).
    /// </summary>
    [Range(-180.0, 180.0, ErrorMessage = "Longitude must be between -180 and 180 degrees.")]
    public double Lng { get; set; }

    /// <summary>
    /// Gets or sets the physical street or regional address of the station.
    /// </summary>
    [StringLength(200, ErrorMessage = "Address cannot exceed 200 characters.")]
    public string Address { get; set; } = string.Empty;
}

/// <summary>
/// Description: Operational schedule payload detailing operating hours and active trading days.
/// Author: Member 2
/// </summary>
public class StationScheduleDto
{
    /// <summary>
    /// Gets or sets the daily station opening time (e.g., '06:00').
    /// </summary>
    [RegularExpression(@"^([01]?[0-9]|2[0-3]):[0-5][0-9]$", ErrorMessage = "OpenTime must be in HH:mm 24-hour format.")]
    public string OpenTime { get; set; } = "06:00";

    /// <summary>
    /// Gets or sets the daily station closing time (e.g., '20:00').
    /// </summary>
    [RegularExpression(@"^([01]?[0-9]|2[0-3]):[0-5][0-9]$", ErrorMessage = "CloseTime must be in HH:mm 24-hour format.")]
    public string CloseTime { get; set; } = "20:00";

    /// <summary>
    /// Gets or sets the active operational days of the week.
    /// </summary>
    public List<string> DaysActive { get; set; } = new()
    {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
}

/// <summary>
/// Description: Request payload submitted by Backoffice staff to register a new solar microgrid station node.
/// Author: Member 2
/// </summary>
public class StationCreateDto
{
    /// <summary>
    /// Gets or sets the unique display name of the microgrid station.
    /// </summary>
    [Required(ErrorMessage = "Station name is required.")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Station name must be between 3 and 100 characters.")]
    public string StationName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the geographic location and coordinates of the station.
    /// </summary>
    [Required(ErrorMessage = "Location coordinates are required.")]
    public StationLocationDto Location { get; set; } = new();

    /// <summary>
    /// Gets or sets the total energy capacity of the station in kilowatt-hours (kWh).
    /// </summary>
    [Range(0.01, 1000000.0, ErrorMessage = "Capacity must be greater than 0 kWh.")]
    public double CapacityKwh { get; set; }

    /// <summary>
    /// Gets or sets the initial count of physical battery slots installed at the station.
    /// </summary>
    [Range(1, 500, ErrorMessage = "Total battery slots must be between 1 and 500.")]
    public int TotalBatterySlots { get; set; } = 4;

    /// <summary>
    /// Gets or sets the operational schedule and active days.
    /// </summary>
    public StationScheduleDto Schedule { get; set; } = new();

    /// <summary>
    /// Gets or sets the unique MongoDB user identifier of the assigned Grid Operator.
    /// </summary>
    public string? AssignedOperatorId { get; set; }
}
