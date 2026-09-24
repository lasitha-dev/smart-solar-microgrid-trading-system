// Description: Internal domain model holding the extracted and sanitized parameters of a parsed QR token.

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Description: Encapsulates the parsed attributes and validation status of an incoming raw QR payload.
/// </summary>
public class ParsedQrToken
{
    public bool IsValidFormat { get; set; }

    public string ReservationId { get; set; } = string.Empty;

    public string ProsumerNic { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public string ScheduledDateTimeRaw { get; set; } = string.Empty;

    public DateTime? ScheduledDateTime { get; set; }

    public string Signature { get; set; } = string.Empty;

    public string? ErrorMessage { get; set; }
}
