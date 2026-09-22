/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: API Controller exposing authentication and prosumer registration endpoints.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Controllers;

/// <summary>
/// Provides HTTP REST endpoints for user authentication, token issuance, and prosumer self-registration.
/// </summary>
[ApiController]
[Route("api/auth")]
[Produces("application/json")]
public class AuthController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<AuthController> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="AuthController"/> class.
    /// </summary>
    /// <param name="userService">The user business logic service.</param>
    /// <param name="logger">The system logger instance.</param>
    /// <exception cref="ArgumentNullException">Thrown when required dependencies are null.</exception>
    public AuthController(IUserService userService, ILogger<AuthController> logger)
    {
        _userService = userService ?? throw new ArgumentNullException(nameof(userService));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Authenticates a Backoffice administrator, Grid Operator, or Solar Prosumer.
    /// Validates credentials and returns a JWT bearer token if the account is in Active status.
    /// </summary>
    /// <param name="request">The login request payload containing username/NIC and password.</param>
    /// <returns>HTTP 200 with JWT token and user profile on success; HTTP 401 for bad credentials; HTTP 403 for inactive status.</returns>
    /// <response code="200">Authentication successful; returns token and user role.</response>
    /// <response code="400">Request validation failed or missing required fields.</response>
    /// <response code="401">Invalid username/NIC or password.</response>
    /// <response code="403">Account is pending activation or deactivated.</response>
    [HttpPost("login")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<LoginResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> Login([FromBody] LoginRequestDto request)
    {
        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.AuthenticateAsync(request);

        if (!success)
        {
            _logger.LogWarning("Authentication failed for identifier '{Identifier}': {Reason} (HTTP {StatusCode})", 
                request.Identifier, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("User '{Username}' ({Role}) successfully authenticated.", data!.Username, data.Role);
        return Ok(ApiResponseDto<LoginResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Registers a new Solar Prosumer via the authentication endpoint.
    /// Enforces the business invariant that all new prosumer registrations default to 'PendingActivation' status.
    /// </summary>
    /// <param name="request">The registration payload containing prosumer details.</param>
    /// <returns>HTTP 201 with sanitized user profile on successful registration; HTTP 409 if NIC or username already exists.</returns>
    /// <response code="201">Prosumer registration submitted; status is PendingActivation.</response>
    /// <response code="400">Validation failure in registration form.</response>
    /// <response code="409">Duplicate NIC or Username conflict.</response>
    [HttpPost("register")]
    [HttpPost("register-prosumer")]
    [AllowAnonymous]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status409Conflict)]
    public async Task<IActionResult> RegisterProsumer([FromBody] ProsumerRegisterDto request)
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
            data!.Username, data.Nic);

        return StatusCode(StatusCodes.Status201Created, ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Retrieves the profile details of the currently authenticated user.
    /// </summary>
    /// <returns>HTTP 200 with sanitized user profile.</returns>
    [HttpGet("profile")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetCurrentProfile()
    {
        var userId = GetCurrentUserId();
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(ApiResponseDto<object>.Fail("Invalid session or missing user identifier in token."));
        }

        var (success, message, statusCode, data) = await _userService.GetUserProfileAsync(userId);
        if (!success)
        {
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data));
    }

    /// <summary>
    /// Updates the editable profile fields (FullName, Phone) for the currently authenticated user.
    /// </summary>
    /// <param name="request">The profile update payload.</param>
    /// <returns>HTTP 200 with updated user profile.</returns>
    [HttpPut("profile")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UpdateCurrentProfile([FromBody] UserProfileUpdateDto request)
    {
        var userId = GetCurrentUserId();
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(ApiResponseDto<object>.Fail("Invalid session or missing user identifier in token."));
        }

        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.UpdateUserProfileAsync(userId, request);
        if (!success)
        {
            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Changes the password of the currently authenticated user.
    /// Validates current password and enforces new password complexity rules.
    /// </summary>
    /// <param name="request">The change password request payload.</param>
    /// <returns>HTTP 200 on success; HTTP 400 on incorrect current password or validation failure.</returns>
    [HttpPost("change-password")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<UserResponseDto>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordDto request)
    {
        var userId = GetCurrentUserId();
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(ApiResponseDto<object>.Fail("Invalid session or missing user identifier in token."));
        }

        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, data) = await _userService.ChangePasswordAsync(userId, request);
        if (!success)
        {
            _logger.LogWarning("Change password failed for user '{UserId}': {Reason} (HTTP {StatusCode})",
                userId, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Password successfully changed for user '{UserId}' ({Username}).", userId, data?.Username);
        return Ok(ApiResponseDto<UserResponseDto>.Ok(data, message));
    }

    /// <summary>
    /// Permanently deletes the currently authenticated user's account upon verifying their registered email.
    /// </summary>
    /// <param name="request">The delete account confirmation payload containing the confirmation email.</param>
    /// <returns>HTTP 200 on successful deletion; HTTP 400 on email mismatch or validation failure.</returns>
    [HttpPost("delete-account")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponseDto<object>), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> DeleteAccount([FromBody] DeleteAccountDto request)
    {
        var userId = GetCurrentUserId();
        if (string.IsNullOrEmpty(userId))
        {
            return Unauthorized(ApiResponseDto<object>.Fail("Invalid session or missing user identifier in token."));
        }

        if (!ModelState.IsValid)
        {
            var errors = string.Join("; ", ModelState.Values.SelectMany(v => v.Errors).Select(e => e.ErrorMessage));
            return BadRequest(ApiResponseDto<object>.Fail(errors));
        }

        var (success, message, statusCode, _) = await _userService.DeleteAccountAsync(userId, request);
        if (!success)
        {
            _logger.LogWarning("Account deletion failed for user '{UserId}': {Reason} (HTTP {StatusCode})",
                userId, message, statusCode);

            return StatusCode(statusCode, ApiResponseDto<object>.Fail(message));
        }

        _logger.LogInformation("Account '{UserId}' successfully deleted by the user.", userId);
        return Ok(ApiResponseDto<object>.Ok(null, message));
    }

    private string? GetCurrentUserId()
    {
        return User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value
            ?? User.FindFirst("userId")?.Value
            ?? User.FindFirst("id")?.Value
            ?? User.FindFirst("nic")?.Value;
    }
}
