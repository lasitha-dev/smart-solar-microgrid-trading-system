/**
 * Description: Predefined simulation scenarios providing canonical, conflicting, and malformed
 * QR payload tokens to facilitate single-device viva demonstrations (Rule 6.3 & FR-M4-05).
 */
package com.sliit.ssmts.operator_dashboard.util

/**
 * Representation of a single viva test QR simulation scenario.
 *
 * @property id Unique scenario identifier.
 * @property title Human-readable scenario title.
 * @property description Explanation of the scenario behavior and expected outcome.
 * @property payload The raw QR token string to inject into the scanner pipeline.
 */
data class FastTestScenario(
    val id: String,
    val title: String,
    val description: String,
    val payload: String
)

/**
 * Registry of canonical viva evaluation test payloads for operator verification.
 */
object FastTestQrScenarios {

    /**
     * Canonical approved reservation ready for physical energy transfer (25.0 kWh expected).
     */
    const val APPROVED_VALID_PAYLOAD =
        "SSMTS-QR|664fa10b9c3e2e1a4f001201|200012345678|ST-002|2026-09-18T10:30:00Z|SIG-a9b8c7"

    /**
     * Reservation that has already completed and finalized (triggers 409 Conflict rejection).
     */
    const val ALREADY_COMPLETED_PAYLOAD =
        "SSMTS-QR|664fa10b9c3e2e1a4f001202|200087654321|ST-001|2026-09-18T09:00:00Z|SIG-d4e5f6"

    /**
     * Malformed token violating delimiter structure (triggers client-side defensive validation failure).
     */
    const val MALFORMED_SYNTAX_PAYLOAD =
        "INVALID_QR_TOKEN_WITHOUT_DELIMITERS"

    /**
     * Retrieves the standard suite of viva demonstration scenarios.
     *
     * @return List of predefined FastTestScenario objects.
     */
    fun getPredefinedScenarios(): List<FastTestScenario> {
        return listOf(
            FastTestScenario(
                id = "scenario_approved",
                title = "1. Valid Approved Reservation",
                description = "Reservation 664fa10b9c3e2e1a4f001201, Bay BAY-02, 25.0 kWh expected. Server returns valid: true.",
                payload = APPROVED_VALID_PAYLOAD
            ),
            FastTestScenario(
                id = "scenario_completed",
                title = "2. Already Completed Reservation",
                description = "Reservation 664fa10b9c3e2e1a4f001202. Server returns valid: false (ERR_RESERVATION_ALREADY_COMPLETED).",
                payload = ALREADY_COMPLETED_PAYLOAD
            ),
            FastTestScenario(
                id = "scenario_malformed",
                title = "3. Malformed QR Payload",
                description = "Missing delimiters and prefix. Defensive client parser rejects before network dispatch.",
                payload = MALFORMED_SYNTAX_PAYLOAD
            )
        )
    }
}
