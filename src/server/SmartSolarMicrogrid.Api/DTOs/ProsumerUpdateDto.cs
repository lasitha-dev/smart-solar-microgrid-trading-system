/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for updating permitted profile fields by solar prosumers.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the update payload submitted by a solar prosumer to modify permitted profile fields.
/// Restricted to non-key attributes (FullName, Phone).
/// </summary>
public class ProsumerUpdateDto
{
    /// <summary>
    /// Gets or sets the updated full legal name.
    /// </summary>
    [Required(ErrorMessage = "Full Name is required.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "Full Name must be between 2 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the updated contact phone number.
    /// </summary>
    [Required(ErrorMessage = "Phone number is required.")]
    [Phone(ErrorMessage = "Invalid phone number format.")]
    [StringLength(20, MinimumLength = 9, ErrorMessage = "Phone number must be between 9 and 20 digits.")]
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the updated residential/street address.
    /// </summary>
    [StringLength(200, ErrorMessage = "Address cannot exceed 200 characters.")]
    public string Address { get; set; } = string.Empty;
}
