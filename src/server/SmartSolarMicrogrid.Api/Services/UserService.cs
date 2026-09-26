/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Core user service implementing authentication, prosumer registration, staff provisioning, and lifecycle transitions.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Models.Enums;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Implements business logic for user authentication, prosumer registration, staff user management, and lifecycle enforcement.
/// All authoritative business rules reside strictly in this service layer following the FAT Service invariant.
/// </summary>
public class UserService : IUserService
{
    private readonly MongoDbContext _dbContext;
    private readonly ITokenService _tokenService;
    private readonly IEmailService _emailService;

    /// <summary>
    /// Initializes a new instance of the <see cref="UserService"/> class.
    /// </summary>
    /// <param name="dbContext">The MongoDB database context.</param>
    /// <param name="tokenService">The JWT token generation service.</param>
    /// <param name="emailService">The automated email notification service.</param>
    /// <exception cref="ArgumentNullException">Thrown when required dependencies are null.</exception>
    public UserService(MongoDbContext dbContext, ITokenService tokenService, IEmailService emailService)
    {
        _dbContext = dbContext ?? throw new ArgumentNullException(nameof(dbContext));
        _tokenService = tokenService ?? throw new ArgumentNullException(nameof(tokenService));
        _emailService = emailService ?? throw new ArgumentNullException(nameof(emailService));
    }

