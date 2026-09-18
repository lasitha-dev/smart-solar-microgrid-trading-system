/**
 * Description: Unit test suite for FastTestQrScenarios verifying validity of viva simulation
 * test payloads against the defensive QR parser (Rule 6.3 & FR-M4-05).
 */
package com.sliit.ssmts.operator_dashboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Asserts syntax compliance and error characteristics of all viva simulation test payloads.
 */
class FastTestQrScenariosTest {

    /**
     * Asserts that the canonical approved payload parses successfully with expected field values.
     */
    @Test
    fun approvedPayload_parsesCleanly() {
        val result = QrPayloadParser.validateAndParse(FastTestQrScenarios.APPROVED_VALID_PAYLOAD)
        assertTrue(result is QrParseResult.Success)

        val success = result as QrParseResult.Success
        assertEquals("664fa10b9c3e2e1a4f001201", success.payload.reservationId)
        assertEquals("200012345678", success.payload.prosumerNic)
        assertEquals("ST-002", success.payload.stationId)
        assertEquals("2026-09-18T10:30:00Z", success.payload.scheduledDateTimeIso)
        assertEquals("SIG-a9b8c7", success.payload.signature)
    }

    /**
     * Asserts that the already completed payload parses successfully so the backend can enforce 409 Conflict.
     */
    @Test
    fun completedPayload_parsesCleanly() {
        val result = QrPayloadParser.validateAndParse(FastTestQrScenarios.ALREADY_COMPLETED_PAYLOAD)
        assertTrue(result is QrParseResult.Success)

        val success = result as QrParseResult.Success
        assertEquals("664fa10b9c3e2e1a4f001202", success.payload.reservationId)
        assertEquals("200087654321", success.payload.prosumerNic)
        assertEquals("ST-001", success.payload.stationId)
    }

    /**
     * Asserts that the malformed syntax payload triggers client-side defensive parsing rejection.
     */
    @Test
    fun malformedPayload_triggersParsingFailure() {
        val result = QrPayloadParser.validateAndParse(FastTestQrScenarios.MALFORMED_SYNTAX_PAYLOAD)
        assertTrue(result is QrParseResult.Failure)

        val failure = result as QrParseResult.Failure
        assertEquals(QrPayloadParser.ERR_MALFORMED_QR_SEGMENTS, failure.errorCode)
    }

    /**
     * Asserts that predefined scenarios list is complete with non-empty descriptions.
     */
    @Test
    fun predefinedScenarios_arePopulatedAndDescriptive() {
        val scenarios = FastTestQrScenarios.getPredefinedScenarios()
        assertEquals(3, scenarios.size)

        for (scenario in scenarios) {
            assertNotNull(scenario.id)
            assertTrue(scenario.id.isNotBlank())
            assertTrue(scenario.title.isNotBlank())
            assertTrue(scenario.description.isNotBlank())
            assertTrue(scenario.payload.isNotBlank())
        }
    }
}
