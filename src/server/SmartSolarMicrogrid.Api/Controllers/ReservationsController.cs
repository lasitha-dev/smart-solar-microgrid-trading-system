// Description: Controller handling Operator QR verification, energy transfer finalization, dashboard metrics, and reservation feeds.

using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController : ControllerBase
{
    private readonly IOperatorVerificationService _verificationService;
    private readonly IDashboardQueryService _dashboardQueryService;

    public ReservationsController(
        IOperatorVerificationService verificationService,
        IDashboardQueryService dashboardQueryService)
    {
        _verificationService = verificationService;
        _dashboardQueryService = dashboardQueryService;
    }

    /// <summary>
    /// Aggregates operational dashboard metrics including live pending count, approved future count, and active spotlight.
    /// GET /api/reservations/dashboard-metrics
    /// </summary>
    [HttpGet("dashboard-metrics")]
    [ProducesResponseType(typeof(DashboardMetricsResponseDto), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetDashboardMetrics()
    {
        var metrics = await _dashboardQueryService.GetDashboardMetricsAsync();
        return Ok(metrics);
    }

    /// <summary>
    /// Retrieves reservations supporting status filtering, case-insensitive text search, and date filtering.
    /// GET /api/reservations
    /// </summary>
    [HttpGet]
    [ProducesResponseType(typeof(List<ReservationItemDto>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetReservations([FromQuery] ReservationFilterQueryDto query)
    {
        var reservations = await _dashboardQueryService.GetFilteredReservationsAsync(query);
        return Ok(reservations);
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
        // Extract authenticated operator identifier or fallback from header / default test operator
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
}
