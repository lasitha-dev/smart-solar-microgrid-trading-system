// Description: Unit tests validating operator QR verification handshake, business rules, and energy transfer finalization.

using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Controllers;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Repositories;
using SmartSolarMicrogrid.Api.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests.Unit;

public class OperatorVerificationServiceTests
{
    private readonly IQrSignatureService _qrSignatureService;
    private readonly FakeReservationRepository _fakeRepo;
    private readonly IOperatorVerificationService _service;
    private readonly IDashboardQueryService _dashboardService;
    private readonly ReservationsController _controller;

    public OperatorVerificationServiceTests()
    {
        var options = new QrSecurityOptions
        {
            HmacSecret = "Test_Secret_Key_For_Unit_Testing_Microgrid_2026!",
            PayloadPrefix = "SSMTS-QR",
            ToleranceMinutes = 60
        };

        _qrSignatureService = new QrSignatureService(Options.Create(options));
        _fakeRepo = new FakeReservationRepository();
        _service = new OperatorVerificationService(_qrSignatureService, _fakeRepo, Options.Create(options));
        _dashboardService = new DashboardQueryService(_fakeRepo);
        _controller = new ReservationsController(_service, _dashboardService);
    }

    [Fact]
    public async Task VerifyQr_WithValidApprovedReservation_ReturnsHandshakeSuccess()
    {
        // Arrange: seed approved reservation
        var scheduledTime = DateTime.UtcNow.AddMinutes(15);
        var reservation = new EnergyReservation
        {
            Id = "664fa10b9c3e2e1a4f001201",
            ProsumerNic = "200012345678",
            StationId = "ST-002",
            StationName = "Peradeniya Agro-Voltaic Hub",
            AllocatedBayId = "BAY-02",
            ScheduledDateTime = scheduledTime,
            Status = "Approved",
            EstimatedKwh = 25.0
        };
        var validPayload = _qrSignatureService.GenerateFullPayload(
            reservation.Id, reservation.ProsumerNic, reservation.StationId, scheduledTime);
        reservation.QrCode = validPayload;
        await _fakeRepo.CreateAsync(reservation);

        // Act
        var result = await _service.VerifyQrAsync(new QrVerificationRequestDto { QrPayload = validPayload });

        // Assert
        Assert.True(result.Valid);
        Assert.Equal("664fa10b9c3e2e1a4f001201", result.ReservationId);
        Assert.Equal("200012345678", result.ProsumerNic);
        Assert.Equal("Peradeniya Agro-Voltaic Hub", result.StationName);
        Assert.Equal("BAY-02", result.AllocatedBayId);
        Assert.Equal("Approved", result.Status);
    }

    [Fact]
    public async Task VerifyQr_WithTamperedSignature_ReturnsInvalidSignatureError()
    {
        // Arrange
        var scheduledTime = DateTime.UtcNow.AddMinutes(15);
        var reservation = new EnergyReservation
        {
            Id = "res-tamper-1",
            ProsumerNic = "200012345678",
            StationId = "ST-002",
            Status = "Approved",
            ScheduledDateTime = scheduledTime
        };
        var validPayload = _qrSignatureService.GenerateFullPayload(
            reservation.Id, reservation.ProsumerNic, reservation.StationId, scheduledTime);
        await _fakeRepo.CreateAsync(reservation);

        // Tamper with payload
        var tamperedPayload = validPayload.Replace(reservation.Id, "res-altered-99");

        // Act
        var result = await _service.VerifyQrAsync(new QrVerificationRequestDto { QrPayload = tamperedPayload });

        // Assert
        Assert.False(result.Valid);
        Assert.Equal("ERR_INVALID_QR_SIGNATURE", result.ErrorCode);
    }

    [Fact]
    public async Task VerifyQr_WhenReservationAlreadyCompleted_ReturnsConflictError()
    {
        // Arrange
        var scheduledTime = DateTime.UtcNow.AddHours(-1);
        var reservation = new EnergyReservation
        {
            Id = "res-completed-1",
            ProsumerNic = "200012345678",
            StationId = "ST-002",
            StationName = "Peradeniya Hub",
            Status = "Completed",
            ScheduledDateTime = scheduledTime,
            FinalizedAt = DateTime.UtcNow.AddMinutes(-30)
        };
        var payload = _qrSignatureService.GenerateFullPayload(
            reservation.Id, reservation.ProsumerNic, reservation.StationId, scheduledTime);
        await _fakeRepo.CreateAsync(reservation);

        // Act
        var response = await _controller.VerifyQr(new QrVerificationRequestDto { QrPayload = payload });

        // Assert
        var conflictResult = Assert.IsType<ConflictObjectResult>(response);
        var body = Assert.IsType<QrVerificationResponseDto>(conflictResult.Value);
        Assert.False(body.Valid);
        Assert.Equal("ERR_RESERVATION_ALREADY_COMPLETED", body.ErrorCode);
    }

