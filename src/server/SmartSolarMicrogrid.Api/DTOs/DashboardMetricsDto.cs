// Description: Data Transfer Objects representing operational dashboard metrics and active booking spotlight data.

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Aggregated response payload for GET /api/reservations/dashboard-metrics.
/// </summary>
public class DashboardMetricsResponseDto
{
    public int PendingReservationsCount { get; set; }

    public int ApprovedFutureReservationsCount { get; set; }

    public int CompletedTodayCount { get; set; }

    public ActiveSpotlightDto? ActiveSpotlight { get; set; }
}

/// <summary>
/// Description: Embedded DTO representing the nearest upcoming approved reservation spotlight.
/// </summary>
public class ActiveSpotlightDto
{
    public string ReservationId { get; set; } = string.Empty;

    public string StationName { get; set; } = string.Empty;

    public string AllocatedBayId { get; set; } = string.Empty;

    public DateTime ScheduledDateTime { get; set; }

    public string Status { get; set; } = string.Empty;

    public double EstimatedKwh { get; set; }
}
