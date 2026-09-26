/*
 * Student Role: Member 3 & Member 4
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Energy Reservation Workflow & Operator Dashboard
 * Description: Controller handling Prosumer reservation CRUD, Operator QR verification,
 *              energy transfer finalization, dashboard metrics, and reservation feeds.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Exceptions;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Repositories;
using SmartSolarMicrogrid.Api.Services;
using Microsoft.Extensions.DependencyInjection;

namespace SmartSolarMicrogrid.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController : ControllerBase
{
    private readonly IOperatorVerificationService _verificationService;
    private readonly IDashboardQueryService _dashboardQueryService;
    private readonly IReservationService _reservationService;
    private readonly IReservationRepository _repository;

    public ReservationsController(
        IOperatorVerificationService verificationService,
        IDashboardQueryService dashboardQueryService)
        : this(verificationService, dashboardQueryService, null!, null!)
    {
    }

    [ActivatorUtilitiesConstructor]
    public ReservationsController(
        IOperatorVerificationService verificationService,
        IDashboardQueryService dashboardQueryService,
        IReservationService reservationService,
        IReservationRepository repository)
    {
        _verificationService = verificationService;
        _dashboardQueryService = dashboardQueryService;
        _reservationService = reservationService;
        _repository = repository;
    }

    /// <summary>
    /// Aggregates operational dashboard metrics including live pending count, approved future count, and active spotlight.
    /// Optionally filtered by operatorId query parameter.
    /// GET /api/reservations/dashboard-metrics
    /// </summary>
    [HttpGet("dashboard-metrics")]
    [ProducesResponseType(typeof(DashboardMetricsResponseDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetDashboardMetrics([FromQuery] string? operatorId = null)
    {
        var metrics = await _dashboardQueryService.GetDashboardMetricsAsync(operatorId);
        return Ok(metrics);
    }

    /// <summary>
    /// Retrieves reservations supporting prosumer view (when prosumerId query param is present)
    /// or operator feeds (status filtering, case-insensitive text search, and date filtering).
    /// GET /api/reservations
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<ReservationItemDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse<IEnumerable<ReservationResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetReservations([FromQuery] ReservationFilterQueryDto query)
    {
        // If prosumerId query parameter is present (either via query model or HTTP Request query),
        // format output as standard ApiResponse<IEnumerable<ReservationResponseDto>> expected by Member 3 clients
        var prosumerId = query.ProsumerId ?? (Request?.Query != null && Request.Query.ContainsKey("prosumerId") ? Request.Query["prosumerId"].ToString() : null);
        if (prosumerId != null)
        {
            var reservations = await _reservationService.GetProsumerReservationsAsync(prosumerId, query.Status);
            var dtos = reservations.Select(MapToDto);
            return Ok(ApiResponse<IEnumerable<ReservationResponseDto>>.SuccessResponse(dtos));
        }

        var operatorItems = await _dashboardQueryService.GetFilteredReservationsAsync(query);
        return Ok(operatorItems);
    }

    /// <summary>
    /// Retrieves available energy booking slots for a specified station and calendar date.
    /// GET /api/reservations/slots
    /// </summary>
    [HttpGet("slots")]
    public async Task<IActionResult> GetAvailableSlots([FromQuery] string stationId, [FromQuery] DateTime date)
    {
        var slots = await _reservationService.GetAvailableSlotsAsync(stationId, date);
        var dtos = slots.Select(s => new AvailableSlotDto
        {
            Id = s.Id ?? string.Empty,
            StationId = s.StationId,
            SlotDate = s.SlotDate,
            StartTime = s.StartTime,
            EndTime = s.EndTime,
            BatterySlotId = s.BatterySlotId,
            Status = s.Status
        });
        return Ok(ApiResponse<IEnumerable<AvailableSlotDto>>.SuccessResponse(dtos));
    }

    /// <summary>
    /// Creates a new energy reservation subject to 7-day rule and slot availability checks.
    /// POST /api/reservations
    /// </summary>
    [HttpPost]
    public async Task<IActionResult> CreateReservation([FromBody] CreateReservationDto dto)
    {
        try
        {
            var reservation = await _reservationService.CreateReservationAsync(dto);
            return CreatedAtAction(nameof(GetReservation), new { id = reservation.Id }, 
                ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation), "Reservation created successfully"));
        }
        catch (BusinessRuleException ex)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
        }
        catch (NotFoundException ex)
        {
            return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
        }
    }

    /// <summary>
    /// Retrieves a single reservation by ID.
    /// GET /api/reservations/{id}
    /// </summary>
    [HttpGet("{id}")]
    public async Task<IActionResult> GetReservation(string id)
    {
        var reservation = await _reservationService.GetReservationByIdAsync(id);
        if (reservation == null)
        {
            return NotFound(ApiResponse<object>.ErrorResponse("Reservation not found", "NOT_FOUND"));
        }

        return Ok(ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation)));
    }

    /// <summary>
    /// Updates a scheduled reservation date/slot subject to the 12-hour modification rule.
    /// PUT /api/reservations/{id}
    /// </summary>
    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateReservation(string id, [FromBody] UpdateReservationDto dto)
    {
        try
        {
            var reservation = await _reservationService.UpdateReservationAsync(id, dto);
            return Ok(ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation), "Reservation updated successfully"));
        }
        catch (BusinessRuleException ex)
        {
            return Conflict(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
        }
        catch (NotFoundException ex)
        {
            return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
        }
    }

    /// <summary>
    /// Cancels a scheduled reservation subject to the 12-hour notice rule.
    /// DELETE /api/reservations/{id}
    /// </summary>
    [HttpDelete("{id}")]
    public async Task<IActionResult> CancelReservation(string id, [FromQuery] string? reason)
    {
        try
        {
            await _reservationService.CancelReservationAsync(id, reason);
            return Ok(ApiResponse<object>.SuccessResponse(new {}, "Reservation cancelled successfully"));
        }
        catch (BusinessRuleException ex)
        {
            return Conflict(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
        }
        catch (NotFoundException ex)
        {
            return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
        }
    }

    /// <summary>
    /// Approves a pending reservation and generates the cryptographic QR token.
    /// PATCH /api/reservations/{id}/approve
    /// </summary>
    [HttpPatch("{id}/approve")]
    [ProducesResponseType(typeof(ReservationItemDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> ApproveReservation(string id, [FromQuery] string? operatorId = null)
    {
        try
        {
            var reservation = await _reservationService.ApproveReservationAsync(id, operatorId ?? string.Empty);
            var itemDto = new ReservationItemDto
            {
                ReservationId = reservation.Id ?? id,
                ProsumerNic = !string.IsNullOrWhiteSpace(reservation.ProsumerNic) ? reservation.ProsumerNic : reservation.ProsumerId,
                StationName = reservation.StationName,
                StationId = reservation.StationId,
                AssignedOperatorId = reservation.AssignedOperatorId,
                ScheduledDateTime = reservation.ScheduledDateTime,
                AllocatedBayId = reservation.AllocatedBayId,
                EstimatedKwh = reservation.EstimatedKwh,
                MeteredEnergyKwh = reservation.MeteredEnergyKwh,
                Status = reservation.Status,
                QrCode = reservation.QrCode
            };
            return Ok(itemDto);
        }
        catch (BusinessRuleException ex)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
        }
        catch (NotFoundException ex)
        {
            return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
        }
    }

    /// <summary>
    /// Rejects a reservation, releasing the booking slot and station bay availability.
    /// PATCH /api/reservations/{id}/reject
    /// </summary>
    [HttpPatch("{id}/reject")]
    [ProducesResponseType(typeof(ReservationItemDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> RejectReservation(string id, [FromQuery] string? reason = null, [FromQuery] string? operatorId = null)
    {
        try
        {
            var effectiveOperatorId = operatorId;
            if (string.IsNullOrWhiteSpace(effectiveOperatorId) && Request.Headers.TryGetValue("X-Operator-Id", out var headerOperatorId))
            {
                effectiveOperatorId = headerOperatorId.ToString();
            }

            var reservation = await _reservationService.RejectReservationAsync(id, reason, effectiveOperatorId);
            var itemDto = new ReservationItemDto
            {
                ReservationId = reservation.Id ?? id,
                ProsumerNic = !string.IsNullOrWhiteSpace(reservation.ProsumerNic) ? reservation.ProsumerNic : reservation.ProsumerId,
                StationName = reservation.StationName,
                StationId = reservation.StationId,
                AssignedOperatorId = reservation.AssignedOperatorId,
                ScheduledDateTime = reservation.ScheduledDateTime,
                AllocatedBayId = reservation.AllocatedBayId,
                EstimatedKwh = reservation.EstimatedKwh,
                MeteredEnergyKwh = reservation.MeteredEnergyKwh,
                Status = reservation.Status,
                QrCode = reservation.QrCode
            };
            return Ok(itemDto);
        }
        catch (BusinessRuleException ex)
        {
            return BadRequest(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
        }
        catch (NotFoundException ex)
        {
            return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
        }
    }

    /// <summary>
    /// Seeds sample slots across upcoming 10 days for testing.
    /// POST /api/reservations/seed
    /// </summary>
    [HttpPost("seed")]
    public async Task<IActionResult> SeedSlots([FromQuery] string? stationId)
    {
        var targetStationId = !string.IsNullOrWhiteSpace(stationId) ? stationId : "60d5ec49f1b2c42d8c3b4a59";
        await _repository.SeedSlotsAsync(targetStationId);
        return Ok(ApiResponse<object>.SuccessResponse(new { stationId = targetStationId, count = 33 }, "Seeded 3 slots per day for next 10 days"));
    }

    /// <summary>
    /// Cryptographically authenticates and validates a scanned prosumer QR payload.
    /// POST /api/reservations/verify-qr
    /// </summary>
    [HttpPost("verify-qr")]
    [ProducesResponseType(typeof(QrVerificationResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(QrVerificationResponseDto), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(QrVerificationResponseDto), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> VerifyQr([FromBody] QrVerificationRequestDto request)
    {
        var result = await _verificationService.VerifyQrAsync(request);

        if (!result.Valid)
        {
            if (result.ErrorCode == "ERR_RESERVATION_ALREADY_COMPLETED")
            {
                return Conflict(result);
            }

            return BadRequest(result);
        }

        return Ok(result);
    }

    /// <summary>
    /// Finalizes physical energy transfer with metered power and updates reservation status to Completed.
    /// PATCH /api/reservations/{id}/finalize
    /// </summary>
    [HttpPatch("{id}/finalize")]
    [ProducesResponseType(typeof(FinalizeTransferResponseDto), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    [ProducesResponseType(StatusCodes.Status409Conflict)]
    public async Task<IActionResult> FinalizeTransfer(string id, [FromBody] FinalizeTransferRequestDto request)
    {
        var operatorId = User.Identity?.Name;
        if (string.IsNullOrWhiteSpace(operatorId) && Request.Headers.TryGetValue("X-Operator-Id", out var headerOperatorId))
        {
            operatorId = headerOperatorId.ToString();
        }
        if (string.IsNullOrWhiteSpace(operatorId))
        {
            operatorId = "OP-PERADENIYA-01";
        }

        var (isSuccess, result, errorCode, errorMessage) = await _verificationService.FinalizeTransferAsync(id, request, operatorId);

        if (!isSuccess)
        {
            if (errorCode == "ERR_RESERVATION_NOT_FOUND")
            {
                return NotFound(new { success = false, errorCode, message = errorMessage });
            }

            if (errorCode == "ERR_RESERVATION_ALREADY_COMPLETED")
            {
                return Conflict(new { success = false, errorCode, message = errorMessage });
            }

            return BadRequest(new { success = false, errorCode, message = errorMessage });
        }

        return Ok(result);
    }

    private static ReservationResponseDto MapToDto(EnergyReservation r)
    {
        return new ReservationResponseDto
        {
            Id = r.Id ?? string.Empty,
            ProsumerId = !string.IsNullOrWhiteSpace(r.ProsumerNic) ? r.ProsumerNic : r.ProsumerId,
            StationId = !string.IsNullOrWhiteSpace(r.StationName) ? r.StationName : r.StationId,
            BookingSlotId = r.BookingSlotId,
            ScheduledDateTime = r.ScheduledDateTime,
            Status = r.Status,
            QrCode = r.QrCode,
            RequestedAt = r.RequestedAt,
            UpdatedAt = r.UpdatedAt,
            CancelledAt = r.CancelledAt,
            CancelReason = r.CancelReason,
            FinalizedBy = r.FinalizedBy,
            FinalizedAt = r.FinalizedAt
        };
    }
}