    [Fact]
    public async Task FinalizeTransfer_WithValidMeteredKwh_TransitionsToCompleted()
    {
        // Arrange
        var reservation = new EnergyReservation
        {
            Id = "res-finalize-1",
            ProsumerNic = "200012345678",
            StationId = "ST-002",
            Status = "Approved",
            EstimatedKwh = 25.0
        };
        await _fakeRepo.CreateAsync(reservation);

        var request = new FinalizeTransferRequestDto
        {
            MeteredEnergyKwh = 24.65,
            Notes = "Transfer executed without voltage anomalies."
        };

        // Act
        var (isSuccess, result, _, _) = await _service.FinalizeTransferAsync(reservation.Id, request, "OP-PERADENIYA-01");

        // Assert
        Assert.True(isSuccess);
        Assert.NotNull(result);
        Assert.Equal("Completed", result.Status);
        Assert.Equal(24.65, result.MeteredEnergyKwh);
        Assert.Equal("OP-PERADENIYA-01", result.FinalizedByOperator);

        var updated = await _fakeRepo.GetByIdAsync(reservation.Id);
        Assert.NotNull(updated);
        Assert.Equal("Completed", updated.Status);
        Assert.Equal(24.65, updated.MeteredEnergyKwh);
    }

    [Theory]
    [InlineData(0.0)]
    [InlineData(-5.5)]
    [InlineData(1000.0)]
    public async Task FinalizeTransfer_WithInvalidMeteredEnergy_FailsDefensiveValidation(double invalidKwh)
    {
        // Arrange
        var reservation = new EnergyReservation
        {
            Id = "res-invalid-kwh",
            Status = "Approved"
        };
        await _fakeRepo.CreateAsync(reservation);

        var request = new FinalizeTransferRequestDto
        {
            MeteredEnergyKwh = invalidKwh
        };

        // Act
        var (isSuccess, _, errorCode, _) = await _service.FinalizeTransferAsync(reservation.Id, request, "OP-01");

        // Assert
        Assert.False(isSuccess);
        Assert.Equal("ERR_INVALID_METERED_KWH", errorCode);
    }
}

/// <summary>
/// In-memory fake repository implementation for fast unit test verification.
/// </summary>
public class FakeReservationRepository : IReservationRepository
{
    private readonly Dictionary<string, EnergyReservation> _store = new();

    public Task<EnergyReservation?> GetByIdAsync(string id)
    {
        _store.TryGetValue(id, out var reservation);
        return Task.FromResult(reservation);
    }

    public Task<EnergyReservation?> GetByQrCodeAsync(string qrCode)
    {
        var reservation = _store.Values.FirstOrDefault(r => r.QrCode == qrCode);
        return Task.FromResult(reservation);
    }

    public Task<bool> FinalizeTransferAsync(string id, double meteredKwh, string operatorId, string? notes, DateTime finalizedAt)
    {
        if (!_store.TryGetValue(id, out var reservation)) return Task.FromResult(false);
        if (reservation.Status != "Approved") return Task.FromResult(false);

        reservation.Status = "Completed";
        reservation.MeteredEnergyKwh = meteredKwh;
        reservation.FinalizedBy = operatorId;
        reservation.FinalizedAt = finalizedAt;
        reservation.Notes = notes;
        reservation.UpdatedAt = finalizedAt;

        return Task.FromResult(true);
    }

    public Task CreateAsync(EnergyReservation reservation)
    {
        _store[reservation.Id] = reservation;
        return Task.CompletedTask;
    }

    public Task<IEnumerable<EnergyReservation>> GetAllAsync(string? prosumerId = null, string? status = null)
    {
        var query = _store.Values.AsEnumerable();
        if (!string.IsNullOrWhiteSpace(prosumerId) && !prosumerId.Equals("all", StringComparison.OrdinalIgnoreCase))
        {
            query = query.Where(r => r.ProsumerId == prosumerId || r.ProsumerNic == prosumerId);
        }
        if (!string.IsNullOrWhiteSpace(status) && !status.Equals("all", StringComparison.OrdinalIgnoreCase))
        {
            query = query.Where(r => r.Status.Equals(status, StringComparison.OrdinalIgnoreCase));
        }
        return Task.FromResult<IEnumerable<EnergyReservation>>(query.ToList());
    }

    public Task UpdateAsync(EnergyReservation reservation)
    {
        _store[reservation.Id] = reservation;
        return Task.CompletedTask;
    }

    public Task DeleteAsync(string id)
    {
        _store.Remove(id);
        return Task.CompletedTask;
    }

    private readonly Dictionary<string, EnergyBookingSlot> _slots = new();

