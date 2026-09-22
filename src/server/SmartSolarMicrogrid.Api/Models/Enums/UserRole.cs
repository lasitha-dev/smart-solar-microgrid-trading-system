/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Enumeration defining system user roles for role-based access control.
 */

using System.Text.Json.Serialization;

namespace SmartSolarMicrogrid.Api.Models.Enums;

/// <summary>
/// Defines the authorized roles within the Smart Solar Microgrid Trading System.
/// </summary>
[JsonConverter(typeof(JsonStringEnumConverter))]
public enum UserRole
{
    /// <summary>
    /// Backoffice administrator responsible for user lifecycle, node setup, and system management.
    /// </summary>
    Backoffice,

    /// <summary>
    /// System Administrator with full administrative privileges across microgrid management.
    /// </summary>
    Administrator,

    /// <summary>
    /// Field grid operator responsible for verifying bookings, QR validation, and energy transfers.
    /// </summary>
    GridOperator,

    /// <summary>
    /// Solar energy producer/consumer accessing the mobile client for reservations and trading.
    /// </summary>
    Prosumer
}
