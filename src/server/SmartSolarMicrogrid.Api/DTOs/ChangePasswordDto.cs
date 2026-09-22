/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: DTO encapsulating change password request with strict complexity validation.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Data Transfer Object for authenticated users changing their account password.
/// Enforces current password verification, complex new password syntax, and confirmation matching.
/// </summary>
public class ChangePasswordDto
{
    /// <summary>
    /// Gets or sets the user's existing active password.
    /// </summary>
    [Required(ErrorMessage = "Current password is required.")]
    public string CurrentPassword { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the desired new password adhering to enterprise complexity rules.
    /// </summary>
    [Required(ErrorMessage = "New password is required.")]
    [StringLength(100, MinimumLength = 6, ErrorMessage = "New password must be at least 6 characters.")]
    [RegularExpression(@"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{6,}$",
        ErrorMessage = "New password must be at least 6 characters and contain at least one uppercase letter, one lowercase letter, one number, and one special symbol.")]
    public string NewPassword { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the confirmation of the new password.
    /// </summary>
    [Required(ErrorMessage = "Please confirm your new password.")]
    [Compare(nameof(NewPassword), ErrorMessage = "New password and confirmation do not match.")]
    public string ConfirmNewPassword { get; set; } = string.Empty;
}
