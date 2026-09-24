// Description: Data Transfer Objects representing individual reservation items, feeds, history, and query parameters.

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Item payload for reservation listings returned by GET /api/reservations.
/// </summary>
public class ReservationItemDto
{
    public string ReservationId { get; set; } = string.Empty;

    public string ProsumerNic { get; set; } = string.Empty;

    public string StationName { get; set; } = string.Empty;

    public DateTime ScheduledDateTime { get; set; }

    public string AllocatedBayId { get; set; } = string.Empty;

    public double EstimatedKwh { get; set; }

    public double? MeteredEnergyKwh { get; set; }

    public string Status { get; set; } = string.Empty;

    public string? QrCode { get; set; }
}

/// <summary>
/// Description: Query parameters filter for GET /api/reservations endpoint.
/// </summary>
public class ReservationFilterQueryDto
{
    /// <summary>
    /// Filter by state: All, Pending, Approved, Completed, Cancelled
    /// </summary>
    public string? Status { get; set; }

    /// <summary>
    /// Debounced search query matching station name or prosumer NIC.
    /// </summary>
    public string? Search { get; set; }

    /// <summary>
    /// Optional ISO date string filtering current daily bookings.
    /// </summary>
    public DateTime? Date { get; set; }
}
