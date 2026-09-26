// Description: Concrete service orchestrating dashboard metrics aggregation and multi-criteria reservation filtering.

using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Repositories;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Implements dashboard aggregation and historical reservation filtering queries.
/// </summary>
public class DashboardQueryService : IDashboardQueryService
{
    private readonly IReservationRepository _reservationRepository;

    public DashboardQueryService(IReservationRepository reservationRepository)
    {
        _reservationRepository = reservationRepository;
    }

    /// <summary>
    /// Computes aggregated metrics: pending count, approved future count, completed today count, and active spotlight.
    /// Optionally filtered by operator ID.
    /// </summary>
    public async Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync(string? operatorId = null)
    {
        return await _reservationRepository.GetDashboardMetricsAsync(operatorId);
    }

    /// <summary>
    /// Queries reservations matching status chips, debounced search keyword, calendar date, and optional operator ID.
    /// </summary>
    public async Task<List<ReservationItemDto>> GetFilteredReservationsAsync(ReservationFilterQueryDto query)
    {
        return await _reservationRepository.GetFilteredReservationsAsync(
            query.Status,
            query.Search,
            query.Date,
            query.OperatorId);
    }
}
