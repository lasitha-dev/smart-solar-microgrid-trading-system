// Description: MongoDB database context establishing connection and exposing the EnergyReservations collection.

using Microsoft.Extensions.Configuration;
using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Data;

/// <summary>
/// Description: Interface defining the database context and accessible MongoDB collections.
/// </summary>
public interface IMongoDbContext
{
    IMongoCollection<EnergyReservation> EnergyReservations { get; }
}

/// <summary>
/// Description: MongoDB database context implementation reading connection settings from configuration.
/// </summary>
public class MongoDbContext : IMongoDbContext
{
    private readonly IMongoDatabase _database;

    public MongoDbContext(IConfiguration configuration)
    {
        var connectionString = configuration["DatabaseSettings:ConnectionString"] ?? "mongodb://localhost:27017";
        var databaseName = configuration["DatabaseSettings:DatabaseName"] ?? "SmartSolarMicrogridDb";

        var client = new MongoClient(connectionString);
        _database = client.GetDatabase(databaseName);
    }

    public IMongoCollection<EnergyReservation> EnergyReservations =>
        _database.GetCollection<EnergyReservation>("EnergyReservation");
}
