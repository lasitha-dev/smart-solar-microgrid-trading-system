/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for updating staff and Grid Operator user account profiles.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the payload submitted by Backoffice administrators to update staff or operator account profiles.
/// </summary>
public class UserUpdateDto
{
    /// <summary>
    /// Gets or sets the full legal name of the user.
    /// </summary>
    [Required(ErrorMessage = "Full Name is required.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "Full Name must be between 2 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the primary contact email address.
    /// </summary>
    [Required(ErrorMessage = "Email address is required.")]
    [EmailAddress(ErrorMessage = "Invalid email address format.")]
    [StringLength(100, MinimumLength = 5, ErrorMessage = "Email must be between 5 and 100 characters.")]
    public string Email { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact telephone / mobile number.
    /// </summary>
    [Required(ErrorMessage = "Phone number is required.")]
    [RegularExpression(@"^(?:0|94|\+94)?(?:7[0-9]|11|2[1-7]|3[1-8]|4[1-7]|5[1-7]|6[3-7]|81)[0-9]{7}$", 
        ErrorMessage = "Invalid Sri Lankan phone number format (e.g. 0771234567 or +94771234567).")]
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the residential or station post address.
    /// </summary>
    [Required(ErrorMessage = "Address is required.")]
    [StringLength(200, MinimumLength = 5, ErrorMessage = "Address must be between 5 and 200 characters.")]
    public string Address { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the optional updated role (restricted to Backoffice or GridOperator).
    /// </summary>
    public UserRole? Role { get; set; }
}
