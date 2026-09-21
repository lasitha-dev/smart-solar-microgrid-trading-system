/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Strongly-typed configuration options for MongoDB database connection.
 */

namespace SmartSolarMicrogrid.Api.Configuration;

/// <summary>
/// Represents configuration settings for connecting to the MongoDB database cluster and collections.
/// </summary>
public class MongoDbSettings
{
    /// <summary>
    /// Configuration section key within appsettings.json.
    /// </summary>
    public const string SectionName = "MongoDbSettings";

    /// <summary>
    /// Gets or sets the MongoDB Atlas or local connection string URI.
    /// </summary>
    public string ConnectionString { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the target database name.
    /// </summary>
    public string DatabaseName { get; set; } = "SmartSolarMicrogridDb";

    /// <summary>
    /// Gets or sets the name of the user detail collection.
    /// </summary>
    public string UsersCollectionName { get; set; } = "User's Detail";
}
