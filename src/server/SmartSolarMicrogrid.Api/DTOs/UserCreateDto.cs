/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for Backoffice administrators creating Backoffice or Grid Operator accounts.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the payload submitted by Backoffice administrators to create administrative or operator accounts.
/// Restricts role assignments to Backoffice or GridOperator.
/// </summary>
public class UserCreateDto
{
    /// <summary>
    /// Gets or sets the National Identity Card (NIC) number.
    /// </summary>
    [Required(ErrorMessage = "NIC is required.")]
    [StringLength(20, MinimumLength = 9, ErrorMessage = "NIC must be between 9 and 20 characters.")]
    public string Nic { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the unique username.
    /// </summary>
    [Required(ErrorMessage = "Username is required.")]
    [StringLength(50, MinimumLength = 3, ErrorMessage = "Username must be between 3 and 50 characters.")]
    public string Username { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact email address of the staff member.
    /// </summary>
    [Required(ErrorMessage = "Email address is required.")]
    [EmailAddress(ErrorMessage = "Invalid email address format.")]
    [StringLength(100, MinimumLength = 5, ErrorMessage = "Email must be between 5 and 100 characters.")]
    public string Email { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the initial password for the staff account.
    /// Requires at least 6 characters, one uppercase letter, one lowercase letter, one number, and one special symbol.
    /// </summary>
    [Required(ErrorMessage = "Password is required.")]
    [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long.")]
    [RegularExpression(@"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{6,}$", 
        ErrorMessage = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special symbol.")]
    public string Password { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the full legal name of the user.
    /// </summary>
    [Required(ErrorMessage = "Full Name is required.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "Full Name must be between 2 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact telephone / mobile number.
    /// </summary>
    [Required(ErrorMessage = "Phone number is required.")]
    [Phone(ErrorMessage = "Invalid phone number format.")]
    [StringLength(20, MinimumLength = 9, ErrorMessage = "Phone number must be between 9 and 20 digits.")]
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the physical address or operational station of the staff member.
    /// </summary>
    [StringLength(200, ErrorMessage = "Address cannot exceed 200 characters.")]
    public string? Address { get; set; }

    /// <summary>
    /// Gets or sets the geographic latitude coordinate of the staff or operational station.
    /// </summary>
    [Range(-90.0, 90.0, ErrorMessage = "Latitude must be between -90 and 90 degrees.")]
    public double? Latitude { get; set; }

    /// <summary>
    /// Gets or sets the geographic longitude coordinate of the staff or operational station.
    /// </summary>
    [Range(-180.0, 180.0, ErrorMessage = "Longitude must be between -180 and 180 degrees.")]
    public double? Longitude { get; set; }

    /// <summary>
    /// Gets or sets the administrative role to assign (must be Backoffice or GridOperator).
    /// </summary>
    [Required(ErrorMessage = "Role is required.")]
    public UserRole Role { get; set; }
}
