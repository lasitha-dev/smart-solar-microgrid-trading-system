/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for prosumer account self-deactivation requests.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs;

/// <summary>
/// Represents the payload submitted when a prosumer requests self-deactivation of their account.
/// </summary>
public class DeactivationRequestDto
{
    /// <summary>
    /// Gets or sets the stated reason for requesting account deactivation.
    /// </summary>
    [StringLength(500, ErrorMessage = "Reason cannot exceed 500 characters.")]
    public string? Reason { get; set; }

    /// <summary>
    /// Gets or sets optional remarks or feedback submitted with the deactivation request.
    /// </summary>
    [StringLength(500, ErrorMessage = "Remarks cannot exceed 500 characters.")]
    public string? Remarks { get; set; }
}
