/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Test fixture helper configuring Moq dependencies, mock Mongo collections, and mock services.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Bson.Serialization;
using MongoDB.Driver;
using Moq;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Models.Enums;
using SmartSolarMicrogrid.Api.Services;

namespace SmartSolarMicrogrid.Api.Tests.TestUtilities;

/// <summary>
/// Provides factory methods and mock setups for testing <see cref="UserService"/> and related components.
/// </summary>
public static class TestDbHelper
{
    /// <summary>
    /// Creates a mock <see cref="MongoDbContext"/> backed by an in-memory list of <see cref="User"/> records.
    /// </summary>
    /// <param name="userList">Initial list of users in the in-memory store.</param>
    /// <returns>A tuple containing the mock DbContext and the mock Users collection.</returns>
    public static (Mock<MongoDbContext> MockContext, Mock<IMongoCollection<User>> MockCollection) CreateMockDbContext(List<User> userList)
    {
        var mockCollection = new Mock<IMongoCollection<User>>();
        var mockContext = new Mock<MongoDbContext>();

        // Mock FindAsync with FilterDefinition
        mockCollection.Setup(c => c.FindAsync(
                It.IsAny<FilterDefinition<User>>(),
                It.IsAny<FindOptions<User, User>>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync((FilterDefinition<User> filter, FindOptions<User, User> options, CancellationToken ct) =>
            {
                var serializerRegistry = BsonSerializer.SerializerRegistry;
                var documentSerializer = serializerRegistry.GetSerializer<User>();
                var rendered = filter.Render(new MongoDB.Driver.RenderArgs<User>(documentSerializer, serializerRegistry));

                var matched = userList.Where(u => MatchesUser(u, rendered)).ToList();
                return new MockAsyncCursor<User>(matched);
            });

        // Mock InsertOneAsync
        mockCollection.Setup(c => c.InsertOneAsync(
                It.IsAny<User>(),
                It.IsAny<InsertOneOptions>(),
                It.IsAny<CancellationToken>()))
            .Callback<User, InsertOneOptions, CancellationToken>((user, opt, ct) =>
            {
                if (string.IsNullOrEmpty(user.Id))
                {
                    user.Id = Guid.NewGuid().ToString("N")[..24];
                }
                userList.Add(user);
            })
            .Returns(Task.CompletedTask);

        // Mock UpdateOneAsync
        mockCollection.Setup(c => c.UpdateOneAsync(
                It.IsAny<FilterDefinition<User>>(),
                It.IsAny<UpdateDefinition<User>>(),
                It.IsAny<UpdateOptions>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(new UpdateResult.Acknowledged(1, 1, null));

        // Mock DeleteOneAsync
        mockCollection.Setup(c => c.DeleteOneAsync(
                It.IsAny<FilterDefinition<User>>(),
                It.IsAny<CancellationToken>()))
            .Callback<FilterDefinition<User>, CancellationToken>((filter, ct) =>
            {
                var serializerRegistry = BsonSerializer.SerializerRegistry;
                var documentSerializer = serializerRegistry.GetSerializer<User>();
                var rendered = filter.Render(new MongoDB.Driver.RenderArgs<User>(documentSerializer, serializerRegistry));
                userList.RemoveAll(u => MatchesUser(u, rendered));
            })
            .ReturnsAsync(new DeleteResult.Acknowledged(1));

        mockContext.Setup(ctx => ctx.Users).Returns(mockCollection.Object);

        return (mockContext, mockCollection);
    }

    private static bool MatchesUser(User u, BsonDocument doc)
    {
        if (doc.ElementCount == 0) return true;

        if (doc.Contains("$or"))
        {
            var orArray = doc["$or"].AsBsonArray;
            return orArray.Any(subDoc => MatchesUser(u, subDoc.AsBsonDocument));
        }

        if (doc.Contains("$and"))
        {
            var andArray = doc["$and"].AsBsonArray;
            return andArray.All(subDoc => MatchesUser(u, subDoc.AsBsonDocument));
        }

        foreach (var element in doc)
        {
            var name = element.Name;
            var val = element.Value;

            if (string.Equals(name, "_id", StringComparison.OrdinalIgnoreCase) || string.Equals(name, "id", StringComparison.OrdinalIgnoreCase))
            {
                var targetId = val.IsObjectId ? val.AsObjectId.ToString() : (val.IsString ? val.AsString : val.ToString());
                if (!string.Equals(u.Id, targetId, StringComparison.OrdinalIgnoreCase)) return false;
            }
            else if (string.Equals(name, "nic", StringComparison.OrdinalIgnoreCase))
            {
                if (!MatchStringOrRegex(u.Nic, val)) return false;
            }
            else if (string.Equals(name, "username", StringComparison.OrdinalIgnoreCase))
            {
                if (!MatchStringOrRegex(u.Username, val)) return false;
            }
            else if (string.Equals(name, "email", StringComparison.OrdinalIgnoreCase))
            {
                if (!MatchStringOrRegex(u.Email, val)) return false;
            }
            else if (string.Equals(name, "status", StringComparison.OrdinalIgnoreCase))
            {
                var statusVal = val.IsString ? val.AsString : val.ToString();
                if (!string.Equals(u.Status.ToString(), statusVal, StringComparison.OrdinalIgnoreCase) &&
                    !string.Equals(((int)u.Status).ToString(), statusVal, StringComparison.OrdinalIgnoreCase))
                    return false;
            }
            else if (string.Equals(name, "role", StringComparison.OrdinalIgnoreCase))
            {
                var roleVal = val.IsString ? val.AsString : val.ToString();
                if (!string.Equals(u.Role.ToString(), roleVal, StringComparison.OrdinalIgnoreCase) &&
                    !string.Equals(((int)u.Role).ToString(), roleVal, StringComparison.OrdinalIgnoreCase))
                    return false;
            }
        }

        return true;
    }

    private static bool MatchStringOrRegex(string actual, BsonValue filterValue)
    {
        if (filterValue.IsBsonRegularExpression)
        {
            var regex = filterValue.AsBsonRegularExpression;
            return Regex.IsMatch(actual ?? string.Empty, regex.Pattern, RegexOptions.IgnoreCase);
        }

        if (filterValue.IsBsonDocument)
        {
            var doc = filterValue.AsBsonDocument;
            if (doc.Contains("$regularExpression"))
            {
                var regDoc = doc["$regularExpression"].AsBsonDocument;
                var pattern = regDoc["pattern"].AsString;
                return Regex.IsMatch(actual ?? string.Empty, pattern, RegexOptions.IgnoreCase);
            }
            if (doc.Contains("$regex"))
            {
                var pattern = doc["$regex"].IsBsonRegularExpression ? doc["$regex"].AsBsonRegularExpression.Pattern : doc["$regex"].AsString;
                return Regex.IsMatch(actual ?? string.Empty, pattern, RegexOptions.IgnoreCase);
            }
            if (doc.Contains("$eq"))
            {
                var eqVal = doc["$eq"].IsString ? doc["$eq"].AsString : doc["$eq"].ToString();
                return string.Equals(actual, eqVal, StringComparison.OrdinalIgnoreCase);
            }
        }

        var strVal = filterValue.IsString ? filterValue.AsString : filterValue.ToString();
        return string.Equals(actual, strVal, StringComparison.OrdinalIgnoreCase);
    }

    /// <summary>
    /// Creates a sample active prosumer entity.
    /// </summary>
    public static User CreateSampleProsumer(
        string nic = "199512345678",
        string username = "prosumer_john",
        string email = "john@microgrid.lk",
        string rawPassword = "Password123!",
        AccountStatus status = AccountStatus.Active)
    {
        return new User
        {
            Id = Guid.NewGuid().ToString("N")[..24],
            Nic = nic,
            Username = username,
            Email = email,
            FullName = "John Silva",
            Phone = "0771234567",
            Address = "No 10, Colombo",
            Latitude = 6.9271,
            Longitude = 79.8612,
            Role = UserRole.Prosumer,
            Status = status,
            PasswordHash = PasswordHasher.HashPassword(rawPassword),
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
    }

    /// <summary>
    /// Creates a sample backoffice officer entity.
    /// </summary>
    public static User CreateSampleBackofficeOfficer(
        string nic = "199011223344",
        string username = "admin_officer",
        string email = "admin@microgrid.lk",
        string rawPassword = "AdminPassword123!",
        AccountStatus status = AccountStatus.Active)
    {
        return new User
        {
            Id = Guid.NewGuid().ToString("N")[..24],
            Nic = nic,
            Username = username,
            Email = email,
            FullName = "System Admin",
            Phone = "0779998888",
            Role = UserRole.Backoffice,
            Status = status,
            PasswordHash = PasswordHasher.HashPassword(rawPassword),
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
    }

    /// <summary>
    /// Creates a sample Grid Operator entity.
    /// </summary>
    public static User CreateSampleGridOperator(
        string nic = "199211334455",
        string username = "grid_operator_1",
        string email = "operator@microgrid.lk",
        string rawPassword = "GridPassword123!",
        AccountStatus status = AccountStatus.Active)
    {
        return new User
        {
            Id = Guid.NewGuid().ToString("N")[..24],
            Nic = nic,
            Username = username,
            Email = email,
            FullName = "Grid Operator Silva",
            Phone = "0715556677",
            Role = UserRole.GridOperator,
            Status = status,
            PasswordHash = PasswordHasher.HashPassword(rawPassword),
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
    }
}
