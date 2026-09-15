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
    /// </summary>
    public async Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync()
    {
        return await _reservationRepository.GetDashboardMetricsAsync();
    }

    /// <summary>
    /// Queries reservations matching status chips, debounced search keyword, and optional calendar date.
    /// </summary>
    public async Task<List<ReservationItemDto>> GetFilteredReservationsAsync(ReservationFilterQueryDto query)
    {
        return await _reservationRepository.GetFilteredReservationsAsync(
            query.Status,
            query.Search,
            query.Date);
    }
}
