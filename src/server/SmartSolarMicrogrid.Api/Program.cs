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
builder.Services.AddScoped<IDashboardQueryService, DashboardQueryService>();

var app = builder.Build();

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

// Root discovery and health status endpoint
app.MapGet("/", () => Results.Ok(new
{
    service = "Smart Solar Microgrid Trading System - Central Web API",
    status = "Online",
    version = "1.0.0",
    description = "C# Web API for Operator Verification, QR Validation, and Operational Dashboard",
    endpoints = new[]
    {
        "/api/reservations/dashboard-metrics",
        "/api/reservations",
        "/api/reservations/verify-qr",
        "/api/reservations/{id}/finalize"
    }
}));

app.Run();
