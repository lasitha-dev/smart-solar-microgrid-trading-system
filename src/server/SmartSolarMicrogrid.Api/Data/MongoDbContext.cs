/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: MongoDB database context configuring client connection and collections using MongoDB.Driver 3.1.0.
 */

using Microsoft.Extensions.Options;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Data;

/// <summary>
/// Provides access to the MongoDB database and collections required by the Smart Solar Microgrid Trading System.
/// </summary>
public class MongoDbContext
{
    private readonly IMongoDatabase _database;
    private readonly MongoDbSettings _settings;

    /// <summary>
    /// Initializes a new instance of the <see cref="MongoDbContext"/> class using injected MongoDB settings.
    /// </summary>
    /// <param name="options">The strongly-typed MongoDB configuration settings.</param>
    /// <exception cref="ArgumentNullException">Thrown when connection settings or database name is missing.</exception>
    public MongoDbContext(IOptions<MongoDbSettings> options)
    {
        _settings = options.Value ?? throw new ArgumentNullException(nameof(options), "MongoDB settings must be provided.");

        if (string.IsNullOrWhiteSpace(_settings.ConnectionString))
        {
            throw new ArgumentException("MongoDB ConnectionString cannot be empty.", nameof(options));
        }

        var clientSettings = MongoClientSettings.FromConnectionString(_settings.ConnectionString);
        var client = new MongoClient(clientSettings);
        _database = client.GetDatabase(_settings.DatabaseName);

        EnsureIndexesCreated();
    }

    /// <summary>
    /// Gets the underlying MongoDB database instance.
    /// </summary>
    public IMongoDatabase Database => _database;

    /// <summary>
    /// Gets the collection accessor for user entities mapped to "User's Detail".
    /// </summary>
    public IMongoCollection<User> Users =>
        _database.GetCollection<User>(_settings.UsersCollectionName);

    /// <summary>
    /// Ensures required unique indexes on NIC and Username fields exist in MongoDB.
    /// </summary>
    private void EnsureIndexesCreated()
    {
        try
        {
            var usersCollection = Users;

            // Unique index for NIC
            var nicIndexKeys = Builders<User>.IndexKeys.Ascending(u => u.Nic);
            var nicIndexOptions = new CreateIndexOptions { Unique = true, Name = "ux_users_nic" };
            var nicIndexModel = new CreateIndexModel<User>(nicIndexKeys, nicIndexOptions);

            // Unique index for Username
            var usernameIndexKeys = Builders<User>.IndexKeys.Ascending(u => u.Username);
            var usernameIndexOptions = new CreateIndexOptions { Unique = true, Name = "ux_users_username" };
            var usernameIndexModel = new CreateIndexModel<User>(usernameIndexKeys, usernameIndexOptions);

            usersCollection.Indexes.CreateMany([nicIndexModel, usernameIndexModel]);
        }
        catch
        {
            // Index creation failure will not block initialization
        }
    }
}
