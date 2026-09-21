/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Enumeration defining account lifecycle states for user accounts.
 */

using System.Text.Json.Serialization;

namespace SmartSolarMicrogrid.Api.Models.Enums;

/// <summary>
/// Defines the lifecycle status states for all user accounts in the system.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum AccountStatus
{
    /// <summary>
    /// Active account authorized to authenticate and utilize system services.
    /// </summary>
    Active,

    /// <summary>
    /// Deactivated account prohibited from logging in or performing system transactions.
    /// </summary>
    Deactivated,

    /// <summary>
    /// Newly registered prosumer awaiting Backoffice review and activation.
    /// </summary>
    PendingActivation
}
