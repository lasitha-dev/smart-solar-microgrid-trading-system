/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for returning sanitized user account information without sensitive hash data.
 */

using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents a sanitized view of a user account for public API responses.
/// Plaintext passwords and password hashes are never exposed.
/// </summary>
public class UserResponseDto
{
    /// <summary>
    /// Gets or sets the database document identifier.
    /// </summary>
    public string Id { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the National Identity Card (NIC) number.
    /// </summary>
    public string Nic { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the unique username.
    /// </summary>
    public string Username { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the full legal name.
    /// </summary>
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the phone number.
    /// </summary>
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the assigned user role.
    /// </summary>
    public UserRole Role { get; set; }

    /// <summary>
    /// Gets or sets the account lifecycle status.
    /// </summary>
    public AccountStatus Status { get; set; }

    /// <summary>
    /// Gets or sets the UTC timestamp when the user account was created.
    /// </summary>
    public DateTime CreatedAt { get; set; }

    /// <summary>
    /// Gets or sets the UTC timestamp when the user account was last updated.
    /// </summary>
    public DateTime UpdatedAt { get; set; }
}