    public Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId)
    {
        _slots.TryGetValue(slotId, out var slot);
        return Task.FromResult(slot);
    }

    public Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date)
    {
        var day = date.Date;
        var slots = _slots.Values.Where(s => s.StationId == stationId && s.SlotDate.Date == day);
        return Task.FromResult<IEnumerable<EnergyBookingSlot>>(slots.ToList());
    }

    public Task UpdateSlotStatusAsync(string slotId, string status)
    {
        if (_slots.TryGetValue(slotId, out var slot))
        {
            slot.Status = status;
        }
        return Task.CompletedTask;
    }

    public Task<bool> TryReserveSlotAsync(string slotId)
    {
        if (_slots.TryGetValue(slotId, out var slot) && slot.Status == "Open")
        {
            slot.Status = "Reserved";
            return Task.FromResult(true);
        }
        return Task.FromResult(false);
    }

    public Task SeedSlotsAsync(string stationId)
    {
        return Task.CompletedTask;
    }

    public Task<SolarStationInfo?> GetStationByIdAsync(string stationId)
    {
        return Task.FromResult<SolarStationInfo?>(new SolarStationInfo
        {
            Id = stationId,
            StationName = "Kandy Solar Hub",
            AssignedOperatorId = "op-1",
            AssignedOperatorName = "Operator Silva",
            AssignedOperatorNic = "901234567V"
        });
    }

    public Task<DashboardMetricsResponseDto> GetDashboardMetricsAsync(string? operatorId = null)
    {
        var nowUtc = DateTime.UtcNow;
        var todayStartUtc = nowUtc.Date;
        var todayEndUtc = todayStartUtc.AddDays(1);
        var sevenDaysFuture = nowUtc.AddDays(7);

        var query = _store.Values.AsEnumerable();
        if (!string.IsNullOrWhiteSpace(operatorId))
        {
            query = query.Where(r => r.AssignedOperatorId == operatorId);
        }

        var pendingCount = query.Count(r => r.Status.Equals("Pending", StringComparison.OrdinalIgnoreCase));
        var approvedFutureCount = query.Count(r =>
            r.Status.Equals("Approved", StringComparison.OrdinalIgnoreCase) &&
            r.ScheduledDateTime >= nowUtc.AddMinutes(-30) &&
            r.ScheduledDateTime <= sevenDaysFuture);

        var completedTodayCount = query.Count(r =>
            r.Status.Equals("Completed", StringComparison.OrdinalIgnoreCase) &&
            ((r.FinalizedAt >= todayStartUtc && r.FinalizedAt < todayEndUtc) ||
             (r.ScheduledDateTime >= todayStartUtc && r.ScheduledDateTime < todayEndUtc)));

        var spotlightDoc = query
            .Where(r => r.Status.Equals("Approved", StringComparison.OrdinalIgnoreCase) &&
                        r.ScheduledDateTime >= nowUtc.AddMinutes(-30))
            .OrderBy(r => r.ScheduledDateTime)
            .FirstOrDefault();

        ActiveSpotlightDto? spotlight = null;
        if (spotlightDoc != null)
        {
            spotlight = new ActiveSpotlightDto
            {
                ReservationId = spotlightDoc.Id,
                StationName = spotlightDoc.StationName,
                AllocatedBayId = spotlightDoc.AllocatedBayId,
                ScheduledDateTime = spotlightDoc.ScheduledDateTime,
                Status = spotlightDoc.Status,
                EstimatedKwh = spotlightDoc.EstimatedKwh
            };
        }

        return Task.FromResult(new DashboardMetricsResponseDto
        {
            PendingReservationsCount = pendingCount,
            ApprovedFutureReservationsCount = approvedFutureCount,
            CompletedTodayCount = completedTodayCount,
            ActiveSpotlight = spotlight
        });
    }

    public Task<List<ReservationItemDto>> GetFilteredReservationsAsync(string? status, string? search, DateTime? date, string? operatorId = null)
    {
        var query = _store.Values.AsEnumerable();

        if (!string.IsNullOrWhiteSpace(operatorId))
        {
            query = query.Where(r => r.AssignedOperatorId == operatorId);
        }

        if (!string.IsNullOrWhiteSpace(status) && !status.Equals("All", StringComparison.OrdinalIgnoreCase))
        {
            query = query.Where(r => r.Status.Equals(status, StringComparison.OrdinalIgnoreCase));
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim();
            query = query.Where(r =>
                r.StationName.Contains(term, StringComparison.OrdinalIgnoreCase) ||
                r.ProsumerNic.Contains(term, StringComparison.OrdinalIgnoreCase) ||
                r.Id.Contains(term, StringComparison.OrdinalIgnoreCase));
        }

        if (date.HasValue)
        {
            var dayStart = date.Value.Date;
            var dayEnd = dayStart.AddDays(1);
            query = query.Where(r => r.ScheduledDateTime >= dayStart && r.ScheduledDateTime < dayEnd);
        }

        var results = query
            .OrderByDescending(r => r.ScheduledDateTime)
            .Select(r => new ReservationItemDto
            {
                ReservationId = r.Id,
                ProsumerNic = r.ProsumerNic,
                StationName = r.StationName,
                StationId = r.StationId,
                AssignedOperatorId = r.AssignedOperatorId,
                ScheduledDateTime = r.ScheduledDateTime,
                AllocatedBayId = r.AllocatedBayId,
                EstimatedKwh = r.EstimatedKwh,
                MeteredEnergyKwh = r.MeteredEnergyKwh,
                Status = r.Status,
                QrCode = r.QrCode
            }).ToList();

        return Task.FromResult(results);
    }
}
