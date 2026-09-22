/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Strongly-typed configuration options class mapping EmailSettings from appsettings.json.
 */

namespace SmartSolarMicrogrid.Api.Configuration;

/// <summary>
/// Encapsulates SMTP email delivery configuration parameters.
/// </summary>
public class EmailSettings
{
    public const string SectionName = "EmailSettings";

    /// <summary>
    /// Gets or sets the SMTP server hostname (e.g., smtp.gmail.com).
    /// </summary>
    public string SmtpHost { get; set; } = "smtp.gmail.com";

    /// <summary>
    /// Gets or sets the SMTP server port number (typically 587 for STARTTLS).
    /// </summary>
    public int SmtpPort { get; set; } = 587;

    /// <summary>
    /// Gets or sets a value indicating whether SSL/TLS encryption is enabled.
    /// </summary>
    public bool EnableSsl { get; set; } = true;

    /// <summary>
    /// Gets or sets the sender email address used for outbound messages.
    /// </summary>
    public string SenderEmail { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the sender application-specific password or authentication credential.
    /// </summary>
    public string SenderPassword { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the display name for the sender.
    /// </summary>
    public string SenderName { get; set; } = "Smart Solar Microgrid Trading System";

    /// <summary>
    /// Gets or sets the URL of the Backoffice Administrative Web Portal.
    /// </summary>
    public string PortalUrl { get; set; } = "http://localhost:5173";
}
