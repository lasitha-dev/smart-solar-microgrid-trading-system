/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Production email service implementing SMTP delivery for staff onboarding and system notifications.
 */

using System.Net;
using System.Net.Mail;
using Microsoft.Extensions.Options;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Implements SMTP email delivery using System.Net.Mail with Gmail SMTP STARTTLS protocol.
/// </summary>
public class EmailService : IEmailService
{
    private readonly EmailSettings _emailSettings;
    private readonly ILogger<EmailService> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="EmailService"/> class.
    /// </summary>
    /// <param name="emailOptions">The strongly-typed email settings options.</param>
    /// <param name="logger">The logging service instance.</param>
    public EmailService(IOptions<EmailSettings> emailOptions, ILogger<EmailService> logger)
    {
        _emailSettings = emailOptions?.Value ?? throw new ArgumentNullException(nameof(emailOptions));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Sends an automated onboarding email containing login credentials to newly created staff accounts.
    /// </summary>
    /// <param name="recipientEmail">The destination email address.</param>
    /// <param name="fullName">The full legal name of the staff member.</param>
    /// <param name="username">The allocated system username.</param>
    /// <param name="initialPassword">The temporary initial plaintext password.</param>
    /// <param name="role">The assigned role (Backoffice or GridOperator).</param>
    /// <returns>A task returning true if the email was dispatched successfully; otherwise false.</returns>
    public async Task<bool> SendStaffOnboardingEmailAsync(
        string recipientEmail,
        string fullName,
        string username,
        string initialPassword,
        UserRole role)
    {
        if (string.IsNullOrWhiteSpace(recipientEmail))
        {
            _logger.LogWarning("Cannot send staff onboarding email: Recipient email is empty.");
            return false;
        }

        try
        {
            var roleTitle = role switch
            {
                UserRole.Backoffice => "Backoffice Officer",
                UserRole.GridOperator => "Grid Operator",
                UserRole.Administrator => "System Administrator",
                _ => role.ToString()
            };

            var portalUrl = string.IsNullOrWhiteSpace(_emailSettings.PortalUrl) ? "http://localhost:5173" : _emailSettings.PortalUrl;

            var subject = $"Welcome to Smart Solar Microgrid - Your {roleTitle} Account Credentials";
            var bodyHtml = BuildStaffOnboardingHtml(fullName, username, initialPassword, roleTitle, portalUrl);

            using var message = new MailMessage
            {
                From = new MailAddress(_emailSettings.SenderEmail, _emailSettings.SenderName),
                Subject = subject,
                Body = bodyHtml,
                IsBodyHtml = true
            };

            message.To.Add(recipientEmail.Trim());

            using var smtpClient = new SmtpClient(_emailSettings.SmtpHost, _emailSettings.SmtpPort)
            {
                EnableSsl = _emailSettings.EnableSsl,
                Credentials = new NetworkCredential(_emailSettings.SenderEmail, _emailSettings.SenderPassword),
                DeliveryMethod = SmtpDeliveryMethod.Network,
                UseDefaultCredentials = false,
                Timeout = 15000 // 15 seconds
            };

            await smtpClient.SendMailAsync(message);
            _logger.LogInformation("Staff onboarding email successfully sent to '{Email}' for user '{Username}'.", recipientEmail, username);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send staff onboarding email to '{Email}' for user '{Username}': {Message}", recipientEmail, username, ex.Message);
            return false;
        }
    }

    /// <summary>
    /// Builds a responsive, modern HTML email template for staff onboarding credentials.
    /// </summary>
    private static string BuildStaffOnboardingHtml(
        string fullName,
        string username,
        string password,
        string roleTitle,
        string portalUrl)
    {
        return $@"
<!DOCTYPE html>
<html lang=""en"">
<head>
  <meta charset=""UTF-8"" />
  <meta name=""viewport"" content=""width=device-width, initial-scale=1.0"" />
  <title>Smart Solar Microgrid Account</title>
  <style>
    body {{
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background-color: #0b1120;
      color: #f8fafc;
      margin: 0;
      padding: 24px;
    }}
    .container {{
      max-width: 580px;
      margin: 0 auto;
      background-color: #0f172a;
      border: 1px solid #1e293b;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
    }}
    .header {{
      background: linear-gradient(135deg, #1e293b, #0f172a);
      padding: 30px 24px;
      text-align: center;
      border-bottom: 2px solid #f59e0b;
    }}
    .header h1 {{
      margin: 0;
      font-size: 22px;
      color: #f59e0b;
      font-weight: 700;
      letter-spacing: 0.5px;
    }}
    .header p {{
      margin: 6px 0 0 0;
      font-size: 13px;
      color: #94a3b8;
    }}
    .content {{
      padding: 28px 24px;
      line-height: 1.6;
      font-size: 14px;
      color: #e2e8f0;
    }}
    .badge {{
      display: inline-block;
      padding: 4px 12px;
      background-color: rgba(139, 92, 246, 0.15);
      color: #a78bfa;
      border: 1px solid rgba(139, 92, 246, 0.3);
      border-radius: 9999px;
      font-size: 12px;
      font-weight: 600;
      margin-top: 4px;
    }}
    .cred-card {{
      background-color: #1e293b;
      border: 1px solid #334155;
      border-left: 4px solid #f59e0b;
      border-radius: 8px;
      padding: 16px 20px;
      margin: 20px 0;
    }}
    .cred-row {{
      margin: 8px 0;
      display: flex;
      justifyContent: space-between;
      border-bottom: 1px solid #334155;
      padding-bottom: 6px;
    }}
    .cred-row:last-child {{
      border-bottom: none;
      padding-bottom: 0;
    }}
    .cred-label {{
      color: #94a3b8;
      font-size: 13px;
    }}
    .cred-value {{
      color: #f8fafc;
      font-weight: 600;
      font-family: Consolas, monospace;
      font-size: 14px;
    }}
    .btn-container {{
      text-align: center;
      margin: 28px 0 16px;
    }}
    .btn {{
      display: inline-block;
      background: linear-gradient(135deg, #f59e0b, #d97706);
      color: #0b1120 !important;
      font-weight: 700;
      font-size: 14px;
      text-decoration: none;
      padding: 12px 28px;
      border-radius: 6px;
      box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35);
    }}
    .footer {{
      background-color: #0b1120;
      padding: 18px 24px;
      text-align: center;
      font-size: 12px;
      color: #64748b;
      border-top: 1px solid #1e293b;
    }}
    .security-note {{
      font-size: 12px;
      color: #94a3b8;
      background-color: rgba(245, 158, 11, 0.05);
      border: 1px solid rgba(245, 158, 11, 0.2);
      border-radius: 6px;
      padding: 10px 14px;
      margin-top: 20px;
    }}
  </style>
</head>
<body>
  <div class=""container"">
    <div class=""header"">
      <h1>⚡ SMART SOLAR MICROGRID</h1>
      <p>Decentralized Peer-to-Peer Energy Trading Network</p>
    </div>
    <div class=""content"">
      <p>Hello <strong>{WebUtility.HtmlEncode(fullName)}</strong>,</p>
      <p>Your staff administrative account has been successfully provisioned on the Smart Solar Microgrid Trading System.</p>
      
      <div>
        <span class=""badge"">Assigned Role: {WebUtility.HtmlEncode(roleTitle)}</span>
      </div>

      <div class=""cred-card"">
        <div class=""cred-row"">
          <span class=""cred-label"">Username:</span>
          <span class=""cred-value"">{WebUtility.HtmlEncode(username)}</span>
        </div>
        <div class=""cred-row"">
          <span class=""cred-label"">Temporary Password:</span>
          <span class=""cred-value"" style=""color: #38bdf8;"">{WebUtility.HtmlEncode(password)}</span>
        </div>
        <div class=""cred-row"">
          <span class=""cred-label"">Portal Access:</span>
          <span class=""cred-value"">{WebUtility.HtmlEncode(portalUrl)}</span>
        </div>
      </div>

      <div class=""btn-container"">
        <a href=""{WebUtility.HtmlEncode(portalUrl)}"" class=""btn"" target=""_blank"">Sign In to Backoffice Portal</a>
      </div>

      <div class=""security-note"">
        <strong>🔒 Security Notice:</strong> Please sign in to the portal using these credentials. For security best practices, keep your password confidential and do not share these details with unauthorized personnel.
      </div>
    </div>
    <div class=""footer"">
      <p>&copy; 2026 Smart Solar Microgrid Trading System (SSMTS) &bull; Enterprise Application Development</p>
      <p>This is an automated system email. Please do not reply directly to this message.</p>
    </div>
  </div>
</body>
</html>";
    }
}
