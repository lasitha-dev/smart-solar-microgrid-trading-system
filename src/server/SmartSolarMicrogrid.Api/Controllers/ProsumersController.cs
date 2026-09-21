/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: API Controller managing Solar Prosumer registrations, profile details, and deactivation requests.
 */

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

/// <summary>
/// Provides RESTful endpoints for solar prosumer registration, profile management, and self-deactivation workflows.
/// </summary>
[ApiController]
[Route("api/prosumers")]
[Produces("application/json")]
public class ProsumersController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<ProsumersController> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="ProsumersController"/> class.
    /// </summary>
    /// <param name="userService">The user business logic service.</param>
    /// <param name="logger">The system logger instance.</param>
    /// <exception cref="ArgumentNullException">Thrown when required dependencies are null.</exception>
    public ProsumersController(IUserService userService, ILogger<ProsumersController> logger)
    {
        _userService = userService ?? throw new ArgumentNullException(nameof(userService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Registers a new Solar Prosumer with initial 'PendingActivation' status.
    /// </summary>
    /// <param name="request">The prosumer registration details.</param>
    /// <returns>HTTP 201 with created prosumer info; HTTP 409 on duplicate NIC or username.</returns>
    /// <response code="201">Prosumer registered successfully in PendingActivation state.</response>
    /// <response code="400">Request validation failed or missing required fields.</response>
    /// <response code="409">NIC or Username already exists in the system.</response>
    [HttpPost]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> Register([FromBody] ProsumerRegisterDto request)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.RegisterProsumerAsync(request);

        if (!success)
        {
            _logger.LogWarning("Prosumer registration failed for NIC '{Nic}': {Reason} (HTTP {StatusCode})", 
                request.Nic, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Prosumer '{Username}' (NIC: {Nic}) registered with PendingActivation status.", 
            data?.Username, data?.Nic);

        return StatusCode(StatusCodes.Status201Created, ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Retrieves a Prosumer profile by their unique National Identity Card (NIC) number.
    /// </summary>
    /// <param name="nic">The unique NIC number.</param>
    /// <returns>HTTP 200 with prosumer details; HTTP 404 if not found.</returns>
    /// <response code="200">Prosumer profile found.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - Caller not authorized to access this profile.</response>
    /// <response code="404">No prosumer exists with the given NIC.</response>
    [HttpGet("{nic}")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetByNic([FromRoute] string nic)
    {
        if (!IsAuthorizedForNic(nic))
        {
            return StatusCode(StatusCodes.Status403Forbidden, ApiResponseDto<object>.Fail("You are not authorized to view another user's profile."));
        }

        var user = await _userService.GetUserByNicAsync(nic);

        if (user == null)
        {
            return NotFound(ApiResponseDto<object>.Fail($"Prosumer with NIC '{nic}' was not found."));
        }

        var dto = new UserResponseDto
        {
            Id = user.Id ?? string.Empty,
            Nic = user.Nic,
            Username = user.Username,
            FullName = user.FullName,
            Phone = user.Phone,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt,
            UpdatedAt = user.UpdatedAt
        };

        return Ok(ApiResponseDto<UserResponseDto>.Ok(dto));
    }

    /// <summary>
    /// Updates permitted profile fields (FullName, Phone) for a solar prosumer identified by NIC.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number.</param>
    /// <param name="request">The profile update payload.</param>
    /// <returns>HTTP 200 with updated profile details; HTTP 400 on validation failure; HTTP 404 if not found.</returns>
    /// <response code="200">Profile successfully updated.</response>
    /// <response code="400">Request validation failed.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - Caller not authorized to modify this profile.</response>
    /// <response code="404">Prosumer not found.</response>
    [HttpPut("{nic}")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateProfile([FromRoute] string nic, [FromBody] ProsumerUpdateDto request)
    {
        if (!IsAuthorizedForNic(nic))
        {
            return StatusCode(StatusCodes.Status403Forbidden, ApiResponseDto<object>.Fail("You are not authorized to update another user's profile."));
        }

        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.UpdateProsumerProfileAsync(nic, request);

        if (!success)
        {
            _logger.LogWarning("Profile update failed for NIC '{Nic}': {Reason} (HTTP {StatusCode})", 
                nic, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Profile updated successfully for prosumer NIC '{Nic}'.", nic);
        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Requests self-deactivation of a Prosumer account.
    /// Initiates account deactivation workflow and transitions account status to Deactivated.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number.</param>
    /// <param name="request">Optional deactivation reason and feedback payload.</param>
    /// <returns>HTTP 200 with updated account status; HTTP 400 on error; HTTP 404 if not found.</returns>
    /// <response code="200">Account deactivation request processed successfully.</response>
    /// <response code="400">Account is already deactivated or request invalid.</response>
    /// <response code="401">Unauthorized - Bearer token missing or invalid.</response>
    /// <response code="403">Forbidden - Caller not authorized to request deactivation for this NIC.</response>
    /// <response code="404">Prosumer not found.</response>
    [HttpPatch("{nic}/request-deactivation")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status403Forbidden)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> RequestDeactivation([FromRoute] string nic, [FromBody] DeactivationRequestDto? request)
    {
        if (!IsAuthorizedForNic(nic))
        {
            return StatusCode(StatusCodes.Status403Forbidden, ApiResponseDto<object>.Fail("You are not authorized to request deactivation for another user's account."));
        }

        var (success, message, statusCode, data) = await _userService.RequestProsumerDeactivationAsync(nic, request);

        if (!success)
        {
            _logger.LogWarning("Deactivation request failed for NIC '{Nic}': {Reason} (HTTP {StatusCode})", 
                nic, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Prosumer account with NIC '{Nic}' requested self-deactivation.", nic);
        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Validates whether the authenticated caller has permission to access or modify resources belonging to the specified NIC.
    /// Returns true if caller's token NIC matches the route NIC or if the caller holds the Backoffice role.
    /// </summary>
    /// <param name="targetNic">The target NIC from the route parameter.</param>
    /// <returns>True if authorized; otherwise false.</returns>
    private bool IsAuthorizedForNic(string targetNic)
    {
        if (User.IsInRole(UserRole.Backoffice.ToString()))
        {
            return true;
        }

        var tokenNic = User.FindFirst("nic")?.Value;
        return !string.IsNullOrWhiteSpace(tokenNic) && 
               string.Equals(tokenNic, targetNic.Trim(), StringComparison.OrdinalIgnoreCase);
    }
}
