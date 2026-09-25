/*
 * Student Name: SILVA M N U (IT22169112) & A.L.M Athulathmudali (IT21129544)
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Smart Solar Microgrid Trading System - Unified Central C# Web API
 * Description: Main application entry point configuring DI, JWT auth, MongoDB, Swagger, and route pipeline.
 */

using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.Repositories;
using SmartSolarMicrogrid.Api.Services;

// ==========================================
// 0. Load Local .env Configuration File
// ==========================================
DotEnvLoader.Load();

var builder = WebApplication.CreateBuilder(args);

// ==========================================
// 1. Strongly-Typed Configuration Binding
// ==========================================
builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection(MongoDbSettings.SectionName));
builder.Services.PostConfigure<MongoDbSettings>(options =>
{
    var envConn = Environment.GetEnvironmentVariable("MONGODB_CONNECTION_STRING");
    if (!string.IsNullOrWhiteSpace(envConn)) options.ConnectionString = envConn;

    var envDb = Environment.GetEnvironmentVariable("MONGODB_DATABASE_NAME");
    if (!string.IsNullOrWhiteSpace(envDb)) options.DatabaseName = envDb;

    var envCol = Environment.GetEnvironmentVariable("MONGODB_USERS_COLLECTION");
    if (!string.IsNullOrWhiteSpace(envCol)) options.UsersCollectionName = envCol;
});

builder.Services.Configure<JwtSettings>(
    builder.Configuration.GetSection(JwtSettings.SectionName));
builder.Services.PostConfigure<JwtSettings>(options =>
{
    var envKey = Environment.GetEnvironmentVariable("JWT_SECRET_KEY");
    if (!string.IsNullOrWhiteSpace(envKey)) options.SecretKey = envKey;

    var envIssuer = Environment.GetEnvironmentVariable("JWT_ISSUER");
    if (!string.IsNullOrWhiteSpace(envIssuer)) options.Issuer = envIssuer;

    var envAudience = Environment.GetEnvironmentVariable("JWT_AUDIENCE");
    if (!string.IsNullOrWhiteSpace(envAudience)) options.Audience = envAudience;

    var envExpiry = Environment.GetEnvironmentVariable("JWT_EXPIRY_MINUTES");
    if (!string.IsNullOrWhiteSpace(envExpiry) && int.TryParse(envExpiry, out var mins)) options.ExpiryMinutes = mins;
});

builder.Services.Configure<EmailSettings>(
    builder.Configuration.GetSection(EmailSettings.SectionName));
builder.Services.PostConfigure<EmailSettings>(options =>
{
    var envHost = Environment.GetEnvironmentVariable("SMTP_HOST");
    if (!string.IsNullOrWhiteSpace(envHost)) options.SmtpHost = envHost;

    var envPort = Environment.GetEnvironmentVariable("SMTP_PORT");
    if (!string.IsNullOrWhiteSpace(envPort) && int.TryParse(envPort, out var p)) options.SmtpPort = p;

    var envSsl = Environment.GetEnvironmentVariable("ENABLE_SSL");
    if (!string.IsNullOrWhiteSpace(envSsl) && bool.TryParse(envSsl, out var ssl)) options.EnableSsl = ssl;

    var envEmail = Environment.GetEnvironmentVariable("SENDER_EMAIL");
    if (!string.IsNullOrWhiteSpace(envEmail)) options.SenderEmail = envEmail;

    var envPass = Environment.GetEnvironmentVariable("SENDER_PASSWORD");
    if (!string.IsNullOrWhiteSpace(envPass)) options.SenderPassword = envPass;

    var envName = Environment.GetEnvironmentVariable("SENDER_NAME");
    if (!string.IsNullOrWhiteSpace(envName)) options.SenderName = envName;

    var envPortal = Environment.GetEnvironmentVariable("PORTAL_URL");
    if (!string.IsNullOrWhiteSpace(envPortal)) options.PortalUrl = envPortal;
});

// Configure QR Security options from appsettings and environment (Member 4)
builder.Services.Configure<QrSecurityOptions>(
    builder.Configuration.GetSection(QrSecurityOptions.SectionName));
builder.Services.PostConfigure<QrSecurityOptions>(options =>
{
    var envHmac = Environment.GetEnvironmentVariable("QR_HMAC_SECRET");
    if (!string.IsNullOrWhiteSpace(envHmac)) options.HmacSecret = envHmac;

    var envPrefix = Environment.GetEnvironmentVariable("QR_PAYLOAD_PREFIX");
    if (!string.IsNullOrWhiteSpace(envPrefix)) options.PayloadPrefix = envPrefix;

    var envTolerance = Environment.GetEnvironmentVariable("QR_TOLERANCE_MINUTES");
    if (!string.IsNullOrWhiteSpace(envTolerance) && int.TryParse(envTolerance, out var tol))
        options.ToleranceMinutes = tol;
});

