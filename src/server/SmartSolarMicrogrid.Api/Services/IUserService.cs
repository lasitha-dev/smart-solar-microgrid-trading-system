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
    /// Retrieves system users with search, role, status filters, and optional pagination.
    /// </summary>
    /// <param name="search">Text query matching NIC, Username, or FullName.</param>
    /// <param name="role">Optional filter by user role.</param>
    /// <param name="status">Optional filter by account status.</param>
    /// <param name="page">1-indexed page number.</param>
    /// <param name="pageSize">Number of records per page (default 50).</param>
    /// <returns>A list of sanitized user response DTOs.</returns>
    Task<List<UserResponseDto>> GetUsersFilteredAsync(string? search = null, UserRole? role = null, AccountStatus? status = null, int page = 1, int pageSize = 50);

    /// <summary>
    /// Retrieves all prosumers whose status is currently PendingActivation.
    /// </summary>
    /// <returns>A list of pending prosumer DTOs.</returns>
    Task<List<UserResponseDto>> GetPendingProsumersAsync();

    /// <summary>
    /// Updates a user account's lifecycle status (Active, Deactivated, PendingActivation) with optional remarks.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="newStatus">The target lifecycle status.</param>
    /// <param name="reason">Optional rejection or state change remark.</param>
    /// <param name="currentUserId">The ID of the administrator invoking the transition.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateUserStatusAsync(string id, AccountStatus newStatus, string? reason = null, string? currentUserId = null);

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
    /// Retrieves an authenticated user's profile details by their database identifier.
    /// </summary>
    /// <param name="userId">The unique MongoDB user document identifier.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and sanitized user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> GetUserProfileAsync(string userId);

    /// <summary>
    /// Updates permitted profile fields (FullName, Phone) for any authenticated user.
    /// </summary>
    /// <param name="userId">The unique MongoDB user document identifier.</param>
    /// <param name="request">The profile update payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateUserProfileAsync(string userId, UserProfileUpdateDto request);

    /// <summary>
    /// Changes an authenticated user's password following verification of their current password and complexity rules.
    /// </summary>
    /// <param name="userId">The unique MongoDB user document identifier.</param>
    /// <param name="request">The change password request payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and user response DTO.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> ChangePasswordAsync(string userId, ChangePasswordDto request);

    /// <summary>
    /// Permanently deletes an authenticated user's account upon verifying their registered email address.
    /// </summary>
    /// <param name="userId">The unique MongoDB user document identifier.</param>
    /// <param name="request">The delete account confirmation payload containing the confirmation email.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and null data.</returns>
    Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> DeleteAccountAsync(string userId, DeleteAccountDto request);

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
