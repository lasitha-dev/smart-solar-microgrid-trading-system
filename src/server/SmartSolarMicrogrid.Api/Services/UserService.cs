/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Core user service implementing authentication, prosumer registration, staff provisioning, and lifecycle transitions.
 */

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

    /// <summary>
    /// Initializes a new instance of the <see cref="UserService"/> class.
    /// </summary>
    /// <param name="dbContext">The MongoDB database context.</param>
    /// <param name="tokenService">The JWT token generation service.</param>
    /// <exception cref="ArgumentNullException">Thrown when required dependencies are null.</exception>
    public UserService(MongoDbContext dbContext, ITokenService tokenService)
    {
        _dbContext = dbContext ?? throw new ArgumentNullException(nameof(dbContext));
        _tokenService = tokenService ?? throw new ArgumentNullException(nameof(tokenService));
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

        // Create new User entity with PendingActivation status
        var now = DateTime.UtcNow;
        var newUser = new User
        {
            Nic = trimmedNic,
            Username = trimmedUsername,
            PasswordHash = PasswordHasher.HashPassword(request.Password),
            FullName = request.FullName.Trim(),
            Phone = request.Phone.Trim(),
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

        var now = DateTime.UtcNow;
        var newUser = new User
        {
            Nic = trimmedNic,
            Username = trimmedUsername,
            PasswordHash = PasswordHasher.HashPassword(request.Password),
            FullName = request.FullName.Trim(),
            Phone = request.Phone.Trim(),
            Role = request.Role,
            Status = AccountStatus.Active,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _dbContext.Users.InsertOneAsync(newUser);

        var responseDto = MapToDto(newUser);
        return (true, $"{request.Role} user '{newUser.Username}' created successfully.", StatusCodes.Status201Created, responseDto);
    }

    /// <summary>
    /// Retrieves all system users with optional filtering by role and status.
    /// </summary>
    /// <param name="role">Optional filter by user role.</param>
    /// <param name="status">Optional filter by account status.</param>
    /// <returns>A list of sanitized user response DTOs.</returns>
    public async Task<List<UserResponseDto>> GetAllUsersAsync(UserRole? role = null, AccountStatus? status = null)
    {
        var filterBuilder = Builders<User>.Filter;
        var filters = new List<FilterDefinition<User>>();

        if (role.HasValue)
        {
            filters.Add(filterBuilder.Eq(u => u.Role, role.Value));
        }

        if (status.HasValue)
        {
            filters.Add(filterBuilder.Eq(u => u.Status, status.Value));
        }

        var filter = filters.Count > 0 ? filterBuilder.And(filters) : filterBuilder.Empty;

        var users = await _dbContext.Users
            .Find(filter)
            .SortByDescending(u => u.CreatedAt)
            .ToListAsync();

        return users.Select(MapToDto).ToList();
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
    /// Updates permitted profile fields (FullName, Phone) for a solar prosumer identified by NIC.
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

        var now = DateTime.UtcNow;
        var trimmedFullName = request.FullName.Trim();
        var trimmedPhone = request.Phone.Trim();

        var update = Builders<User>.Update
            .Set(u => u.FullName, trimmedFullName)
            .Set(u => u.Phone, trimmedPhone)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(u => u.Id == user.Id, update);

        user.FullName = trimmedFullName;
        user.Phone = trimmedPhone;
        user.UpdatedAt = now;

        return (true, "Profile updated successfully.", StatusCodes.Status200OK, MapToDto(user));
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
        var update = Builders<User>.Update
            .Set(u => u.Status, AccountStatus.Deactivated)
            .Set(u => u.UpdatedAt, now);

        await _dbContext.Users.UpdateOneAsync(u => u.Id == user.Id, update);

        user.Status = AccountStatus.Deactivated;
        user.UpdatedAt = now;

        return (true, "Account self-deactivation request completed successfully.", StatusCodes.Status200OK, MapToDto(user));
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
    private static UserResponseDto MapToDto(User user) => new()
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
}