var jwtSettings = builder.Configuration
    .GetSection(JwtSettings.SectionName)
    .Get<JwtSettings>() ?? new JwtSettings();

var envJwtKey = Environment.GetEnvironmentVariable("JWT_SECRET_KEY");
if (!string.IsNullOrWhiteSpace(envJwtKey)) jwtSettings.SecretKey = envJwtKey;

// ==========================================
// 2. Dependency Injection Registrations
// ==========================================
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddSingleton<IMongoDbContext>(sp => sp.GetRequiredService<MongoDbContext>());

// Member 1 Services
builder.Services.AddScoped<ITokenService, TokenService>();
builder.Services.AddScoped<IEmailService, EmailService>();
builder.Services.AddScoped<IUserService, UserService>();

// Member 2 Services
builder.Services.AddScoped<IStationService, StationService>();

// Member 4 Services
builder.Services.AddScoped<IReservationRepository, ReservationRepository>();
builder.Services.AddSingleton<IQrSignatureService, QrSignatureService>();
builder.Services.AddScoped<IOperatorVerificationService, OperatorVerificationService>();
builder.Services.AddScoped<IDashboardQueryService, DashboardQueryService>();

// ==========================================
// 3. Controllers & JSON Formatting
// ==========================================
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
        options.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
    });

// ==========================================
// 4. JWT Authentication & Authorization
// ==========================================
var keyBytes = Encoding.UTF8.GetBytes(
    string.IsNullOrEmpty(jwtSettings.SecretKey) 
        ? "SmartSolarMicrogridSuperSecretKey2026EnterpriseSecurityTokenSignatureKey!" 
        : jwtSettings.SecretKey);

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.RequireHttpsMetadata = false;
    options.SaveToken = true;
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(keyBytes),
        ValidateIssuer = true,
        ValidIssuer = string.IsNullOrEmpty(jwtSettings.Issuer) ? "SmartSolarMicrogridApi" : jwtSettings.Issuer,
        ValidateAudience = true,
        ValidAudience = string.IsNullOrEmpty(jwtSettings.Audience) ? "SmartSolarMicrogridClients" : jwtSettings.Audience,
        ValidateLifetime = true,
        ClockSkew = TimeSpan.FromMinutes(1)
    };
});

builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("BackofficeOnly", policy => policy.RequireRole("Backoffice", "Administrator"));
    options.AddPolicy("GridOperatorOnly", policy => policy.RequireRole("GridOperator"));
    options.AddPolicy("ProsumerOnly", policy => policy.RequireRole("Prosumer"));
    options.AddPolicy("StaffOnly", policy => policy.RequireRole("Backoffice", "Administrator", "GridOperator"));
});

// ==========================================
// 5. CORS Policy
// ==========================================
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAllOrigins", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

// ==========================================
// 6. Swagger / OpenAPI Configuration
// ==========================================
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Smart Solar Microgrid Central Web API",
        Version = "v1",
        Description = "Enterprise RESTful Web API for Smart Solar Microgrid Trading System (SE4040 - 2026)",
        Contact = new OpenApiContact
        {
            Name = "Smart Solar Microgrid Enterprise Team",
            Email = "support@smartgrid.lk"
        }
    });

    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.ApiKey,
        Scheme = "Bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter 'Bearer' [space] and then your valid JWT token in the text input below.\r\n\r\nExample: \"Bearer eyJhbGciOiJIUzI1Ni...\""
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

var app = builder.Build();

// ==========================================
// 7. Request Processing Pipeline
// ==========================================
if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "Smart Solar Microgrid API v1");
    c.RoutePrefix = "swagger";
});

app.UseCors("AllowAllOrigins");
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

// Central Service Discovery & Health Status Endpoint
app.MapGet("/", () => Results.Ok(new
{
    service = "Smart Solar Microgrid Trading System - Central Web API",
    status = "Online",
    version = "1.0.0",
    modules = new[]
    {
        "Member 1: Identity, Authentication & User Lifecycle",
        "Member 4: Operator Verification & Operational Dashboard"
    },
    swagger = "/swagger",
    endpoints = new[]
    {
        "/api/auth/login",
        "/api/auth/register-prosumer",
        "/api/users",
        "/api/prosumers/pending",
        "/api/reservations/dashboard-metrics",
        "/api/reservations",
        "/api/reservations/verify-qr",
        "/api/reservations/{id}/finalize"
    }
}));

app.Run();
