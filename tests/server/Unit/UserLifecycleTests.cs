/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Unit test suite for prosumer registration, staff provisioning, activation, and account deletion workflows.
 */

using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Moq;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;
using SmartSolarMicrogrid.Api.Tests.TestUtilities;
using Xunit;

namespace SmartSolarMicrogrid.Api.Tests.Unit;

/// <summary>
/// Unit tests verifying user registration uniqueness, lifecycle status transitions, administrative approvals, and account deletion.
/// </summary>
public class UserLifecycleTests
{
    private readonly Mock<ITokenService> _tokenServiceMock;
    private readonly Mock<IEmailService> _emailServiceMock;

    public UserLifecycleTests()
    {
        _tokenServiceMock = new Mock<ITokenService>();
        _emailServiceMock = new Mock<IEmailService>();
    }

    [Fact]
    public async Task RegisterProsumer_DuplicateNic_ReturnsConflict409()
    {
        // Arrange
        var existingUser = TestDbHelper.CreateSampleProsumer(nic: "199512345678", username: "john_doe", email: "john@microgrid.lk");
        var userList = new List<User> { existingUser };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var registerDto = new ProsumerRegisterDto
        {
            Nic = "199512345678", // Duplicate NIC
            Username = "unique_user",
            Email = "unique@microgrid.lk",
            Password = "SecurePassword123!",
            FullName = "New Prosumer",
            Phone = "0771234567",
            Address = "No 10, Colombo",
            Latitude = 6.9271,
            Longitude = 79.8612
        };

        // Act
        var result = await userService.RegisterProsumerAsync(registerDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status409Conflict);
        result.Message.Should().Contain("NIC");
    }

    [Fact]
    public async Task RegisterProsumer_DuplicateEmail_ReturnsConflict409()
    {
        // Arrange
        var existingUser = TestDbHelper.CreateSampleProsumer(nic: "199011223344", username: "existing_user", email: "duplicate@microgrid.lk");
        var userList = new List<User> { existingUser };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var registerDto = new ProsumerRegisterDto
        {
            Nic = "200012345678",
            Username = "new_unique_user",
            Email = "duplicate@microgrid.lk", // Duplicate Email
            Password = "SecurePassword123!",
            FullName = "New Prosumer",
            Phone = "0771234567",
            Address = "No 10, Colombo",
            Latitude = 6.9271,
            Longitude = 79.8612
        };

        // Act
        var result = await userService.RegisterProsumerAsync(registerDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status409Conflict);
        result.Message.Should().Contain("email");
    }

    [Fact]
    public async Task CreateStaffUser_DuplicateEmailOrUsername_ReturnsConflict()
    {
        // Arrange
        var existingStaff = TestDbHelper.CreateSampleBackofficeOfficer(username: "staff_admin", email: "admin@microgrid.lk");
        var userList = new List<User> { existingStaff };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var createStaffDto = new UserCreateDto
        {
            Nic = "198811223344",
            Username = "staff_admin", // Duplicate Username
            Email = "new_email@microgrid.lk",
            Password = "AdminPassword123!",
            FullName = "Assistant Admin",
            Phone = "0771234567",
            Role = UserRole.Backoffice
        };

        // Act
        var result = await userService.CreateStaffUserAsync(createStaffDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status409Conflict);
    }

    [Fact]
    public async Task GetPendingProsumers_ReturnsOnlyPendingStatusUsers()
    {
        // Arrange
        var pendingUser1 = TestDbHelper.CreateSampleProsumer(nic: "199500000001", username: "p1", status: AccountStatus.PendingActivation);
        var pendingUser2 = TestDbHelper.CreateSampleProsumer(nic: "199500000002", username: "p2", status: AccountStatus.PendingActivation);
        var activeUser = TestDbHelper.CreateSampleProsumer(nic: "199500000003", username: "active1", status: AccountStatus.Active);

        var userList = new List<User> { pendingUser1, pendingUser2, activeUser };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        // Act
        var result = await userService.GetPendingProsumersAsync();

        // Assert
        result.Should().NotBeNull();
        result.Count.Should().Be(2);
    }

    [Fact]
    public async Task ApproveProsumer_TransitionsStatusToActive()
    {
        // Arrange
        var pendingUser = TestDbHelper.CreateSampleProsumer(
            nic: "199500000001",
            username: "pending_john",
            email: "pending@microgrid.lk",
            status: AccountStatus.PendingActivation);

        var userList = new List<User> { pendingUser };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        // Act
        var result = await userService.UpdateUserStatusAsync(
            pendingUser.Id,
            AccountStatus.Active,
            "All facility coordinates verified.");

        // Assert
        result.Success.Should().BeTrue();
        result.StatusCode.Should().Be(StatusCodes.Status200OK);
        result.Data.Should().NotBeNull();
        result.Data!.Status.Should().Be(AccountStatus.Active);
    }

    [Fact]
    public async Task DeleteAccount_WithMatchingEmail_PermanentlyDeletesUser()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(email: "delete_me@microgrid.lk");
        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var deleteDto = new DeleteAccountDto
        {
            ConfirmEmail = "delete_me@microgrid.lk"
        };

        // Act
        var result = await userService.DeleteAccountAsync(user.Id, deleteDto);

        // Assert
        result.Success.Should().BeTrue();
        result.StatusCode.Should().Be(StatusCodes.Status200OK);
        result.Message.Should().Contain("deleted successfully");
    }

    [Fact]
    public async Task DeleteAccount_WithMismatchedEmail_ReturnsBadRequest()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleProsumer(email: "registered_email@microgrid.lk");
        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var deleteDto = new DeleteAccountDto
        {
            ConfirmEmail = "wrong_email@microgrid.lk"
        };

        // Act
        var result = await userService.DeleteAccountAsync(user.Id, deleteDto);

        // Assert
        result.Success.Should().BeFalse();
        result.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
        result.Message.Should().Contain("does not match");
    }

    [Fact]
    public async Task UpdateProfile_WithValidData_UpdatesFullNameAndPhone()
    {
        // Arrange
        var user = TestDbHelper.CreateSampleBackofficeOfficer(
            nic: "199011223344",
            rawPassword: "AdminPassword123!");

        var userList = new List<User> { user };
        var (mockContext, _) = TestDbHelper.CreateMockDbContext(userList);
        var userService = new UserService(mockContext.Object, _tokenServiceMock.Object, _emailServiceMock.Object);

        var updateDto = new UserProfileUpdateDto
        {
            FullName = "Updated Senior Admin",
            Phone = "0778889999"
        };

        // Act
        var result = await userService.UpdateUserProfileAsync(user.Id, updateDto);

        // Assert
        result.Success.Should().BeTrue();
        result.StatusCode.Should().Be(StatusCodes.Status200OK);
        result.Data.Should().NotBeNull();
        result.Data!.FullName.Should().Be("Updated Senior Admin");
        result.Data.Phone.Should().Be("0778889999");
    }
}
