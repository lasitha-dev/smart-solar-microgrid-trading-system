// Description: Unit tests validating HMAC-SHA256 signature generation, payload formatting, tamper detection, and error parsing.

using Microsoft.Extensions.Options;
using SmartSolarMicrogrid.Api.Configuration;
using SmartSolarMicrogrid.Api.Services;
using Xunit;

namespace SmartSolarMicrogrid.Tests.Unit;

public class QrSignatureServiceTests
{
    private readonly IQrSignatureService _sut;
    private readonly QrSecurityOptions _options;

    public QrSignatureServiceTests()
    {
        _options = new QrSecurityOptions
        {
            HmacSecret = "Test_Secret_Key_For_Unit_Testing_Microgrid_2026!",
            PayloadPrefix = "SSMTS-QR",
            ToleranceMinutes = 30
        };

        _sut = new QrSignatureService(Options.Create(_options));
    }

    [Fact]
    public void GenerateFullPayload_ShouldProduceExpectedSixPartPipeDelimitedFormat()
    {
        // Arrange
        var reservationId = "664fa10b9c3e2e1a4f001201";
        var prosumerNic = "200012345678";
        var stationId = "ST-002";
        var scheduledTime = new DateTime(2026, 9, 18, 10, 30, 0, DateTimeKind.Utc);

        // Act
        var payload = _sut.GenerateFullPayload(reservationId, prosumerNic, stationId, scheduledTime);

        // Assert
        Assert.NotNull(payload);
        var parts = payload.Split('|');
        Assert.Equal(6, parts.Length);
        Assert.Equal("SSMTS-QR", parts[0]);
        Assert.Equal(reservationId, parts[1]);
        Assert.Equal(prosumerNic, parts[2]);
        Assert.Equal(stationId, parts[3]);
        Assert.Equal("2026-09-18T10:30:00Z", parts[4]);
        Assert.False(string.IsNullOrWhiteSpace(parts[5]));
    }

    [Fact]
    public void VerifySignature_WithValidPayload_ShouldReturnTrue()
    {
        // Arrange
        var reservationId = "664fa10b9c3e2e1a4f001201";
        var prosumerNic = "200012345678";
        var stationId = "ST-002";
        var scheduledTime = new DateTime(2026, 9, 18, 10, 30, 0, DateTimeKind.Utc);
        var payload = _sut.GenerateFullPayload(reservationId, prosumerNic, stationId, scheduledTime);

        // Act
        var isValid = _sut.VerifySignature(payload);

        // Assert
        Assert.True(isValid);
    }

    [Fact]
    public void VerifySignature_WithTamperedReservationId_ShouldReturnFalse()
    {
        // Arrange
        var reservationId = "664fa10b9c3e2e1a4f001201";
        var prosumerNic = "200012345678";
        var stationId = "ST-002";
        var scheduledTime = new DateTime(2026, 9, 18, 10, 30, 0, DateTimeKind.Utc);
        var payload = _sut.GenerateFullPayload(reservationId, prosumerNic, stationId, scheduledTime);

        // Tamper with reservation ID
        var tamperedPayload = payload.Replace("664fa10b9c3e2e1a4f001201", "664fa10b9c3e2e1a4f009999");

        // Act
        var isValid = _sut.VerifySignature(tamperedPayload);

        // Assert
        Assert.False(isValid);
    }

    [Fact]
    public void VerifySignature_WithTamperedProsumerNic_ShouldReturnFalse()
    {
        // Arrange
        var payload = _sut.GenerateFullPayload("res-1", "200012345678", "ST-001", DateTime.UtcNow);
        var tamperedPayload = payload.Replace("200012345678", "199999999999");

        // Act
        var isValid = _sut.VerifySignature(tamperedPayload);

        // Assert
        Assert.False(isValid);
    }

    [Fact]
    public void VerifySignature_WithTamperedStationId_ShouldReturnFalse()
    {
        // Arrange
        var payload = _sut.GenerateFullPayload("res-1", "nic-1", "ST-001", DateTime.UtcNow);
        var tamperedPayload = payload.Replace("ST-001", "ST-999");

        // Act
        var isValid = _sut.VerifySignature(tamperedPayload);

        // Assert
        Assert.False(isValid);
    }

    [Fact]
    public void VerifySignature_WithTamperedScheduledTime_ShouldReturnFalse()
    {
        // Arrange
        var scheduledTime = new DateTime(2026, 9, 18, 10, 30, 0, DateTimeKind.Utc);
        var payload = _sut.GenerateFullPayload("res-1", "nic-1", "ST-001", scheduledTime);
        var tamperedPayload = payload.Replace("2026-09-18T10:30:00Z", "2026-09-18T12:30:00Z");

        // Act
        var isValid = _sut.VerifySignature(tamperedPayload);

        // Assert
        Assert.False(isValid);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("SSMTS-QR|only|three|parts")]
    [InlineData("INVALID-PREFIX|res1|nic1|st1|2026-09-18T10:30:00Z|sig1")]
    [InlineData("SSMTS-QR||nic1|st1|2026-09-18T10:30:00Z|sig1")]
    public void ParsePayload_WithMalformedInputs_ShouldFlagInvalidFormat(string malformedPayload)
    {
        // Act
        var result = _sut.ParsePayload(malformedPayload);

        // Assert
        Assert.False(result.IsValidFormat);
        Assert.NotNull(result.ErrorMessage);
    }
}
