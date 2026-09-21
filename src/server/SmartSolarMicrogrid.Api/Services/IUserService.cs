/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Service contract defining user authentication, prosumer registration, and account lifecycle business logic.
 */

using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Defines business operations for user authentication, prosumer registration, and profile administration.
/// Encapsulates the FAT Service pattern ensuring zero business logic in client layers.
/// </summary>
public interface IUserService
{
    /// <summary>
    /// Authenticates a user by username or NIC and returns an access token if active.
    /// </summary>
    /// <param name="request">The login request payload containing credentials.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and response DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, LoginResponseDto? Data)> AuthenticateAsync(LoginRequestDto request);

    /// <summary>
    /// Registers a new solar prosumer account with initial status "PendingActivation".
    /// </summary>
    /// <param name="request">The registration details submitted by the prosumer.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and sanitized user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> RegisterProsumerAsync(ProsumerRegisterDto request);

    /// <summary>
    /// Creates a Backoffice or Grid Operator user account (restricted to Backoffice administrators).
    /// </summary>
    /// <param name="request">The staff user creation payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and created user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> CreateStaffUserAsync(UserCreateDto request);

    /// <summary>
    /// Retrieves all system users with optional filtering by role and status.
    /// </summary>
    /// <param name="role">Optional filter by user role.</param>
    /// <param name="status">Optional filter by account status.</param>
    /// <returns>A list of sanitized user response DTOs.</returns>
    Task<List<UserResponseDto>> GetAllUsersAsync(UserRole? role = null, AccountStatus? status = null);

    /// <summary>
    /// Retrieves all prosumers whose status is currently PendingActivation.
    /// </summary>
    /// <returns>A list of pending prosumer DTOs.</returns>
    Task<List<UserResponseDto>> GetPendingProsumersAsync();

    /// <summary>
    /// Activates a user account (e.g. approving a pending prosumer or reactivating a deactivated account).
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> ActivateUserAsync(string id);

    /// <summary>
    /// Deactivates a user account (with protection against self-deactivation for administrators).
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="currentUserId">The ID of the administrator invoking deactivation.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> DeactivateUserAsync(string id, string? currentUserId = null);

    /// <summary>
    /// Updates permitted profile fields (FullName, Phone) for a solar prosumer identified by NIC.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number.</param>
    /// <param name="request">The profile update payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateProsumerProfileAsync(string nic, ProsumerUpdateDto request);

    /// <summary>
    /// Handles a prosumer request to self-deactivate their account.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number of the requesting prosumer.</param>
    /// <param name="request">Optional deactivation reason and remarks payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> RequestProsumerDeactivationAsync(string nic, DeactivationRequestDto? request);

    /// <summary>
    /// Retrieves a prosumer profile by their National Identity Card (NIC) number.
    /// </summary>
    /// <param name="nic">The unique NIC number.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and sanitized user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> GetProsumerProfileAsync(string nic);

    /// <summary>
    /// Retrieves a user entity by their database document identifier.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>The user entity if found; otherwise, null.</returns>
    Task<User?> GetUserByIdAsync(string id);

    /// <summary>
    /// Retrieves a user entity by their National Identity Card (NIC) number.
    /// </summary>
    /// <param name="nic">The unique NIC number.</param>
    /// <returns>The user entity if found; otherwise, null.</returns>
    Task<User?> GetUserByNicAsync(string nic);
}
