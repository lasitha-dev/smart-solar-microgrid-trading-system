// Description: Data Transfer Objects for QR code verification request and validation response contracts.

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Request payload for POST /api/reservations/verify-qr containing the raw scanned QR payload.
/// </summary>
public class QrVerificationRequestDto
{
    public string QrPayload { get; set; } = string.Empty;
}

/// <summary>
/// Description: Response payload for POST /api/reservations/verify-qr indicating validation success or rejection reason.
/// </summary>
public class QrVerificationResponseDto
{
    public bool Valid { get; set; }

    public string? ReservationId { get; set; }

    public string? ProsumerNic { get; set; }

    public string? StationName { get; set; }

    public string? AllocatedBayId { get; set; }

    public string? Status { get; set; }

    public string? Message { get; set; }

    public string? ErrorCode { get; set; }
}