    /// <summary>
    /// Authenticates user credentials and checks account status.
    /// Blocks access if the account is in PendingActivation or Deactivated status.
    /// </summary>
    /// <param name="request">The login request payload containing identifier and password.</param>
    /// <returns>A tuple with success flag, descriptive message, HTTP status code, and login response DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, LoginResponseDto? Data)> AuthenticateAsync(LoginRequestDto request)
    {
        if (request == null || string.IsNullOrWhiteSpace(request.Identifier) || string.IsNullOrWhiteSpace(request.Password))
        {
            return (false, "Identifier and password are required.", StatusCodes.Status400BadRequest, null);
        }

        var trimmedIdentifier = request.Identifier.Trim();

        // Search by Username or NIC (case-insensitive)
        var filter = Builders<User>.Filter.Or(
            Builders<User>.Filter.Regex(u => u.Username, new BsonRegularExpression($"^{trimmedIdentifier}$", "i")),
            Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{trimmedIdentifier}$", "i"))
        );

        var user = await _dbContext.Users.Find(filter).FirstOrDefaultAsync();

        if (user == null)
        {
            return (false, "Invalid username/NIC or password.", StatusCodes.Status401Unauthorized, null);
        }

        // Verify password hash
        if (!PasswordHasher.VerifyPassword(request.Password, user.PasswordHash))
        {
            return (false, "Invalid username/NIC or password.", StatusCodes.Status401Unauthorized, null);
        }

        // Enforce account lifecycle status guards
        if (user.Status == AccountStatus.PendingActivation)
        {
            return (false, "Your account is currently pending activation. Please wait for Backoffice administrator approval.", StatusCodes.Status403Forbidden, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Your account has been deactivated. Please contact system administrators.", StatusCodes.Status403Forbidden, null);
        }

        if (user.Status != AccountStatus.Active)
        {
            return (false, "Account is not authorized for login.", StatusCodes.Status403Forbidden, null);
        }

        // Generate JWT token
        var (token, expiresAt) = _tokenService.GenerateToken(user);

        var responseDto = new LoginResponseDto
        {
            Token = token,
            TokenType = "Bearer",
            UserId = user.Id ?? string.Empty,
            Nic = user.Nic,
            Username = user.Username,
            FullName = user.FullName,
            Phone = user.Phone,
            Email = user.Email ?? (user.ExtraElements != null && user.ExtraElements.Contains("email") && !user.ExtraElements["email"].IsBsonNull ? user.ExtraElements["email"].AsString : string.Empty),
            Address = user.Address,
            Latitude = user.Latitude,
            Longitude = user.Longitude,
            Role = user.Role,
            Status = user.Status,
            ExpiresAt = expiresAt
        };

        return (true, "Authentication successful.", StatusCodes.Status200OK, responseDto);
    }

    /// <summary>
    /// Registers a new prosumer in the system with initial status 'PendingActivation'.
    /// Validates uniqueness of NIC and Username before persisting.
    /// </summary>
    /// <param name="request">The registration payload submitted by the prosumer.</param>
    /// <returns>A tuple with success flag, descriptive message, HTTP status code, and created user response DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> RegisterProsumerAsync(ProsumerRegisterDto request)
    {
        if (request == null)
        {
            return (false, "Registration payload cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var trimmedNic = request.Nic.Trim();
        var trimmedUsername = request.Username.Trim();
        var trimmedEmail = request.Email.Trim().ToLowerInvariant();

        // Check for existing NIC (case-insensitive)
        var nicFilter = Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{trimmedNic}$", "i"));
        var existingByNic = await _dbContext.Users.Find(nicFilter).FirstOrDefaultAsync();
        if (existingByNic != null)
        {
            return (false, $"A user with NIC '{trimmedNic}' is already registered in the system.", StatusCodes.Status409Conflict, null);
        }

        // Check for existing Username (case-insensitive)
        var usernameFilter = Builders<User>.Filter.Regex(u => u.Username, new BsonRegularExpression($"^{trimmedUsername}$", "i"));
        var existingByUsername = await _dbContext.Users.Find(usernameFilter).FirstOrDefaultAsync();
        if (existingByUsername != null)
        {
            return (false, $"A user with username '{trimmedUsername}' already exists.", StatusCodes.Status409Conflict, null);
        }

        // Check password complexity (min 6 chars, uppercase, lowercase, number, symbol)
        if (!IsComplexPassword(request.Password))
        {
            return (false, "Password must be at least 6 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special symbol.", StatusCodes.Status400BadRequest, null);
        }

        // Check for existing Email (case-insensitive)
        var emailFilter = Builders<User>.Filter.Regex(u => u.Email, new BsonRegularExpression($"^{trimmedEmail}$", "i"));
        var existingByEmail = await _dbContext.Users.Find(emailFilter).FirstOrDefaultAsync();
        if (existingByEmail != null)
        {
            return (false, $"A user with email '{trimmedEmail}' is already registered in the system.", StatusCodes.Status409Conflict, null);
        }

        // Create new User entity with PendingActivation status
        var now = DateTime.UtcNow;
        var newUser = new User
        {
            Nic = trimmedNic,
            Username = trimmedUsername,
            Email = trimmedEmail,
            PasswordHash = PasswordHasher.HashPassword(request.Password),
            FullName = request.FullName.Trim(),
            Phone = request.Phone.Trim(),
            Address = request.Address.Trim(),
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            Role = UserRole.Prosumer,
            Status = AccountStatus.PendingActivation,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _dbContext.Users.InsertOneAsync(newUser);

        var responseDto = MapToDto(newUser);
        return (true, "Prosumer registered successfully. Your account is pending Backoffice approval.", StatusCodes.Status201Created, responseDto);
    }

    /// <summary>
    /// Creates a Backoffice or Grid Operator user account.
    /// Restricted to Backoffice administrators; accounts are created in Active status.
    /// </summary>
    /// <param name="request">The staff user creation payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and created user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> CreateStaffUserAsync(UserCreateDto request)
    {
        if (request == null)
        {
            return (false, "User creation payload cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        if (request.Role == UserRole.Prosumer)
        {
            return (false, "Prosumers must be registered via the prosumer registration workflow.", StatusCodes.Status400BadRequest, null);
        }

        var trimmedNic = request.Nic.Trim();
        var trimmedUsername = request.Username.Trim();
        var trimmedEmail = request.Email.Trim().ToLowerInvariant();

        // Check for existing NIC
        var nicFilter = Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{trimmedNic}$", "i"));
        var existingByNic = await _dbContext.Users.Find(nicFilter).FirstOrDefaultAsync();
        if (existingByNic != null)
        {
            return (false, $"A user with NIC '{trimmedNic}' already exists in the system.", StatusCodes.Status409Conflict, null);
        }

        // Check for existing Username
        var usernameFilter = Builders<User>.Filter.Regex(u => u.Username, new BsonRegularExpression($"^{trimmedUsername}$", "i"));
        var existingByUsername = await _dbContext.Users.Find(usernameFilter).FirstOrDefaultAsync();
        if (existingByUsername != null)
        {
            return (false, $"A user with username '{trimmedUsername}' already exists.", StatusCodes.Status409Conflict, null);
        }

        // Check password complexity (min 6 chars, uppercase, lowercase, number, symbol)
        if (!IsComplexPassword(request.Password))
        {
            return (false, "Password must be at least 6 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special symbol.", StatusCodes.Status400BadRequest, null);
        }

        // Check for existing Email (case-insensitive)
        var emailFilter = Builders<User>.Filter.Regex(u => u.Email, new BsonRegularExpression($"^{trimmedEmail}$", "i"));
        var existingByEmail = await _dbContext.Users.Find(emailFilter).FirstOrDefaultAsync();
        if (existingByEmail != null)
        {
            return (false, $"A user with email '{trimmedEmail}' already exists in the system.", StatusCodes.Status409Conflict, null);
        }

        var now = DateTime.UtcNow;
        var newUser = new User
        {
            Nic = trimmedNic,
            Username = trimmedUsername,
            Email = trimmedEmail,
            PasswordHash = PasswordHasher.HashPassword(request.Password),
            FullName = request.FullName.Trim(),
            Phone = request.Phone.Trim(),
            Address = request.Address?.Trim() ?? string.Empty,
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            Role = request.Role,
            Status = AccountStatus.Active,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _dbContext.Users.InsertOneAsync(newUser);

        // Dispatch automated onboarding credentials email via SMTP
        if (!string.IsNullOrWhiteSpace(newUser.Email))
        {
            _ = Task.Run(async () =>
            {
                await _emailService.SendStaffOnboardingEmailAsync(
                    newUser.Email,
                    newUser.FullName,
                    newUser.Username,
                    request.Password,
                    newUser.Role);
            });
        }

        var responseDto = MapToDto(newUser);
        return (true, $"{request.Role} user '{newUser.Username}' created successfully. Onboarding credentials email dispatched.", StatusCodes.Status201Created, responseDto);
    }

    /// <summary>
    /// Updates an existing Backoffice administrator or Grid Operator user account profile.
    /// </summary>
    /// <param name="id">The unique MongoDB document identifier.</param>
    /// <param name="request">The staff user update payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateStaffUserAsync(string id, UserUpdateDto request)
    {
        if (string.IsNullOrWhiteSpace(id) || request == null)
        {
            return (false, "User ID and payload are required.", StatusCodes.Status400BadRequest, null);
        }

        var user = await _dbContext.Users.Find(u => u.Id == id).FirstOrDefaultAsync();
        if (user == null)
        {
            return (false, "User account not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Role == UserRole.Prosumer)
        {
            return (false, "Prosumer profiles cannot be modified via staff update endpoint.", StatusCodes.Status400BadRequest, null);
        }

        var trimmedEmail = request.Email.Trim().ToLowerInvariant();

        // If email changed, check uniqueness
        if (!string.Equals(user.Email, trimmedEmail, StringComparison.OrdinalIgnoreCase))
        {
            var emailFilter = Builders<User>.Filter.And(
                Builders<User>.Filter.Regex(u => u.Email, new BsonRegularExpression($"^{trimmedEmail}$", "i")),
                Builders<User>.Filter.Ne(u => u.Id, id)
            );
            var existingByEmail = await _dbContext.Users.Find(emailFilter).FirstOrDefaultAsync();
            if (existingByEmail != null)
            {
                return (false, $"A user with email '{trimmedEmail}' already exists in the system.", StatusCodes.Status409Conflict, null);
            }
            user.Email = trimmedEmail;
        }

        user.FullName = request.FullName.Trim();
        user.Phone = request.Phone.Trim();
        user.Address = request.Address.Trim();

        if (request.Role.HasValue && request.Role.Value != UserRole.Prosumer)
        {
            user.Role = request.Role.Value;
        }

        user.UpdatedAt = DateTime.UtcNow;

        await _dbContext.Users.ReplaceOneAsync(u => u.Id == id, user);

        // Update station record if operator name changed
        if (user.Role == UserRole.GridOperator)
        {
            var stationUpdate = Builders<SolarStationInfo>.Update
                .Set(s => s.AssignedOperatorName, user.FullName);
            await _dbContext.SolarStations.UpdateManyAsync(s => s.AssignedOperatorId == id, stationUpdate);
        }

        return (true, $"User account '{user.Username}' updated successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Deletes a Backoffice administrator or Grid Operator account.
    /// Strictly enforces the FAT Service Invariant: Grid Operators with active reservations (Pending or Approved)
    /// or active station assignments cannot be deleted and return HTTP 409 Conflict.
    /// </summary>
    /// <param name="id">The unique MongoDB document identifier.</param>
    /// <returns>A tuple with success status, descriptive message, and HTTP status code.</returns>
    public async Task<(bool Success, string Message, int StatusCode)> DeleteStaffUserAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            return (false, "User ID is required.", StatusCodes.Status400BadRequest);
        }

        var user = await _dbContext.Users.Find(u => u.Id == id).FirstOrDefaultAsync();
        if (user == null)
        {
            return (false, "User account not found.", StatusCodes.Status404NotFound);
        }

        if (user.Role == UserRole.GridOperator)
        {
            // FAT Service Invariant: Check for active or approved reservations
            var activeReservationsFilter = Builders<EnergyReservation>.Filter.And(
                Builders<EnergyReservation>.Filter.Eq(r => r.AssignedOperatorId, id),
                Builders<EnergyReservation>.Filter.In(r => r.Status, new[] { "Pending", "Approved" })
            );
            var activeCount = await _dbContext.EnergyReservations.CountDocumentsAsync(activeReservationsFilter);
            if (activeCount > 0)
            {
                return (false, $"Cannot delete Grid Operator '{user.FullName}'. Operator has {activeCount} active or approved reservations.", StatusCodes.Status409Conflict);
            }

            // FAT Service Invariant: Check if operator is currently assigned to any microgrid stations
            var assignedStationsFilter = Builders<SolarStationInfo>.Filter.Eq(s => s.AssignedOperatorId, id);
            var assignedStationsCount = await _dbContext.SolarStations.CountDocumentsAsync(assignedStationsFilter);
            if (assignedStationsCount > 0)
            {
                return (false, $"Cannot delete Grid Operator '{user.FullName}'. Operator is assigned to {assignedStationsCount} microgrid station(s). Reassign stations before deleting.", StatusCodes.Status409Conflict);
            }
        }

        await _dbContext.Users.DeleteOneAsync(u => u.Id == id);
        return (true, $"User account '{user.Username}' deleted successfully.", StatusCodes.Status200OK);
    }

    /// <summary>
    /// Retrieves all system users with optional filtering by role and status.
    /// </summary>
    /// <param name="role">Optional filter by user role.</param>
    /// <param name="status">Optional filter by account status.</param>
    /// <returns>A list of sanitized user response DTOs.</returns>
    public async Task<List<UserResponseDto>> GetAllUsersAsync(UserRole? role = null, AccountStatus? status = null)
    {
        return await GetUsersFilteredAsync(null, role, status, 1, 1000);
    }

    /// <summary>
    /// Retrieves system users with search query, role, status filters, and pagination.
    /// </summary>
    /// <param name="search">Text query matching NIC, Username, or FullName.</param>
    /// <param name="role">Optional filter by user role.</param>
    /// <param name="status">Optional filter by account status.</param>
    /// <param name="page">1-indexed page number.</param>
    /// <param name="pageSize">Number of records per page (default 50).</param>
    /// <returns>A list of sanitized user response DTOs.</returns>
    public async Task<List<UserResponseDto>> GetUsersFilteredAsync(string? search = null, UserRole? role = null, AccountStatus? status = null, int page = 1, int pageSize = 50)
    {
        var filterBuilder = Builders<User>.Filter;
        var filters = new List<FilterDefinition<User>>();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var trimmedSearch = search.Trim();
            var regex = new BsonRegularExpression(trimmedSearch, "i");
            var searchFilter = filterBuilder.Or(
                filterBuilder.Regex(u => u.Nic, regex),
                filterBuilder.Regex(u => u.Username, regex),
                filterBuilder.Regex(u => u.FullName, regex),
                filterBuilder.Regex(u => u.Email, regex)
            );
            filters.Add(searchFilter);
        }

        if (role.HasValue)
        {
            filters.Add(filterBuilder.Eq(u => u.Role, role.Value));
        }

        if (status.HasValue)
        {
            filters.Add(filterBuilder.Eq(u => u.Status, status.Value));
        }

        var filter = filters.Count > 0 ? filterBuilder.And(filters) : filterBuilder.Empty;

        var skip = Math.Max(0, (page - 1) * pageSize);
        var limit = Math.Clamp(pageSize, 1, 1000);

        var users = await _dbContext.Users
            .Find(filter)
            .SortByDescending(u => u.CreatedAt)
            .Skip(skip)
            .Limit(limit)
            .ToListAsync();

        return users.Select(MapToDto).ToList();
    }

    /// <summary>
    /// Updates a user account's lifecycle status (Active, Deactivated, PendingActivation) with optional remarks.
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="newStatus">The target lifecycle status.</param>
    /// <param name="reason">Optional rejection or state change remark.</param>
    /// <param name="currentUserId">The ID of the administrator invoking the transition.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateUserStatusAsync(string id, AccountStatus newStatus, string? reason = null, string? currentUserId = null)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            return (false, "User ID cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(id);
        if (user == null)
        {
            return (false, $"User with ID '{id}' was not found.", StatusCodes.Status404NotFound, null);
        }

        if (newStatus == AccountStatus.Deactivated && !string.IsNullOrWhiteSpace(currentUserId) && string.Equals(id, currentUserId, StringComparison.OrdinalIgnoreCase))
        {
            return (false, "Administrators cannot deactivate their own active account.", StatusCodes.Status400BadRequest, null);
        }

        var now = DateTime.UtcNow;
        var updateBuilder = Builders<User>.Update
            .Set(u => u.Status, newStatus)
            .Set(u => u.UpdatedAt, now);

        if (!string.IsNullOrWhiteSpace(reason))
        {
            updateBuilder = updateBuilder.Set("StatusReason", reason.Trim());
        }

        await _dbContext.Users.UpdateOneAsync(u => u.Id == id, updateBuilder);

        user.Status = newStatus;
        user.UpdatedAt = now;

        var actionText = newStatus switch
        {
            AccountStatus.Active => "activated/approved",
            AccountStatus.Deactivated => "deactivated/rejected",
            AccountStatus.PendingActivation => "moved to pending review",
            _ => "updated"
        };

        return (true, $"User account '{user.Username}' was successfully {actionText}.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Retrieves all prosumers whose status is currently PendingActivation.
    /// </summary>
    /// <returns>A list of pending prosumer DTOs.</returns>
    public async Task<List<UserResponseDto>> GetPendingProsumersAsync()
    {
        var filter = Builders<User>.Filter.And(
            Builders<User>.Filter.Eq(u => u.Role, UserRole.Prosumer),
            Builders<User>.Filter.Eq(u => u.Status, AccountStatus.PendingActivation)
        );

        var pendingUsers = await _dbContext.Users
            .Find(filter)
            .SortByDescending(u => u.CreatedAt)
            .ToListAsync();

        return pendingUsers.Select(MapToDto).ToList();
    }

    /// <summary>
    /// Activates a user account (approving a pending prosumer or reactivating an account).
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> ActivateUserAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            return (false, "User ID cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(id);
        if (user == null)
        {
            return (false, $"User with ID '{id}' was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Active)
        {
            return (false, $"User account '{user.Username}' is already active.", StatusCodes.Status400BadRequest, null);
        }

        var now = DateTime.UtcNow;
        var update = Builders<User>.Update
            .Set(u => u.Status, AccountStatus.Active)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(u => u.Id == id, update);

        user.Status = AccountStatus.Active;
        user.UpdatedAt = now;

        return (true, $"User account '{user.Username}' activated successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Deactivates a user account (with protection against self-deactivation for administrators).
    /// </summary>
    /// <param name="id">The unique MongoDB user document identifier.</param>
    /// <param name="currentUserId">The ID of the administrator invoking deactivation.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> DeactivateUserAsync(string id, string? currentUserId = null)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            return (false, "User ID cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        if (!string.IsNullOrWhiteSpace(currentUserId) && string.Equals(id, currentUserId, StringComparison.OrdinalIgnoreCase))
        {
            return (false, "Administrators cannot deactivate their own active account.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(id);
        if (user == null)
        {
            return (false, $"User with ID '{id}' was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, $"User account '{user.Username}' is already deactivated.", StatusCodes.Status400BadRequest, null);
        }

        var now = DateTime.UtcNow;
        var update = Builders<User>.Update
            .Set(u => u.Status, AccountStatus.Deactivated)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(u => u.Id == id, update);

        user.Status = AccountStatus.Deactivated;
        user.UpdatedAt = now;

        return (true, $"User account '{user.Username}' deactivated successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Updates permitted profile fields (FullName, Phone, Address) for a solar prosumer identified by NIC.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number.</param>
    /// <param name="request">The profile update payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateProsumerProfileAsync(string nic, ProsumerUpdateDto request)
    {
        if (string.IsNullOrWhiteSpace(nic) || request == null)
        {
            return (false, "NIC and update payload are required.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByNicAsync(nic);
        if (user == null)
        {
            return (false, $"Prosumer with NIC '{nic}' was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Deactivated accounts cannot modify profile information.", StatusCodes.Status403Forbidden, null);
        }

        var now = DateTime.UtcNow;
        var trimmedFullName = request.FullName.Trim();
        var trimmedPhone = request.Phone.Trim();
        var trimmedAddress = (request.Address ?? string.Empty).Trim();

        var filter = Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{nic.Trim()}$", "i"));
        var update = Builders<User>.Update
            .Set(u => u.FullName, trimmedFullName)
            .Set(u => u.Phone, trimmedPhone)
            .Set(u => u.Address, trimmedAddress)
            .Set("Address", trimmedAddress)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(filter, update);

        user.FullName = trimmedFullName;
        user.Phone = trimmedPhone;
        user.Address = trimmedAddress;
        user.UpdatedAt = now;

        return (true, "Profile updated successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Retrieves a prosumer profile by their National Identity Card (NIC) number.
    /// </summary>
    /// <param name="nic">The unique NIC number.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and sanitized user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> GetProsumerProfileAsync(string nic)
    {
        if (string.IsNullOrWhiteSpace(nic))
        {
            return (false, "NIC cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByNicAsync(nic);
        if (user == null)
        {
            return (false, $"Prosumer with NIC '{nic}' was not found.", StatusCodes.Status404NotFound, null);
        }

        return (true, "Profile retrieved successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Handles a prosumer request to self-deactivate their account.
    /// </summary>
    /// <param name="nic">The unique National Identity Card number of the requesting prosumer.</param>
    /// <param name="request">Optional deactivation reason and remarks payload.</param>
    /// <returns>A tuple with success status, message, HTTP status code, and updated user DTO.</returns>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> RequestProsumerDeactivationAsync(string nic, DeactivationRequestDto? request)
    {
        if (string.IsNullOrWhiteSpace(nic))
        {
            return (false, "NIC cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByNicAsync(nic);
        if (user == null)
        {
            return (false, $"Prosumer with NIC '{nic}' was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Your account is already deactivated.", StatusCodes.Status400BadRequest, null);
        }

        var now = DateTime.UtcNow;
        var filter = Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{nic.Trim()}$", "i"));
        var update = Builders<User>.Update
            .Set(u => u.Status, AccountStatus.Deactivated)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(filter, update);

        user.Status = AccountStatus.Deactivated;
        user.UpdatedAt = now;

        return (true, "Account self-deactivation request completed successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Retrieves an authenticated user's profile details by their database identifier or NIC.
    /// </summary>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> GetUserProfileAsync(string userId)
    {
        if (string.IsNullOrWhiteSpace(userId))
        {
            return (false, "User identifier cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(userId) ?? await GetUserByNicAsync(userId);
        if (user == null)
        {
            return (false, "User account was not found.", StatusCodes.Status404NotFound, null);
        }

        return (true, "User profile retrieved successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Updates permitted profile fields (FullName, Phone) for any authenticated user.
    /// Enforces strict immutability on NIC, Username, Email, and Role.
    /// </summary>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> UpdateUserProfileAsync(string userId, UserProfileUpdateDto request)
    {
        if (string.IsNullOrWhiteSpace(userId))
        {
            return (false, "User identifier cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(userId) ?? await GetUserByNicAsync(userId);
        if (user == null)
        {
            return (false, "User account was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Deactivated accounts cannot modify profile information.", StatusCodes.Status403Forbidden, null);
        }

        var now = DateTime.UtcNow;
        var trimmedFullName = request.FullName.Trim();
        var trimmedPhone = request.Phone.Trim();

        var filter = Builders<User>.Filter.Eq(u => u.Id, user.Id);
        var update = Builders<User>.Update
            .Set(u => u.FullName, trimmedFullName)
            .Set(u => u.Phone, trimmedPhone)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(filter, update);

        user.FullName = trimmedFullName;
        user.Phone = trimmedPhone;
        user.UpdatedAt = now;

        return (true, "Profile details updated successfully.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Changes an authenticated user's password following verification of their current password and complexity rules.
    /// </summary>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> ChangePasswordAsync(string userId, ChangePasswordDto request)
    {
        if (string.IsNullOrWhiteSpace(userId))
        {
            return (false, "User identifier cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(userId) ?? await GetUserByNicAsync(userId);
        if (user == null)
        {
            return (false, "User account was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Deactivated accounts cannot change password.", StatusCodes.Status403Forbidden, null);
        }

        // 1. Verify Current Password
        if (!BCrypt.Net.BCrypt.Verify(request.CurrentPassword, user.PasswordHash))
        {
            return (false, "The current password you entered is incorrect.", StatusCodes.Status400BadRequest, null);
        }

        // 2. Validate New Password Confirmation
        if (request.NewPassword != request.ConfirmNewPassword)
        {
            return (false, "New password and confirmation do not match.", StatusCodes.Status400BadRequest, null);
        }

        // 3. Verify Complexity
        if (!IsComplexPassword(request.NewPassword))
        {
            return (false, "New password must be at least 6 characters and contain at least one uppercase letter, one lowercase letter, one number, and one special character.", StatusCodes.Status400BadRequest, null);
        }

        // 4. Update Password Hash
        var newHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);
        var now = DateTime.UtcNow;

        var filter = Builders<User>.Filter.Eq(u => u.Id, user.Id);
        var update = Builders<User>.Update
            .Set(u => u.PasswordHash, newHash)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(filter, update);

        user.PasswordHash = newHash;
        user.UpdatedAt = now;

        return (true, "Password changed successfully. Please sign in with your new password.", StatusCodes.Status200OK, MapToDto(user));
    }

    /// <summary>
    /// Permanently deletes an authenticated user's account from MongoDB after validating that
    /// the provided confirmation email strictly matches the user's registered email address.
    /// </summary>
    public async Task<(bool Success, string Message, int StatusCode, UserResponseDto? Data)> DeleteAccountAsync(string userId, DeleteAccountDto request)
    {
        if (string.IsNullOrWhiteSpace(userId))
        {
            return (false, "User identifier cannot be empty.", StatusCodes.Status400BadRequest, null);
        }

        if (request == null || string.IsNullOrWhiteSpace(request.ConfirmEmail))
        {
            return (false, "Confirmation email address is required.", StatusCodes.Status400BadRequest, null);
        }

        var user = await GetUserByIdAsync(userId) ?? await GetUserByNicAsync(userId);
        if (user == null)
        {
            return (false, "User account was not found.", StatusCodes.Status404NotFound, null);
        }

        if (user.Status == AccountStatus.Deactivated)
        {
            return (false, "Deactivated accounts cannot delete account.", StatusCodes.Status403Forbidden, null);
        }

        var userEmail = user.Email ?? (user.ExtraElements != null && user.ExtraElements.Contains("email") && !user.ExtraElements["email"].IsBsonNull ? user.ExtraElements["email"].AsString : string.Empty);
        var inputEmail = request.ConfirmEmail.Trim();

        // Strictly verify that the typed email matches the registered account email
        if (!string.Equals(inputEmail, userEmail.Trim(), StringComparison.OrdinalIgnoreCase))
        {
            return (false, "The confirmation email you entered does not match your registered email address.", StatusCodes.Status400BadRequest, null);
        }

        // Permanently delete user document from MongoDB
        var filter = Builders<User>.Filter.Eq(u => u.Id, user.Id);
        var deleteResult = await _dbContext.Users.DeleteOneAsync(filter);

        if (deleteResult.DeletedCount == 0)
        {
            return (false, "Failed to delete user account. Please try again later.", StatusCodes.Status500InternalServerError, null);
        }

        return (true, "Your account has been deleted successfully.", StatusCodes.Status200OK, null);
    }

    /// <summary>
    /// Retrieves a user document by its MongoDB ObjectId.
    /// </summary>
    /// <param name="id">The unique document ID.</param>
    /// <returns>The user entity or null.</returns>
    public async Task<User?> GetUserByIdAsync(string id)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            return null;
        }

        return await _dbContext.Users.Find(u => u.Id == id).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Retrieves a user document by their NIC.
    /// </summary>
    /// <param name="nic">The unique NIC number.</param>
    /// <returns>The user entity or null.</returns>
    public async Task<User?> GetUserByNicAsync(string nic)
    {
        if (string.IsNullOrWhiteSpace(nic))
        {
            return null;
        }

        var filter = Builders<User>.Filter.Regex(u => u.Nic, new BsonRegularExpression($"^{nic.Trim()}$", "i"));
        return await _dbContext.Users.Find(filter).FirstOrDefaultAsync();
    }

    /// <summary>
    /// Helper method mapping a User entity to a sanitized UserResponseDto.
    /// </summary>
    /// <param name="user">The user entity to map.</param>
    /// <returns>The sanitized UserResponseDto.</returns>
    private static UserResponseDto MapToDto(User user)
    {
        var address = user.Address;
        if (string.IsNullOrWhiteSpace(address) && user.ExtraElements != null)
        {
            if (user.ExtraElements.Contains("Address") && !user.ExtraElements["Address"].IsBsonNull)
                address = user.ExtraElements["Address"].AsString;
            else if (user.ExtraElements.Contains("address") && !user.ExtraElements["address"].IsBsonNull)
                address = user.ExtraElements["address"].AsString;
            else if (user.ExtraElements.Contains("facilityAddress") && !user.ExtraElements["facilityAddress"].IsBsonNull)
                address = user.ExtraElements["facilityAddress"].AsString;
        }

        var lat = user.Latitude;
        var lon = user.Longitude;
        if (!lat.HasValue && user.ExtraElements != null)
        {
            if (user.ExtraElements.Contains("Latitude") && !user.ExtraElements["Latitude"].IsBsonNull)
                lat = user.ExtraElements["Latitude"].ToDouble();
            else if (user.ExtraElements.Contains("latitude") && !user.ExtraElements["latitude"].IsBsonNull)
                lat = user.ExtraElements["latitude"].ToDouble();
        }
        if (!lon.HasValue && user.ExtraElements != null)
        {
            if (user.ExtraElements.Contains("Longitude") && !user.ExtraElements["Longitude"].IsBsonNull)
                lon = user.ExtraElements["Longitude"].ToDouble();
            else if (user.ExtraElements.Contains("longitude") && !user.ExtraElements["longitude"].IsBsonNull)
                lon = user.ExtraElements["longitude"].ToDouble();
        }

        var email = user.Email;
        if (string.IsNullOrWhiteSpace(email) && user.ExtraElements != null)
        {
            if (user.ExtraElements.Contains("Email") && !user.ExtraElements["Email"].IsBsonNull)
                email = user.ExtraElements["Email"].AsString;
            else if (user.ExtraElements.Contains("email") && !user.ExtraElements["email"].IsBsonNull)
                email = user.ExtraElements["email"].AsString;
        }

        return new UserResponseDto
        {
            Id = user.Id ?? string.Empty,
            Nic = user.Nic,
            Username = user.Username,
            FullName = user.FullName,
            Phone = user.Phone,
            Email = email ?? string.Empty,
            Address = address ?? string.Empty,
            Latitude = lat,
            Longitude = lon,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt,
            UpdatedAt = user.UpdatedAt
        };
    }

    /// <summary>
    /// Validates password complexity: minimum 6 characters, at least 1 uppercase letter,
    /// at least 1 lowercase letter, at least 1 digit, and at least 1 special symbol.
    /// </summary>
    private static bool IsComplexPassword(string? password)
    {
        if (string.IsNullOrWhiteSpace(password) || password.Length < 6)
        {
            return false;
        }

        // Requires >= 1 uppercase, >= 1 lowercase, >= 1 digit, >= 1 special symbol
        var hasUpper = Regex.IsMatch(password, @"[A-Z]");
        var hasLower = Regex.IsMatch(password, @"[a-z]");
        var hasDigit = Regex.IsMatch(password, @"\d");
        var hasSymbol = Regex.IsMatch(password, @"[^a-zA-Z0-9]");

        return hasUpper && hasLower && hasDigit && hasSymbol;
    }
}
