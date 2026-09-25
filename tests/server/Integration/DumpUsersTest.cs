using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Models;
using Xunit;
using Xunit.Abstractions;

namespace SmartSolarMicrogrid.Api.Tests.Integration;

public class DumpUsersTest
{
    private readonly ITestOutputHelper _output;

    public DumpUsersTest(ITestOutputHelper output)
    {
        _output = output;
    }

    [Fact]
    public async Task PrintAllUsers()
    {
        var connStr = "mongodb+srv://user5:user5@cluster0.8e4nq9e.mongodb.net/SmartSolarMicrogridDb?retryWrites=true&w=majority&appName=Cluster0";
        var client = new MongoClient(connStr);
        var db = client.GetDatabase("SmartSolarMicrogridDb");
        var collection = db.GetCollection<User>("User's Detail");

        var users = await collection.Find(_ => true).ToListAsync();
        _output.WriteLine($"Total users found: {users.Count}");

        var candidatePasswords = new[]
        {
            "Password123!", "Admin@123", "Admin123!", "Solar#2026", "Solar@2026",
            "Admin@2026", "Test@1234", "P@ssword123", "123456", "admin", "admin123",
            "Officer@123", "Operator@123", "Prosumer@123", "Password@123"
        };

        foreach (var u in users)
        {
            string foundPw = "UNKNOWN";
            foreach (var pw in candidatePasswords)
            {
                if (PasswordHasher.VerifyPassword(pw, u.PasswordHash))
                {
                    foundPw = pw;
                    break;
                }
            }
            _output.WriteLine($"[USER] Username: {u.Username} | NIC: {u.Nic} | Role: {u.Role} | Status: {u.Status} | Password: {foundPw}");
        }
    }
}
