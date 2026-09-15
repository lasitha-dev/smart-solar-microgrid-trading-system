// Description: Central application entry point and ASP.NET Core service configuration.

var builder = WebApplication.CreateBuilder(args);

// Add controllers support for REST API endpoints
builder.Services.AddControllers();

var app = builder.Build();

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

app.Run();
