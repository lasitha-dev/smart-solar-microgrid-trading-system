/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Interface definition for JWT token generation and validation services.
 */

using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Defines contracts for generating cryptographic JSON Web Tokens (JWT) for authenticated users.
/// </summary>
public interface ITokenService
{
    /// <summary>
    /// Generates a signed JWT bearer token containing role, user ID, and NIC claims.
    /// </summary>
    /// <param name="user">The authenticated user entity.</param>
    /// <returns>A tuple containing the raw JWT string and the UTC expiration timestamp.</returns>
    (string Token, DateTime ExpiresAt) GenerateToken(User user);
}
