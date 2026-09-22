/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for Backoffice administrators updating user lifecycle status.
 */

using System.ComponentModel.DataAnnotations;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the payload for transitioning a user account lifecycle state (Active, Deactivated, PendingActivation).
/// </summary>
public class UserStatusUpdateDto
{
    /// <summary>
    /// Gets or sets the target account lifecycle status.
    /// </summary>
    [Required(ErrorMessage = "Status is required.")]
    public AccountStatus Status { get; set; }

    /// <summary>
    /// Gets or sets an optional reason or remark for the status transition (e.g., rejection remarks).
    /// </summary>
    [StringLength(500, ErrorMessage = "Reason cannot exceed 500 characters.")]
    public string? Reason { get; set; }
}
