/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object returned upon successful user authentication containing JWT token and session details.
 */

using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the response payload returned upon successful authentication.
/// Contains the JWT bearer token, user identifier, role, and profile information.
/// </summary>
public class LoginResponseDto
{
    /// <summary>
    /// Gets or sets the signed JWT bearer token used for authorized API requests.
    /// </summary>
    public string Token { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the token type (typically "Bearer").
    /// </summary>
    public string TokenType { get; set; } = "Bearer";

    /// <summary>
    /// Gets or sets the unique database user identifier.
    /// </summary>
    public string UserId { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the National Identity Card number of the authenticated user.
    /// </summary>
    public string Nic { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the unique username of the authenticated user.
    /// </summary>
    public string Username { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the full legal name of the authenticated user.
    /// </summary>
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact phone number of the authenticated user.
    /// </summary>
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the address of the prosumer.
    /// </summary>
    public string Address { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the latitude coordinate of the solar microgrid facility.
    /// </summary>
    public double? Latitude { get; set; }

    /// <summary>
    /// Gets or sets the longitude coordinate of the solar microgrid facility.
    /// </summary>
    public double? Longitude { get; set; }

    /// <summary>
    /// Gets or sets the assigned user role (Backoffice, GridOperator, Prosumer).
    /// </summary>
    public UserRole Role { get; set; }

    /// <summary>
    /// Gets or sets the account status (Active).
    /// </summary>
    public AccountStatus Status { get; set; }

    /// <summary>
    /// Gets or sets the UTC timestamp when the issued token expires.
    /// </summary>
    public DateTime ExpiresAt { get; set; }
}
