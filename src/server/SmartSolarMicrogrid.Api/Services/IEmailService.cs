/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Interface definition for automated email notification service.
 */

using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Contract for sending automated transactional and onboarding emails.
/// </summary>
public interface IEmailService
{
    /// <summary>
    /// Sends an automated onboarding email containing login credentials to newly created staff accounts.
    /// </summary>
    /// <param name="recipientEmail">The destination email address.</param>
    /// <param name="fullName">The full legal name of the staff member.</param>
    /// <param name="username">The allocated system username.</param>
    /// <param name="initialPassword">The temporary initial plaintext password.</param>
    /// <param name="role">The assigned role (Backoffice or GridOperator).</param>
    /// <returns>A task returning true if the email was dispatched successfully; otherwise false.</returns>
    Task<bool> SendStaffOnboardingEmailAsync(
        string recipientEmail,
        string fullName,
        string username,
        string initialPassword,
        UserRole role);
}
