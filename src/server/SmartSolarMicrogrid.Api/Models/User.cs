/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: MongoDB entity model representing user accounts mapped to collection "User's Detail".
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.Models;

/// <summary>
/// Represents a user entity stored in the MongoDB "User's Detail" collection.
/// Encompasses Backoffice administrators, Grid Operators, and Prosumers.
/// </summary>
[BsonIgnoreExtraElements]
public class User
{
    /// <summary>
    /// Gets or sets the primary document identifier in MongoDB.
    /// </summary>
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    /// <summary>
    /// Gets or sets the National Identity Card (NIC) number, serving as the unique business identifier.
    /// </summary>
    [BsonElement("nic")]
    public string Nic { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the unique login username.
    /// </summary>
    [BsonElement("username")]
    public string Username { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the primary contact email address. Must be unique across all system users.
    /// </summary>
    [BsonElement("email")]
    public string Email { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the one-way BCrypt hashed password string. Plaintext passwords must never be stored.
    /// </summary>
    [BsonElement("passwordHash")]
    public string PasswordHash { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the full legal name of the user.
    /// </summary>
    [BsonElement("fullName")]
    public string FullName { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the contact telephone / mobile number.
    /// </summary>
    [BsonElement("phone")]
    public string Phone { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the street/residential address of the prosumer or facility.
    /// </summary>
    [BsonElement("address")]
    public string Address { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the latitude coordinate of the solar facility. Strictly immutable once confirmed during registration.
    /// </summary>
    [BsonElement("latitude")]
    public double? Latitude { get; set; }

    /// <summary>
    /// Gets or sets the longitude coordinate of the solar facility. Strictly immutable once confirmed during registration.
    /// </summary>
    [BsonElement("longitude")]
    public double? Longitude { get; set; }

    /// <summary>
    /// Gets or sets the assigned user role for system access control.
    /// </summary>
    [BsonElement("role")]
    [BsonRepresentation(BsonType.String)]
    public UserRole Role { get; set; } = UserRole.Prosumer;

    /// <summary>
    /// Gets or sets the lifecycle status of the account (Active, Deactivated, or PendingActivation).
    /// </summary>
    [BsonElement("status")]
    [BsonRepresentation(BsonType.String)]
    public AccountStatus Status { get; set; } = AccountStatus.PendingActivation;

    /// <summary>
    /// Gets or sets the UTC timestamp when the user account was created.
    /// </summary>
    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>
    /// Gets or sets the UTC timestamp when the user account was last modified.
    /// </summary>
    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    /// <summary>
    /// Captures any extra elements in MongoDB documents to ensure resilient deserialization.
    /// </summary>
    [BsonExtraElements]
    public BsonDocument? ExtraElements { get; set; }
}
