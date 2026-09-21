/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for user authentication login requests.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the payload submitted by a client attempting to authenticate.
/// Supports login via either Username or NIC.
/// </summary>
public class LoginRequestDto
{
    /// <summary>
    /// Gets or sets the login credential identifier (can be Username or National Identity Card number).
    /// </summary>
    [Required(ErrorMessage = "Username or NIC is required.")]
    public string Identifier { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the plaintext password submitted for verification.
    /// </summary>
    [Required(ErrorMessage = "Password is required.")]
    public string Password { get; set; } = string.Empty;
}
