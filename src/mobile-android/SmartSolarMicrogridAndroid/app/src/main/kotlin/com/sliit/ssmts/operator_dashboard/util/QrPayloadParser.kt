/**
 * Description: Defensive parser and sanitizer utility validating raw camera QR strings,
 * ensuring delimiter counts and canonical schema compliance before network verification.
 */
package com.sliit.ssmts.operator_dashboard.util

/**
 * Domain representation of structured fields extracted from a verified QR code token.
 *
 * @property rawPayload The unedited scanned token string.
 * @property reservationId Remote MongoDB reservation identifier.
 * @property prosumerNic Prosumer National Identity Card identifier.
 * @property stationId Solar station hub identifier.
 * @property scheduledDateTimeIso Scheduled energy transfer slot in ISO format.
 * @property signature Cryptographic HMAC-SHA256 signature string.
 */
data class ParsedQrPayload(
    val rawPayload: String,
    val reservationId: String,
    val prosumerNic: String,
    val stationId: String,
    val scheduledDateTimeIso: String,
    val signature: String
)

/**
 * Result abstraction representing successful parsing or structured failure.
 */
sealed class QrParseResult {
    /**
     * Successfully validated and parsed QR token.
     */
    data class Success(val payload: ParsedQrPayload) : QrParseResult()

    /**
     * Parsing failure containing structured error code and explanation.
     */
    data class Failure(val errorCode: String, val message: String) : QrParseResult()
}

/**
 * Exception thrown when parsing a malformed or unauthorized QR token.
 */
class MalformedQrException(
    val errorCode: String,
    message: String
) : IllegalArgumentException(message)

/**
 * Utility responsible for sanitizing, validating, and extracting components from raw QR strings.
 */
object QrPayloadParser {

    const val EXPECTED_PREFIX = "SSMTS-QR"
    const val DELIMITER = "|"
    const val EXPECTED_SEGMENT_COUNT = 6

    const val ERR_EMPTY_QR_PAYLOAD = "ERR_EMPTY_QR_PAYLOAD"
    const val ERR_INVALID_QR_PREFIX = "ERR_INVALID_QR_PREFIX"
    const val ERR_MALFORMED_QR_SEGMENTS = "ERR_MALFORMED_QR_SEGMENTS"
    const val ERR_MISSING_QR_FIELDS = "ERR_MISSING_QR_FIELDS"

    /**
     * Validates and parses a raw QR payload into a structured result.
     *
     * @param rawQr The raw scanned string from the camera viewfinder.
     * @return QrParseResult indicating Success with parsed data or Failure with error code.
     */
    fun validateAndParse(rawQr: String?): QrParseResult {
        if (rawQr.isNullOrBlank()) {
            return QrParseResult.Failure(
                errorCode = ERR_EMPTY_QR_PAYLOAD,
                message = "Scanned QR code token is empty or contains only whitespace."
            )
        }

        val trimmed = rawQr.trim()
        val segments = trimmed.split(DELIMITER)

        if (segments.size != EXPECTED_SEGMENT_COUNT) {
            return QrParseResult.Failure(
                errorCode = ERR_MALFORMED_QR_SEGMENTS,
                message = "QR token must contain exactly $EXPECTED_SEGMENT_COUNT delimited segments; found ${segments.size}."
            )
        }

        if (!segments[0].trim().equals(EXPECTED_PREFIX, ignoreCase = true)) {
            return QrParseResult.Failure(
                errorCode = ERR_INVALID_QR_PREFIX,
                message = "Unrecognized QR format; expected prefix '$EXPECTED_PREFIX' but found '${segments[0]}'."
            )
        }

        val reservationId = segments[1].trim()
        val prosumerNic = segments[2].trim()
        val stationId = segments[3].trim()
        val scheduledDateTime = segments[4].trim()
        val signature = segments[5].trim()

        if (reservationId.isEmpty() ||
            prosumerNic.isEmpty() ||
            stationId.isEmpty() ||
            scheduledDateTime.isEmpty() ||
            signature.isEmpty()
        ) {
            return QrParseResult.Failure(
                errorCode = ERR_MISSING_QR_FIELDS,
                message = "QR token contains missing or empty constituent fields."
            )
        }

        val payload = ParsedQrPayload(
            rawPayload = trimmed,
            reservationId = reservationId,
            prosumerNic = prosumerNic,
            stationId = stationId,
            scheduledDateTimeIso = scheduledDateTime,
            signature = signature
        )

        return QrParseResult.Success(payload)
    }

    /**
     * Parses a raw QR payload into ParsedQrPayload, throwing MalformedQrException on failure.
     *
     * @param rawQr The raw scanned string.
     * @return Extracted ParsedQrPayload.
     * @throws MalformedQrException If token is malformed, has invalid prefix, or contains empty fields.
     */
    fun parse(rawQr: String?): ParsedQrPayload {
        return when (val result = validateAndParse(rawQr)) {
            is QrParseResult.Success -> result.payload
            is QrParseResult.Failure -> throw MalformedQrException(result.errorCode, result.message)
        }
    }

    /**
     * Checks whether a raw QR code string satisfies the canonical SSMTS token structure.
     *
     * @param rawQr The raw scanned string.
     * @return True if token is structurally valid; false otherwise.
     */
    fun isValid(rawQr: String?): Boolean {
        return validateAndParse(rawQr) is QrParseResult.Success
    }
}
