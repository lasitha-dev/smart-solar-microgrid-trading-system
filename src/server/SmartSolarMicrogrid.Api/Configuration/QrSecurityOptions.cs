// Description: Configuration options defining HMAC-SHA256 secret keys, payload prefixes, and verification tolerances.

namespace SmartSolarMicrogrid.Api.Configuration;

/// <summary>
/// Description: Strong configuration binder for QR cryptographic operations and operational window tolerances.
/// </summary>
public class QrSecurityOptions
{
    public const string SectionName = "QrSecurity";

    /// <summary>
    /// Secret key used to compute and verify HMAC-SHA256 signatures on QR payloads.
    /// </summary>
    public string HmacSecret { get; set; } = "SSMTS_Enterprise_Solar_Microgrid_2026_HMAC_Secret_Key_Secure_Auth!";

    /// <summary>
    /// Standardized prefix prepended to all microgrid QR payloads.
    /// </summary>
    public string PayloadPrefix { get; set; } = "SSMTS-QR";

    /// <summary>
    /// Time window tolerance in minutes allowing for operational variance during physical verification.
    /// </summary>
    public int ToleranceMinutes { get; set; } = 30;
}
