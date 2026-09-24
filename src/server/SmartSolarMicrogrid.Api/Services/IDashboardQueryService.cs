// Description: Service contract defining queries for operational dashboard metrics and filtered reservation feeds.

using SmartSolarMicrogrid.Api.DTOs;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Provides aggregated metric counters and filtered reservation feeds for dashboard and history views.
/// </summary>
public interface IDashboardQueryService
{
    /// <summary>
    /// Computes aggregated metrics: pending count, approved future count, completed today count, and active spotlight.
    /// </summary>
    Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync();

    /// <summary>
    /// Queries reservations matching status chips, debounced search keyword, and optional calendar date.
    /// </summary>
    Task<List<ReservationItemDto>> GetFilteredReservationsAsync(ReservationFilterQueryDto query);
}
