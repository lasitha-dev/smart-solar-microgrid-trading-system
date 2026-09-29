/*
 * Student Role: Member 2
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Microgrid Nodes, Schedules & Maps (Member 2)
 * Description: Data transfer object for updating existing solar microgrid station details.
 * Author: Member 2
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Request payload submitted to update attributes of an existing station. Properties are nullable to permit partial updates.
/// Author: Member 2
/// </summary>
public class StationUpdateDto
{
    /// <summary>
    /// Gets or sets the updated display name of the station.
    /// </summary>
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Station name must be between 3 and 100 characters.")]
    public string? StationName { get; set; }

    /// <summary>
    /// Gets or sets updated location coordinates and physical address.
    /// </summary>
    public StationLocationDto? Location { get; set; }

    /// <summary>
    /// Gets or sets the updated energy storage capacity in kilowatt-hours (kWh).
    /// </summary>
    [Range(0.01, 1000000.0, ErrorMessage = "Capacity must be greater than 0 kWh.")]
    public double? CapacityKwh { get; set; }

    /// <summary>
    /// Gets or sets the updated count of physical battery bays.
    /// </summary>
    [Range(1, 500, ErrorMessage = "Total battery slots must be between 1 and 500.")]
    public int? TotalBatterySlots { get; set; }

    /// <summary>
    /// Gets or sets the updated operational opening/closing schedule.
    /// </summary>
    public StationScheduleDto? Schedule { get; set; }

    /// <summary>
    /// Gets or sets the operational lifecycle status of the node (Active, Inactive, Maintenance).
    /// </summary>
    public string? Status { get; set; }

    /// <summary>
    /// Gets or sets the updated unique MongoDB user identifier of the assigned Grid Operator.
    /// </summary>
    public string? AssignedOperatorId { get; set; }
}
