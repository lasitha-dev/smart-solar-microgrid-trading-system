/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Domain model representing an authenticated user session in the Android client.
 */

package com.sliit.ssmts.domain.model

/**
 * Encapsulates the active session credentials and profile information for a logged-in user.
 *
 * @property token The signed JWT bearer token used for authorization headers.
 * @property userId The unique database document identifier of the user.
 * @property nic The National Identity Card number of the user.
 * @property username The login username of the user.
 * @property fullName The full legal name of the user.
 * @property role The system role (e.g., "Prosumer", "GridOperator", "Backoffice").
 * @property status The account lifecycle status (e.g., "Active", "PendingActivation", "Deactivated").
 * @property expiresAt The ISO timestamp when the issued token expires.
 */
data class UserSession(
    val token: String,
    val userId: String,
    val nic: String,
    val username: String,
    val fullName: String,
    val phone: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val role: String,
    val status: String,
    val expiresAt: String? = null
)
