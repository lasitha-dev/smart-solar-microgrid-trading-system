/*
 * Student Name: SILVA M N U (IT22169112) & A.L.M Athulathmudali (IT21129544)
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Central MongoDB Context for Users and Energy Reservations
 * Description: MongoDB database context configuring client connection, collections, and unique indexes.
 */

using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Options;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Data;

/// <summary>
/// Interface defining the database context and accessible MongoDB collections.
/// </summary>
public interface IMongoDbContext
{
    IMongoCollection<User> Users { get; }
    IMongoCollection<EnergyReservation> EnergyReservations { get; }
    IMongoDatabase Database { get; }
}

/// <summary>
/// Provides access to the MongoDB database and collections required by the Smart Solar Microgrid Trading System.
/// </summary>
public class MongoDbContext : IMongoDbContext
{
    private readonly IMongoDatabase _database;
    private readonly MongoDbSettings _settings;

    /// <summary>
    /// Parameterless constructor for unit testing and mocking.
    /// </summary>
    public MongoDbContext()
    {
        _database = null!;
        _settings = new MongoDbSettings();
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="MongoDbContext"/> class using injected MongoDB settings.
    /// </summary>
    /// <param name="options">The strongly-typed MongoDB configuration settings.</param>
    /// <param name="configuration">Optional application configuration fallback.</param>
    public MongoDbContext(IOptions<MongoDbSettings> options, IConfiguration? configuration = null)
    {
        _settings = options.Value ?? new MongoDbSettings();

        var connectionString = !string.IsNullOrWhiteSpace(_settings.ConnectionString)
            ? _settings.ConnectionString
            : configuration?["DatabaseSettings:ConnectionString"] ?? "mongodb://localhost:27017";

        var databaseName = !string.IsNullOrWhiteSpace(_settings.DatabaseName)
            ? _settings.DatabaseName
            : configuration?["DatabaseSettings:DatabaseName"] ?? "SmartSolarMicrogridDb";

        var clientSettings = MongoClientSettings.FromConnectionString(connectionString);
        var client = new MongoClient(clientSettings);
        _database = client.GetDatabase(databaseName);

        EnsureIndexesCreated();
    }

    /// <summary>
    /// Gets the underlying MongoDB database instance.
    /// </summary>
    public virtual IMongoDatabase Database => _database;

    /// <summary>
    /// Gets the collection accessor for user entities mapped to "User's Detail".
    /// </summary>
    public virtual IMongoCollection<User> Users =>
        _database.GetCollection<User>(_settings.UsersCollectionName.IfBlank("User's Detail"));

    /// <summary>
    /// Gets the collection accessor for energy reservations.
    /// </summary>
    public virtual IMongoCollection<EnergyReservation> EnergyReservations =>
        _database.GetCollection<EnergyReservation>("EnergyReservation");

    /// <summary>
    /// Ensures required unique indexes on NIC, Username, and Email fields exist in MongoDB.
    /// </summary>
    private void EnsureIndexesCreated()
    {
        try
        {
            if (_database == null) return;

            var usersCollection = Users;

            // Unique index for NIC
            var nicIndexKeys = Builders<User>.IndexKeys.Ascending(u => u.Nic);
            var nicIndexOptions = new CreateIndexOptions { Unique = true, Name = "ux_users_nic" };
            var nicIndexModel = new CreateIndexModel<User>(nicIndexKeys, nicIndexOptions);

            // Unique index for Username
            var usernameIndexKeys = Builders<User>.IndexKeys.Ascending(u => u.Username);
            var usernameIndexOptions = new CreateIndexOptions { Unique = true, Name = "ux_users_username" };
            var usernameIndexModel = new CreateIndexModel<User>(usernameIndexKeys, usernameIndexOptions);

            // Unique sparse index for Email
            var emailIndexKeys = Builders<User>.IndexKeys.Ascending(u => u.Email);
            var emailIndexOptions = new CreateIndexOptions { Unique = true, Sparse = true, Name = "ux_users_email" };
            var emailIndexModel = new CreateIndexModel<User>(emailIndexKeys, emailIndexOptions);

            usersCollection.Indexes.CreateMany([nicIndexModel, usernameIndexModel, emailIndexModel]);
        }
        catch
        {
            // Index creation failure will not block initialization
        }
    }
}

internal static class StringExtensions
{
    public static string IfBlank(this string? source, string fallback) =>
        string.IsNullOrWhiteSpace(source) ? fallback : source;
}
