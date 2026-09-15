// Description: Service contract defining business workflows for QR verification handshake and energy transfer finalization.

using SmartSolarMicrogrid.Api.DTOs;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Enforces central FAT service validation rules for operator QR verification and transfer finalization.
/// </summary>
public interface IOperatorVerificationService
{
    /// <summary>
    /// Authenticates and cryptographically verifies a prosumer QR payload against database records and operational rules.
    /// </summary>
    Task<QrVerificationResponseDto> VerifyQrAsync(QrVerificationRequestDto request);

    /// <summary>
    /// Defensively validates and commits metered energy reading, completing the reservation lifecycle.
    /// </summary>
    Task<(bool IsSuccess, FinalizeTransferResponseDto? Result, string? ErrorCode, string? ErrorMessage)> FinalizeTransferAsync(
        string reservationId,
        FinalizeTransferRequestDto request,
        string operatorId);
}
