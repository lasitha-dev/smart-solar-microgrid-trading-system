// Description: Service contract for HMAC-SHA256 signature generation, payload formatting, parsing, and cryptographic verification.

using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Services;

/// <summary>
/// Description: Defines cryptographic operations for generating and validating signed QR tokens.
/// </summary>
public interface IQrSignatureService
{
    /// <summary>
    /// Computes the HMAC-SHA256 signature for the given reservation parameters.
    /// </summary>
    string GenerateSignature(string reservationId, string prosumerNic, string stationId, string scheduledDateTime);

    /// <summary>
    /// Constructs the complete pipe-delimited payload conforming to SSMTS-QR standards.
    /// </summary>
    string GenerateFullPayload(string reservationId, string prosumerNic, string stationId, DateTime scheduledDateTime);

    /// <summary>
    /// Parses a raw scanned QR payload into structured tokens and verifies delimiter syntax.
    /// </summary>
    ParsedQrToken ParsePayload(string rawQrPayload);

    /// <summary>
    /// Verifies the cryptographic integrity of a raw QR payload.
    /// </summary>
    bool VerifySignature(string rawQrPayload);

    /// <summary>
    /// Verifies the cryptographic integrity given explicit token segments.
    /// </summary>
    bool VerifySignature(string reservationId, string prosumerNic, string stationId, string scheduledDateTime, string signature);
}
