// Description: Data Transfer Objects for energy transfer finalization request and receipt response contracts.

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Description: Request payload for PATCH /api/reservations/{id}/finalize with metered power reading.
/// </summary>
public class FinalizeTransferRequestDto
{
    public double MeteredEnergyKwh { get; set; }

    public string? Notes { get; set; }
}

/// <summary>
/// Description: Response payload confirming completion of energy transfer transaction.
/// </summary>
public class FinalizeTransferResponseDto
{
    public bool Success { get; set; }

    public string ReservationId { get; set; } = string.Empty;

    public string Status { get; set; } = string.Empty;

    public double MeteredEnergyKwh { get; set; }

    public DateTime FinalizedAt { get; set; }

    public string FinalizedByOperator { get; set; } = string.Empty;
}
