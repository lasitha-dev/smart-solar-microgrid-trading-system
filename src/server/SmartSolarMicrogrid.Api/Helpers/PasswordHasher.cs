/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Security helper providing cryptographic one-way password hashing and verification using BCrypt.
 */

namespace SmartSolarMicrogrid.Api.Helpers;

/// <summary>
/// Provides secure password hashing and verification methods utilizing the BCrypt algorithm.
/// Adheres to the FAT Service invariant ensuring zero plaintext password persistence.
/// </summary>
public static class PasswordHasher
{
    private const int DefaultWorkFactor = 11;

    /// <summary>
    /// Computes a cryptographically secure salt and hashes the plaintext password using BCrypt.
    /// </summary>
    /// <param name="password">The plaintext password to hash.</param>
    /// <returns>A salted and hashed password string.</returns>
    /// <exception cref="ArgumentException">Thrown when the password is null or empty.</exception>
    public static string HashPassword(string password)
    {
        if (string.IsNullOrWhiteSpace(password))
        {
            throw new ArgumentException("Password cannot be null or empty.", nameof(password));
        }

        return BCrypt.Net.BCrypt.HashPassword(password, workFactor: DefaultWorkFactor);
    }

    /// <summary>
    /// Verifies whether a supplied plaintext password matches a stored BCrypt password hash.
    /// </summary>
    /// <param name="password">The candidate plaintext password.</param>
    /// <param name="passwordHash">The persisted BCrypt hash.</param>
    /// <returns><c>true</c> if the password matches the hash; otherwise, <c>false</c>.</returns>
    public static bool VerifyPassword(string password, string passwordHash)
    {
        if (string.IsNullOrWhiteSpace(password) || string.IsNullOrWhiteSpace(passwordHash))
        {
            return false;
        }

        try
        {
            return BCrypt.Net.BCrypt.Verify(password, passwordHash);
        }
        catch
        {
            return false;
        }
    }
}
