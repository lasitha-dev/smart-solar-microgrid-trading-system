/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Main application entry point configuring dependency injection, JWT authentication, and HTTP request pipeline.
 */

using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Data;
using SmartSolarMicrogrid.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// ==========================================
// 1. Strongly-Typed Configuration Binding
// ==========================================
builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection(MongoDbSettings.SectionName));

builder.Services.Configure<JwtSettings>(
    builder.Configuration.GetSection(JwtSettings.SectionName));

var jwtSettings = builder.Configuration
    .GetSection(JwtSettings.SectionName)
    .Get<JwtSettings>() ?? new JwtSettings();

// ==========================================
// 2. Dependency Injection Registrations
// ==========================================
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<ITokenService, TokenService>();
builder.Services.AddScoped<IUserService, UserService>();

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
    options.AddPolicy("BackofficeOnly", policy => policy.RequireRole("Backoffice"));
    options.AddPolicy("GridOperatorOnly", policy => policy.RequireRole("GridOperator"));
    options.AddPolicy("ProsumerOnly", policy => policy.RequireRole("Prosumer"));
    options.AddPolicy("StaffOnly", policy => policy.RequireRole("Backoffice", "GridOperator"));
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
        Title = "Smart Solar Microgrid API",
        Version = "v1",
        Description = "Enterprise RESTful Web API for Smart Solar Microgrid Trading System - Member 1 (Auth & User Management)",
        Contact = new OpenApiContact
        {
            Name = "SILVA M N U (IT22169112)",
            Email = "IT22169112@my.sliit.lk"
        }
    });

    // Configure JWT Bearer authorization in Swagger UI
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
    c.RoutePrefix = string.Empty; // Serve Swagger at application root
});

app.UseCors("AllowAllOrigins");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
