/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: DTO for updating authenticated user profile editable fields (FullName, Phone).
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Data Transfer Object for authenticated users updating their basic personal contact details.
/// Enforces read-only immutability on NIC, Username, Email, and Role.
/// </summary>
public class UserProfileUpdateDto
{
    /// <summary>
    /// Gets or sets the updated full legal name.
    /// </summary>
    [Required(ErrorMessage = "Full Name is required.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "Full Name must be between 2 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the updated contact telephone number.
    /// </summary>
    [Required(ErrorMessage = "Phone number is required.")]
    [RegularExpression(@"^[+]?[0-9]{9,15}$", ErrorMessage = "Invalid phone number format (9 to 15 digits).")]
    public string Phone { get; set; } = string.Empty;
}
