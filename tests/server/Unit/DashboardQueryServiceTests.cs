// Description: Unit tests validating operational dashboard metrics aggregation and multi-criteria reservation filtering.

using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.Controllers;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests.Unit;

public class DashboardQueryServiceTests
{
    private readonly FakeReservationRepository _repo;
    private readonly IDashboardQueryService _dashboardService;
    private readonly ReservationsController _controller;

    public DashboardQueryServiceTests()
    {
        _repo = new FakeReservationRepository();
        _dashboardService = new DashboardQueryService(_repo);

        // Dummy operator verification service for controller instantiation
        var dummyService = new OperatorVerificationService(
            new QrSignatureService(Microsoft.Extensions.Options.Options.Create(new SmartSolarMicrogrid.Api.Configuration.QrSecurityOptions())),
            _repo,
            Microsoft.Extensions.Options.Options.Create(new SmartSolarMicrogrid.Api.Configuration.QrSecurityOptions())
        );

        _controller = new ReservationsController(dummyService, _dashboardService);
    }

    [Fact]
    public async Task GetDashboardMetrics_AggregatesCountsAndNearestSpotlight()
    {
        // Arrange
        var now = DateTime.UtcNow;

        // 1. Pending reservations (2 items)
        await _repo.CreateAsync(new EnergyReservation { Id = "res-p1", Status = "Pending", ScheduledDateTime = now.AddDays(1) });
        await _repo.CreateAsync(new EnergyReservation { Id = "res-p2", Status = "Pending", ScheduledDateTime = now.AddDays(2) });

        // 2. Approved future reservations (2 items within 7 days, 1 beyond 7 days)
        await _repo.CreateAsync(new EnergyReservation
        {
            Id = "res-a1",
            Status = "Approved",
            StationName = "Peradeniya Agro Hub",
            AllocatedBayId = "BAY-02",
            ScheduledDateTime = now.AddHours(2),
            EstimatedKwh = 30.0
        });
        await _repo.CreateAsync(new EnergyReservation
        {
            Id = "res-a2",
            Status = "Approved",
            StationName = "Kandy Grid Station",
            AllocatedBayId = "BAY-01",
            ScheduledDateTime = now.AddDays(3),
            EstimatedKwh = 15.0
        });
        await _repo.CreateAsync(new EnergyReservation
        {
            Id = "res-a-far",
            Status = "Approved",
            StationName = "Far Station",
            ScheduledDateTime = now.AddDays(10)
        });

        // 3. Completed today (1 item)
        await _repo.CreateAsync(new EnergyReservation
        {
            Id = "res-c1",
            Status = "Completed",
            FinalizedAt = now.AddHours(-1),
            ScheduledDateTime = now.AddHours(-2)
        });

        // Act
        var response = await _controller.GetDashboardMetrics();

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(response);
        var metrics = Assert.IsType<DashboardMetricsResponseDto>(okResult.Value);

        Assert.Equal(2, metrics.PendingReservationsCount);
        Assert.Equal(2, metrics.ApprovedFutureReservationsCount);
        Assert.Equal(1, metrics.CompletedTodayCount);

        Assert.NotNull(metrics.ActiveSpotlight);
        Assert.Equal("res-a1", metrics.ActiveSpotlight.ReservationId);
        Assert.Equal("Peradeniya Agro Hub", metrics.ActiveSpotlight.StationName);
        Assert.Equal("BAY-02", metrics.ActiveSpotlight.AllocatedBayId);
    }

    [Fact]
    public async Task GetFilteredReservations_WithStatusChip_ReturnsMatchingSubset()
    {
        // Arrange
        await _repo.CreateAsync(new EnergyReservation { Id = "r-1", Status = "Pending", ProsumerNic = "NIC-01", StationName = "Station A" });
        await _repo.CreateAsync(new EnergyReservation { Id = "r-2", Status = "Approved", ProsumerNic = "NIC-02", StationName = "Station B" });
        await _repo.CreateAsync(new EnergyReservation { Id = "r-3", Status = "Completed", ProsumerNic = "NIC-03", StationName = "Station C" });
        await _repo.CreateAsync(new EnergyReservation { Id = "r-4", Status = "Cancelled", ProsumerNic = "NIC-04", StationName = "Station D" });

        // Act - Filter Approved
        var query = new ReservationFilterQueryDto { Status = "Approved" };
        var response = await _controller.GetReservations(query);

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(response);
        var items = Assert.IsType<List<ReservationItemDto>>(okResult.Value);
        Assert.Single(items);
        Assert.Equal("r-2", items[0].ReservationId);
        Assert.Equal("Approved", items[0].Status);
    }

    [Fact]
    public async Task GetFilteredReservations_WithSearchKeyword_MatchesStationAndNicCaseInsensitive()
    {
        // Arrange
        await _repo.CreateAsync(new EnergyReservation { Id = "r-10", Status = "Approved", ProsumerNic = "200011112222", StationName = "Peradeniya Agro Hub" });
        await _repo.CreateAsync(new EnergyReservation { Id = "r-20", Status = "Approved", ProsumerNic = "199588889999", StationName = "Matale Solar Hub" });

        // Act 1 - Search by station substring (case-insensitive)
        var queryStation = new ReservationFilterQueryDto { Search = "peradeniya" };
        var responseStation = await _controller.GetReservations(queryStation);
        var itemsStation = Assert.IsType<List<ReservationItemDto>>(((OkObjectResult)responseStation).Value);
        Assert.Single(itemsStation);
        Assert.Equal("r-10", itemsStation[0].ReservationId);

        // Act 2 - Search by NIC substring
        var queryNic = new ReservationFilterQueryDto { Search = "8888" };
        var responseNic = await _controller.GetReservations(queryNic);
        var itemsNic = Assert.IsType<List<ReservationItemDto>>(((OkObjectResult)responseNic).Value);
        Assert.Single(itemsNic);
        Assert.Equal("r-20", itemsNic[0].ReservationId);
    }

    [Fact]
    public async Task GetFilteredReservations_WithCalendarDate_FiltersToSpecificDay()
    {
        // Arrange
        var targetDate = new DateTime(2026, 9, 18, 0, 0, 0, DateTimeKind.Utc);
        await _repo.CreateAsync(new EnergyReservation { Id = "r-target", Status = "Approved", ScheduledDateTime = targetDate.AddHours(10) });
        await _repo.CreateAsync(new EnergyReservation { Id = "r-other-day", Status = "Approved", ScheduledDateTime = targetDate.AddDays(1).AddHours(2) });

        // Act
        var query = new ReservationFilterQueryDto { Date = targetDate };
        var response = await _controller.GetReservations(query);

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(response);
        var items = Assert.IsType<List<ReservationItemDto>>(okResult.Value);
        Assert.Single(items);
        Assert.Equal("r-target", items[0].ReservationId);
    }
}
