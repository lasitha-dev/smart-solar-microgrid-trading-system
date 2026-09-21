/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: API Controller for Backoffice administrative user management and lifecycle operations.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

/// <summary>
/// Provides RESTful endpoints for Backoffice administrators to manage system user accounts and review account lifecycles.
/// All endpoints in this controller require Backoffice role authorization.
/// </summary>
[ApiController]
[Route("api/users")]
[Authorize(Roles = "Backoffice")]
[Produces("application/json")]
public class UsersController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<UsersController> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="UsersController"/> class.
    /// </summary>
    /// <param name="userService">The user management business logic service.</param>
    /// <param name="logger">The system logger instance.</param>
    /// <exception cref="ArgumentNullException">Thrown when required dependencies are null.</exception>
    public UsersController(IUserService userService, ILogger<UsersController> logger)
    {
        _userService = userService ?? throw new ArgumentNullException(nameof(userService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Creates a new Backoffice administrator or Grid Operator account.
    /// Restricted strictly to Backoffice administrators.
    /// </summary>
    /// <param name="request">The staff user creation payload.</param>
    /// <returns>HTTP 201 with created user details; HTTP 400 on validation failure; HTTP 409 on duplicate NIC/username.</returns>
    /// <response code="201">Staff account successfully created.</response>
    /// <response code="400">Request validation failed or illegal role assignment.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - User does not have Backoffice role.</response>
    /// <response code="409">User with specified NIC or Username already exists.</response>
    [HttpPost]
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
    /// Retrieves all system users with optional filtering by role and lifecycle status.
    /// </summary>
    /// <param name="role">Optional filter by user role (Backoffice, GridOperator, Prosumer).</param>
    /// <param name="status">Optional filter by account status (Active, Deactivated, PendingActivation).</param>
    /// <returns>HTTP 200 with list of user accounts.</returns>
    /// <response code="200">List of users matching the query filters.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - User does not have Backoffice role.</response>
    [HttpGet]
    [ProducesResponseType(typeof(ApiResponseDto<List<UserResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetAllUsers([FromQuery] UserRole? role, [FromQuery] AccountStatus? status)
    {
        var users = await _userService.GetAllUsersAsync(role, status);
        return Ok(ApiResponseDto<List<UserResponseDto>>.Ok(users, $"Retrieved {users.Count} user accounts."));
    }

    /// <summary>
    /// Retrieves all Prosumers currently in 'PendingActivation' status awaiting Backoffice review.
    /// </summary>
    /// <returns>HTTP 200 with list of pending prosumer registrations.</returns>
    /// <response code="200">List of pending prosumers.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - User does not have Backoffice role.</response>
    [HttpGet("pending")]
    [ProducesResponseType(typeof(ApiResponseDto<List<UserResponseDto>>), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPendingProsumers()
    {
        var pendingProsumers = await _userService.GetPendingProsumersAsync();
        return Ok(ApiResponseDto<List<UserResponseDto>>.Ok(pendingProsumers, $"Retrieved {pendingProsumers.Count} pending prosumer activations."));
    }

    /// <summary>
    /// Activates a pending prosumer registration or reactivates a deactivated user account.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>HTTP 200 with activated user details; HTTP 400 if already active; HTTP 404 if not found.</returns>
    /// <response code="200">User account successfully activated.</response>
    /// <response code="400">User account is already active or invalid ID.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - User does not have Backoffice role.</response>
    /// <response code="404">User not found.</response>
    [HttpPatch("{id}/activate")]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ActivateUser([FromRoute] string id)
    {
        var (success, message, statusCode, data) = await _userService.ActivateUserAsync(id);

        if (!success)
        {
            _logger.LogWarning("Account activation failed for user ID '{Id}': {Reason} (HTTP {StatusCode})",
                id, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("User ID '{Id}' ({Username}) activated by Backoffice administrator.",
            id, data!.Username);

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Deactivates an active user account.
    /// Protects against administrators deactivating their own currently logged-in account.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>HTTP 200 with deactivated user details; HTTP 400 if self-deactivation attempted or already deactivated.</returns>
    /// <response code="200">User account successfully deactivated.</response>
    /// <response code="400">Cannot deactivate own account or account is already deactivated.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - User does not have Backoffice role.</response>
    /// <response code="404">User not found.</response>
    [HttpPatch("{id}/deactivate")]
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
            _logger.LogWarning("Account deactivation failed for user ID '{Id}': {Reason} (HTTP {StatusCode})",
                id, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("User ID '{Id}' ({Username}) deactivated by Backoffice administrator.",
            id, data!.Username);

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }
}
