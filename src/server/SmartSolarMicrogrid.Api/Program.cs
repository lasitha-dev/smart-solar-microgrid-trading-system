// Description: Central application entry point and ASP.NET Core service configuration.

using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Add controllers support for REST API endpoints
builder.Services.AddControllers();

// Configure QR Security options from appsettings
builder.Services.Configure<QrSecurityOptions>(
    builder.Configuration.GetSection(QrSecurityOptions.SectionName));

// Register Cryptographic QR Signature Service as Singleton
builder.Services.AddSingleton<IQrSignatureService, QrSignatureService>();

var app = builder.Build();

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.Run();
