/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Unit test suite for authentication workflows, password hashing, and login security rules.
 */

using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Moq;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;
using SmartSolarMicrogrid.Api.Tests.TestUtilities;
using Xunit;

namespace SmartSolarMicrogrid.Api.Tests.Unit;

/// <summary>
/// Unit tests verifying core authentication logic, JWT token issuance, password validation, and status guards.
/// </summary>
public class AuthServiceTests
{
    private readonly Mock<ITokenService> _tokenServiceMock;
    private readonly Mock<IEmailService> _emailServiceMock;

    public AuthServiceTests()
    {
        _tokenServiceMock = new Mock<ITokenService>();
        _emailServiceMock = new Mock<IEmailService>();

        // Default mock token generator returning a mock JWT tuple
        _tokenServiceMock.Setup(t => t.GenerateToken(It.IsAny<User>()))
            .Returns(("mock_jwt_access_token_12345", DateTime.UtcNow.AddHours(2)));
    }

    [Fact]
    public async Task Login_WithValidCredentials_ReturnsSuccessAndJwtTokenWithClaims()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(
            nic: "199512345678",
            username: "prosumer_john",
            rawPassword: "Password123!",
            status: AccountStatus.Active);

        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var loginDto = new LoginRequestDto
        {
            Identifier = "prosumer_john",
            Password = "Password123!"
        };

        // Act
        var result = await userService.AuthenticateAsync(loginDto);

        // Assert
        result.Success.Should().BeTrue();
        result.StatusCode.Should().Be(StatusCodes.Status200OK);
        result.Data.Should().NotBeNull();
        result.Data!.Token.Should().Be("mock_jwt_access_token_12345");
        result.Data.Nic.Should().Be("199512345678");
        result.Data.Username.Should().Be("prosumer_john");
        result.Data.Role.Should().Be(UserRole.Prosumer);
    }

    [Fact]
    public async Task Login_WithInvalidPassword_ReturnsUnauthorized()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(rawPassword: "CorrectPassword123!");
        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var loginDto = new LoginRequestDto
        {
            Identifier = user.Username,
            Password = "WrongPassword999!"
        };

        // Act
        var result = await userService.AuthenticateAsync(loginDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status401Unauthorized);
        result.Data.Should().BeNull();
        result.Message.Should().Contain("Invalid");
    }

    [Fact]
    public async Task Login_WithPendingActivationStatus_ReturnsForbidden()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(
            rawPassword: "Password123!",
            status: AccountStatus.PendingActivation);

        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var loginDto = new LoginRequestDto
        {
            Identifier = user.Nic,
            Password = "Password123!"
        };

        // Act
        var result = await userService.AuthenticateAsync(loginDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        result.Data.Should().BeNull();
        result.Message.Should().Contain("pending activation");
    }

    [Fact]
    public async Task Login_WithDeactivatedStatus_ReturnsForbidden()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(
            rawPassword: "Password123!",
            status: AccountStatus.Deactivated);

        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var loginDto = new LoginRequestDto
        {
            Identifier = user.Username,
            Password = "Password123!"
        };

        // Act
        var result = await userService.AuthenticateAsync(loginDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status403Forbidden);
        result.Data.Should().BeNull();
        result.Message.Should().Contain("deactivated");
    }

    [Fact]
    public void PasswordHashing_Verification_NeverStoresRawPassword()
    {
        // Arrange
        var rawPassword = "SecurePass123!";

        // Act
        var hash = PasswordHasher.HashPassword(rawPassword);

        // Assert
        hash.Should().NotBeNullOrEmpty();
        hash.Should().NotBe(rawPassword);
        hash.Should().StartWith("$2"); // Standard BCrypt identifier prefix

        // Verify verification passes with original password
        PasswordHasher.VerifyPassword(rawPassword, hash).Should().BeTrue();

        // Verify verification fails with incorrect password
        PasswordHasher.VerifyPassword("WrongPassword123!", hash).Should().BeFalse();
    }

    [Fact]
    public async Task ChangePassword_WithValidCredentials_UpdatesHashAndSucceeds()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(rawPassword: "OldPassword123!");
        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var changePasswordDto = new ChangePasswordDto
        {
            CurrentPassword = "OldPassword123!",
            NewPassword = "NewPassword123!@#",
            ConfirmNewPassword = "NewPassword123!@#"
        };

        // Act
        var result = await userService.ChangePasswordAsync(user.Id, changePasswordDto);

        // Assert
        result.Success.Should().BeTrue();
        result.StatusCode.Should().Be(StatusCodes.Status200OK);
        result.Message.Should().Contain("successfully");

        // Verify that the new password hash verifies correctly with the new password
        PasswordHasher.VerifyPassword("NewPassword123!@#", user.PasswordHash).Should().BeTrue();
    }

    [Fact]
    public async Task ChangePassword_WithInvalidCurrentPassword_Fails()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(rawPassword: "OldPassword123!");
        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var changePasswordDto = new ChangePasswordDto
        {
            CurrentPassword = "IncorrectCurrentPassword123!",
            NewPassword = "NewPassword123!@#",
            ConfirmNewPassword = "NewPassword123!@#"
        };

        // Act
        var result = await userService.ChangePasswordAsync(user.Id, changePasswordDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
        result.Message.Should().Contain("incorrect");
    }
}
