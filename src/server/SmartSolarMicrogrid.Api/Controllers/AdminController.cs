/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: RESTful API Controller for Backoffice and Administrator web portal operations.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

/// <summary>
/// Provides administrative RESTful API endpoints for the Backoffice Web Portal.
/// Restricted strictly to authenticated users with Backoffice or Administrator roles.
/// </summary>
[ApiController]
[Route("api/admin")]
[Authorize(Roles = "Backoffice,Administrator")]
[Produces("application/json")]
public class AdminController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<AdminController> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="AdminController"/> class.
    /// </summary>
    /// <param name="userService">The user business service.</param>
    /// <param name="logger">The system logger.</param>
    /// <exception cref="ArgumentNullException">Thrown when dependencies are null.</exception>
    public AdminController(IUserService userService, ILogger<AdminController> logger)
    {
        _userService = userService ?? throw new ArgumentNullException(nameof(userService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Retrieves system users with instant text search, role filtering, status filtering, and pagination.
    /// </summary>
    /// <param name="search">Text query matching NIC, Username, or Full Name.</param>
    /// <param name="role">Optional filter by user role (Backoffice, Administrator, GridOperator, Prosumer).</param>
    /// <param name="status">Optional filter by account status (Active, Deactivated, PendingActivation).</param>
    /// <param name="page">1-indexed page number (default 1).</param>
    /// <param name="pageSize">Number of items per page (default 50, max 1000).</param>
    /// <returns>HTTP 200 with list of matching user accounts.</returns>
    [HttpGet("users")]
    [ProducesResponseType(typeof(ApiResponseDto<List<UserResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetUsers(
        [FromQuery] string? search,
        [FromQuery] UserRole? role,
        [FromQuery] AccountStatus? status,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50)
    {
        var users = await _userService.GetUsersFilteredAsync(search, role, status, page, pageSize);
        return Ok(ApiResponseDto<List<UserResponseDto>>.Ok(users, $"Retrieved {users.Count} user accounts."));
    }

    /// <summary>
    /// Retrieves all Prosumers currently in 'PendingActivation' status awaiting Backoffice review.
    /// </summary>
    /// <returns>HTTP 200 with list of pending prosumer registrations.</returns>
    [HttpGet("prosumers/pending")]
    [ProducesResponseType(typeof(ApiResponseDto<List<UserResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPendingProsumers()
    {
        var pendingProsumers = await _userService.GetPendingProsumersAsync();
        return Ok(ApiResponseDto<List<UserResponseDto>>.Ok(pendingProsumers, $"Retrieved {pendingProsumers.Count} pending prosumer registrations."));
    }

    /// <summary>
    /// Transitions a user account's lifecycle status (e.g. approve prosumer, reject/deactivate, or reactivate).
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="request">The target status and optional remark/rejection reason.</param>
    /// <returns>HTTP 200 with updated user details; HTTP 400 or HTTP 404 on failure.</returns>
    [HttpPatch("users/{id}/status")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateUserStatus([FromRoute] string id, [FromBody] UserStatusUpdateDto request)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var currentUserId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value 
            ?? User.FindFirst("userId")?.Value;

        var (success, message, statusCode, data) = await _userService.UpdateUserStatusAsync(id, request.Status, request.Reason, currentUserId);

        if (!success)
        {
            _logger.LogWarning("Status transition to '{Status}' failed for user ID '{Id}': {Reason} (HTTP {StatusCode})",
                request.Status, id, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("User ID '{Id}' ({Username}) status transitioned to '{Status}' by Backoffice officer.",
            id, data!.Username, request.Status);

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Activates a pending prosumer registration or reactivates a deactivated user account.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>HTTP 200 with activated user details; HTTP 400 on error.</returns>
    [HttpPatch("users/{id}/activate")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ActivateUser([FromRoute] string id)
    {
        var (success, message, statusCode, data) = await _userService.ActivateUserAsync(id);

        if (!success)
        {
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Deactivates an active user account.
    /// Protects against administrators deactivating their own currently logged-in account.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>HTTP 200 with deactivated user details; HTTP 400 on error.</returns>
    [HttpPatch("users/{id}/deactivate")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> DeactivateUser([FromRoute] string id)
    {
        var currentUserId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value 
            ?? User.FindFirst("userId")?.Value;

        var (success, message, statusCode, data) = await _userService.DeactivateUserAsync(id, currentUserId);

        if (!success)
        {
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Creates a new Backoffice administrator or Grid Operator account.
    /// </summary>
    /// <param name="request">The staff user creation payload.</param>
    /// <returns>HTTP 201 with created user details; HTTP 400 on validation failure; HTTP 409 on conflict.</returns>
    [HttpPost("users")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> CreateStaffUser([FromBody] UserCreateDto request)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.CreateStaffUserAsync(request);

        if (!success)
        {
            _logger.LogWarning("Staff creation failed for username '{Username}': {Reason} (HTTP {StatusCode})",
                request.Username, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Backoffice administrator created staff user '{Username}' with role '{Role}'.",
            data!.Username, data.Role);

        return StatusCode(StatusCodes.Status201Created, ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Updates a staff or Grid Operator user account profile.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="request">The staff user update payload.</param>
    /// <returns>HTTP 200 with updated user details; HTTP 400 on error; HTTP 404 if not found; HTTP 409 on conflict.</returns>
    [HttpPut("users/{id}")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> UpdateStaffUser([FromRoute] string id, [FromBody] UserUpdateDto request)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.UpdateStaffUserAsync(id, request);

        if (!success)
        {
            _logger.LogWarning("Staff update failed for user ID '{Id}': {Reason} (HTTP {StatusCode})",
                id, message, statusCode);
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Backoffice administrator updated staff user ID '{Id}' ({Username}).", id, data!.Username);
        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Deletes a staff or Grid Operator user account.
    /// Returns HTTP 409 Conflict if a Grid Operator has active reservations or assigned stations.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>HTTP 200 with success message; HTTP 404 if not found; HTTP 409 on business rule conflict.</returns>
    [HttpDelete("users/{id}")]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> DeleteStaffUser([FromRoute] string id)
    {
        var (success, message, statusCode) = await _userService.DeleteStaffUserAsync(id);

        if (!success)
        {
            _logger.LogWarning("Staff deletion rejected for user ID '{Id}': {Reason} (HTTP {StatusCode})",
                id, message, statusCode);
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Backoffice administrator deleted staff user ID '{Id}'.", id);
        return Ok(ApiResponseDto<object>.Ok(new { }, message));
    }
}
