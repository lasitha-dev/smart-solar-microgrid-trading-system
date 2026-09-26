// Description: Concrete service implementing FAT service business rules, cryptographic checks, and status transitions.

using Microsoft.Extensions.Options;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Repositories;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Implements verification and finalization logic enforcing all state and cryptographic constraints.
/// </summary>
public class OperatorVerificationService : IOperatorVerificationService
{
    private readonly IQrSignatureService _qrSignatureService;
    private readonly IReservationRepository _reservationRepository;
    private readonly QrSecurityOptions _securityOptions;

    public OperatorVerificationService(
        IQrSignatureService qrSignatureService,
        IReservationRepository reservationRepository,
        IOptions<QrSecurityOptions> securityOptions)
    {
        _qrSignatureService = qrSignatureService;
        _reservationRepository = reservationRepository;
        _securityOptions = securityOptions.Value;
    }

    /// <summary>
    /// Authenticates and cryptographically verifies a prosumer QR payload against database records and operational rules.
    /// </summary>
    public async Task<QrVerificationResponseDto> VerifyQrAsync(QrVerificationRequestDto request)
    {
        if (string.IsNullOrWhiteSpace(request.QrPayload))
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ErrorCode = "ERR_MALFORMED_QR",
                Message = "QR payload cannot be empty."
            };
        }

        // 1. Parse token syntax
        var parsed = _qrSignatureService.ParsePayload(request.QrPayload);
        if (!parsed.IsValidFormat)
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ErrorCode = "ERR_MALFORMED_QR",
                Message = parsed.ErrorMessage ?? "Malformed QR payload structure."
            };
        }

        // 2. Cryptographic signature check
        var isSignatureValid = _qrSignatureService.VerifySignature(request.QrPayload);
        if (!isSignatureValid)
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = parsed.ReservationId,
                ErrorCode = "ERR_INVALID_QR_SIGNATURE",
                Message = "Invalid cryptographic signature. QR token may have been altered or forged."
            };
        }

        // 3. Database existence check
        var reservation = await _reservationRepository.GetByIdAsync(parsed.ReservationId);
        if (reservation == null)
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = parsed.ReservationId,
                ErrorCode = "ERR_RESERVATION_NOT_FOUND",
                Message = $"Reservation '{parsed.ReservationId}' was not found in the central database."
            };
        }

        // 4. State validation rules
        if (string.Equals(reservation.Status, "Completed", StringComparison.OrdinalIgnoreCase))
        {
            var finalizedTimeStr = reservation.FinalizedAt?.ToString("yyyy-MM-ddTHH:mm:ssZ") ?? "previously";
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = reservation.Id,
                ProsumerNic = reservation.ProsumerNic,
                StationName = reservation.StationName,
                AllocatedBayId = reservation.AllocatedBayId,
                Status = reservation.Status,
                ErrorCode = "ERR_RESERVATION_ALREADY_COMPLETED",
                Message = $"This reservation was already finalized on {finalizedTimeStr}."
            };
        }

        if (string.Equals(reservation.Status, "Cancelled", StringComparison.OrdinalIgnoreCase))
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = reservation.Id,
                Status = reservation.Status,
                ErrorCode = "ERR_RESERVATION_CANCELLED",
                Message = "This reservation was cancelled and cannot undergo energy transfer."
            };
        }

        if (!string.Equals(reservation.Status, "Approved", StringComparison.OrdinalIgnoreCase))
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = reservation.Id,
                Status = reservation.Status,
                ErrorCode = "ERR_NOT_APPROVED",
                Message = $"Reservation status is '{reservation.Status}'. Only Approved reservations can be verified."
            };
        }

        // 5. Operational window validation (tolerance window check)
        var scheduledUtc = reservation.ScheduledDateTime.ToUniversalTime();
        var nowUtc = DateTime.UtcNow;
        var tolerance = TimeSpan.FromMinutes(_securityOptions.ToleranceMinutes);

        // Disallow transfers if reservation is more than 24 hours in the future
        if (scheduledUtc - nowUtc > TimeSpan.FromHours(24))
        {
            return new QrVerificationResponseDto
            {
                Valid = false,
                ReservationId = reservation.Id,
                Status = reservation.Status,
                ErrorCode = "ERR_OUTSIDE_WINDOW",
                Message = $"Reservation is scheduled for {scheduledUtc:yyyy-MM-dd HH:mm UTC}. Current time is too early for verification."
            };
        }

        // 6. Verification successful: Return handshake payload
        return new QrVerificationResponseDto
        {
            Valid = true,
            ReservationId = reservation.Id,
            ProsumerNic = reservation.ProsumerNic,
            StationName = reservation.StationName,
            AllocatedBayId = reservation.AllocatedBayId,
            Status = reservation.Status,
            Message = "QR token valid. Proceed to physical energy transfer."
        };
    }

    /// <summary>
    /// Defensively validates and commits metered energy reading, completing the reservation lifecycle.
    /// </summary>
    public async Task<(bool IsSuccess, FinalizeTransferResponseDto? Result, string? ErrorCode, string? ErrorMessage)> FinalizeTransferAsync(
        string reservationId,
        FinalizeTransferRequestDto request,
        string operatorId)
    {
        // 1. Defensive input validation (Rule 4: strictly 0.01 to 999.99 kWh)
        if (request.MeteredEnergyKwh < 0.01 || request.MeteredEnergyKwh > 999.99 ||
            double.IsNaN(request.MeteredEnergyKwh) || double.IsInfinity(request.MeteredEnergyKwh))
        {
            return (false, null, "ERR_INVALID_METERED_KWH",
                "Actual metered energy must be a valid numeric value between 0.01 and 999.99 kWh.");
        }

        // 2. Existence check
        var reservation = await _reservationRepository.GetByIdAsync(reservationId);
        if (reservation == null)
        {
            return (false, null, "ERR_RESERVATION_NOT_FOUND",
                $"Reservation '{reservationId}' was not found.");
        }

        // 3. Status eligibility check
        if (string.Equals(reservation.Status, "Completed", StringComparison.OrdinalIgnoreCase))
        {
            return (false, null, "ERR_RESERVATION_ALREADY_COMPLETED",
                "This reservation has already been finalized.");
        }

        if (!string.Equals(reservation.Status, "Approved", StringComparison.OrdinalIgnoreCase))
        {
            return (false, null, "ERR_NOT_APPROVED",
                $"Cannot finalize reservation with status '{reservation.Status}'. Must be in Approved state.");
        }

        // 4. Commit status transition
        var now = DateTime.UtcNow;
        var committed = await _reservationRepository.FinalizeTransferAsync(
            reservationId,
            request.MeteredEnergyKwh,
            operatorId,
            request.Notes,
            now);

        if (!committed)
        {
            return (false, null, "ERR_COMMIT_FAILED",
                "Failed to commit transaction state change to the database.");
        }

        // Release the booked energy slot back to Open so it can be booked again
        if (!string.IsNullOrWhiteSpace(reservation.BookingSlotId))
        {
            await _reservationRepository.UpdateSlotStatusAsync(reservation.BookingSlotId, "Open");
        }

        // Release station battery bay back to available
        if (!string.IsNullOrWhiteSpace(reservation.StationId) && !string.IsNullOrWhiteSpace(reservation.AllocatedBayId))
        {
            await _reservationRepository.UpdateStationBayAvailabilityAsync(reservation.StationId, reservation.AllocatedBayId, true);
        }

        var response = new FinalizeTransferResponseDto
        {
            Success = true,
            ReservationId = reservation.Id,
            Status = "Completed",
            MeteredEnergyKwh = request.MeteredEnergyKwh,
            FinalizedAt = now,
            FinalizedByOperator = operatorId
        };

        return (true, response, null, null);
    }
}
