/**
 * Description: Unit test suite for QrPayloadParser verifying canonical token extraction,
 * delimiter checks, whitespace trimming, and predictable error taxonomy on malformed inputs.
 */
package com.sliit.ssmts.operator_dashboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit test verifying QrPayloadParser defensive validation behavior.
 */
class QrPayloadParserTest {

    private val validToken = "SSMTS-QR|RES-2026-001|199512345678|STATION-01|2026-09-16T14:30:00Z|d41d8cd98f00b204e9800998ecf8427e"

    /**
     * Verifies that valid canonical QR tokens parse cleanly into structured domain models.
     */
    @Test
    fun validateAndParse_validCanonicalToken_returnsSuccessWithCorrectFields() {
        val result = QrPayloadParser.validateAndParse(validToken)

        assertTrue(result is QrParseResult.Success)
        val payload = (result as QrParseResult.Success).payload

        assertEquals("RES-2026-001", payload.reservationId)
        assertEquals("199512345678", payload.prosumerNic)
        assertEquals("STATION-01", payload.stationId)
        assertEquals("2026-09-16T14:30:00Z", payload.scheduledDateTimeIso)
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", payload.signature)
        assertEquals(validToken, payload.rawPayload)
    }

    /**
     * Verifies parse() direct invocation returns ParsedQrPayload on valid input.
     */
    @Test
    fun parse_validCanonicalToken_returnsParsedPayload() {
        val payload = QrPayloadParser.parse(validToken)
        assertEquals("RES-2026-001", payload.reservationId)
        assertEquals("199512345678", payload.prosumerNic)
    }

    /**
     * Verifies null input returns ERR_EMPTY_QR_PAYLOAD.
     */
    @Test
    fun validateAndParse_nullInput_returnsEmptyPayloadError() {
        val result = QrPayloadParser.validateAndParse(null)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_EMPTY_QR_PAYLOAD, failure.errorCode)
    }

    /**
     * Verifies blank input returns ERR_EMPTY_QR_PAYLOAD.
     */
    @Test
    fun validateAndParse_blankInput_returnsEmptyPayloadError() {
        val result = QrPayloadParser.validateAndParse("   ")

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_EMPTY_QR_PAYLOAD, failure.errorCode)
    }

    /**
     * Verifies tokens without SSMTS-QR prefix return ERR_INVALID_QR_PREFIX.
     */
    @Test
    fun validateAndParse_invalidPrefix_returnsInvalidPrefixError() {
        val token = "OTHER-APP|RES-001|199512345678|STATION-01|2026-09-16T14:30:00Z|SIG"
        val result = QrPayloadParser.validateAndParse(token)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_INVALID_QR_PREFIX, failure.errorCode)
    }

    /**
     * Verifies tokens with fewer than 6 segments return ERR_MALFORMED_QR_SEGMENTS.
     */
    @Test
    fun validateAndParse_tooFewSegments_returnsMalformedSegmentsError() {
        val token = "SSMTS-QR|RES-001|199512345678"
        val result = QrPayloadParser.validateAndParse(token)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_MALFORMED_QR_SEGMENTS, failure.errorCode)
    }

    /**
     * Verifies tokens with more than 6 segments return ERR_MALFORMED_QR_SEGMENTS.
     */
    @Test
    fun validateAndParse_tooManySegments_returnsMalformedSegmentsError() {
        val token = "SSMTS-QR|RES-001|199512345678|STATION-01|2026-09-16T14:30:00Z|SIG|EXTRA_FIELD"
        val result = QrPayloadParser.validateAndParse(token)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_MALFORMED_QR_SEGMENTS, failure.errorCode)
    }

    /**
     * Verifies tokens with empty intermediate reservation ID return ERR_MISSING_QR_FIELDS.
     */
    @Test
    fun validateAndParse_missingReservationId_returnsMissingFieldsError() {
        val token = "SSMTS-QR|  |199512345678|STATION-01|2026-09-16T14:30:00Z|SIG"
        val result = QrPayloadParser.validateAndParse(token)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_MISSING_QR_FIELDS, failure.errorCode)
    }

    /**
     * Verifies tokens with empty signature return ERR_MISSING_QR_FIELDS.
     */
    @Test
    fun validateAndParse_missingSignature_returnsMissingFieldsError() {
        val token = "SSMTS-QR|RES-001|199512345678|STATION-01|2026-09-16T14:30:00Z|"
        val result = QrPayloadParser.validateAndParse(token)

        assertTrue(result is QrParseResult.Failure)
        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_MISSING_QR_FIELDS, failure.errorCode)
    }

    /**
     * Verifies parse() throws MalformedQrException with predictable error code on invalid input.
     */
    @Test
    fun parse_malformedToken_throwsMalformedQrExceptionWithErrorCode() {
        try {
            QrPayloadParser.parse("MALFORMED_RAW_STRING")
            fail("Expected MalformedQrException to be thrown")
        } catch (e: MalformedQrException) {
            assertEquals(QrPayloadParser.ERR_MALFORMED_QR_SEGMENTS, e.errorCode)
        }
    }

    /**
     * Verifies isValid helper returns true for valid token and false for invalid tokens.
     */
    @Test
    fun isValid_returnsTrueForValidAndFalseForInvalid() {
        assertTrue(QrPayloadParser.isValid(validToken))
        assertFalse(QrPayloadParser.isValid(null))
        assertFalse(QrPayloadParser.isValid("INVALID"))
    }

    /**
     * Verifies surrounding whitespace is trimmed cleanly from tokens and segments.
     */
    @Test
    fun validateAndParse_trimsSurroundingWhitespace() {
        val whitespaceToken = "  SSMTS-QR | RES-TRIM | 199512345678 | STATION-01 | 2026-09-16T14:30:00Z | SIG123  "
        val result = QrPayloadParser.validateAndParse(whitespaceToken)

        assertTrue(result is QrParseResult.Success)
        val payload = (result as QrParseResult.Success).payload
        assertEquals("RES-TRIM", payload.reservationId)
        assertEquals("199512345678", payload.prosumerNic)
        assertEquals("SIG123", payload.signature)
    }
}
