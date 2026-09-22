/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for self-account deletion with strict email confirmation.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Payload submitted by a user to permanently delete their own account.
/// Requires typing the matching registered email address to verify intent.
/// </summary>
public class DeleteAccountDto
{
    /// <summary>
    /// The user's registered email address entered for confirmation.
    /// </summary>
    [Required(ErrorMessage = "Confirmation email address is required.")]
    [EmailAddress(ErrorMessage = "Please enter a valid email address format.")]
    public string ConfirmEmail { get; set; } = string.Empty;
}
