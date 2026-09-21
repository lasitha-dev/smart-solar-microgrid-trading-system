/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Strongly-typed configuration options for JWT bearer token issuance and validation.
 */

namespace SmartSolarMicrogrid.Api.Configuration;

/// <summary>
/// Represents configuration settings for JWT token generation and validation.
/// </summary>
public class JwtSettings
{
    /// <summary>
    /// Configuration section key within appsettings.json.
    /// </summary>
    public const string SectionName = "JwtSettings";

    /// <summary>
    /// Gets or sets the symmetric secret key used to sign JWT tokens.
    /// </summary>
    public string SecretKey { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the expected token issuer.
    /// </summary>
    public string Issuer { get; set; } = "SmartSolarMicrogridApi";

    /// <summary>
    /// Gets or sets the valid token audience.
    /// </summary>
    public string Audience { get; set; } = "SmartSolarMicrogridClients";

    /// <summary>
    /// Gets or sets the token lifetime in minutes before expiration.
    /// </summary>
    public int ExpiryMinutes { get; set; } = 1440;
}
