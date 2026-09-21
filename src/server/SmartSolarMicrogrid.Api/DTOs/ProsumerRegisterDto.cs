/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for Prosumer self-registration requests from mobile clients.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the registration payload submitted by a solar prosumer.
/// Upon receipt, server registers the account with initial status "PendingActivation".
/// </summary>
public class ProsumerRegisterDto
{
    /// <summary>
    /// Gets or sets the National Identity Card (NIC) number, which acts as the unique business identifier.
    /// </summary>
    [Required(ErrorMessage = "NIC is required.")]
    [StringLength(20, MinimumLength = 9, ErrorMessage = "NIC must be between 9 and 20 characters.")]
    public string Nic { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the requested unique username.
    /// </summary>
    [Required(ErrorMessage = "Username is required.")]
    [StringLength(50, MinimumLength = 3, ErrorMessage = "Username must be between 3 and 50 characters.")]
    public string Username { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the plaintext password, which will be securely hashed using BCrypt.
    /// </summary>
    [Required(ErrorMessage = "Password is required.")]
    [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long.")]
    public string Password { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the full legal name of the prosumer.
    /// </summary>
    [Required(ErrorMessage = "Full Name is required.")]
    [StringLength(100, MinimumLength = 2, ErrorMessage = "Full Name must be between 2 and 100 characters.")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact mobile telephone number.
    /// </summary>
    [Required(ErrorMessage = "Phone number is required.")]
    [Phone(ErrorMessage = "Invalid phone number format.")]
    [StringLength(20, MinimumLength = 9, ErrorMessage = "Phone number must be between 9 and 20 digits.")]
    public string Phone { get; set; } = string.Empty;
}
