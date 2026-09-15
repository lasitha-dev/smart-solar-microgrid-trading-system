// Description: Central application entry point and ASP.NET Core service configuration.

using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.Repositories;
using SmartSolarMicrogrid.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Add controllers support for REST API endpoints
builder.Services.AddControllers();

// Configure QR Security options from appsettings
builder.Services.Configure<QrSecurityOptions>(
    builder.Configuration.GetSection(QrSecurityOptions.SectionName));

// Register Data Access and Repositories
builder.Services.AddSingleton<IMongoDbContext, MongoDbContext>();
builder.Services.AddScoped<IReservationRepository, ReservationRepository>();

// Register Domain Services
builder.Services.AddSingleton<IQrSignatureService, QrSignatureService>();
builder.Services.AddScoped<IOperatorVerificationService, OperatorVerificationService>();

var app = builder.Build();

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.Run();
