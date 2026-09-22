/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Domain repository interface defining authentication, registration, profile, and session contracts.
 */

package com.sliit.ssmts.domain.repository

import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract defining authentication, prosumer registration,
 * profile synchronization, and local SQLite session persistence operations.
 */
interface IAuthRepository {

    /**
     * Authenticates user credentials via the Web API.
     * Caches active session in local SQLite on success.
     *
     * @param identifier The username or NIC of the user.
     * @param password The plaintext password.
     * @return [NetworkResult] containing [UserSession] on success or error details on failure.
     */
    suspend fun login(identifier: String, password: String): NetworkResult<UserSession>

    /**
     * Registers a new solar prosumer account with initial status 'PendingActivation'.
     *
     * @param nic The unique National Identity Card number.
     * @param username The unique username.
     * @param password The account password.
     * @param fullName The full legal name.
     * @param phone The contact telephone number.
     * @param email The primary email address.
     * @param address The residential/facility address.
     * @param latitude The latitude coordinate of the facility.
     * @param longitude The longitude coordinate of the facility.
     * @return [NetworkResult] with the registered user profile.
     */
    suspend fun registerProsumer(
        nic: String,
        username: String,
        password: String,
        fullName: String,
        phone: String,
        email: String,
        address: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): NetworkResult<UserSession>

    /**
     * Retrieves the profile information for a prosumer by NIC.
     *
     * @param nic The unique NIC of the prosumer.
     * @return [NetworkResult] containing the updated [UserSession].
     */
    suspend fun getProfile(nic: String): NetworkResult<UserSession>

    /**
     * Updates permitted profile fields (fullName, phone, address) on the backend server.
     *
     * @param nic The prosumer's NIC.
     * @param fullName The updated full legal name.
     * @param phone The updated phone number.
     * @param address The updated address.
     * @return [NetworkResult] containing the modified [UserSession].
     */
    suspend fun updateProfile(
        nic: String,
        fullName: String,
        phone: String,
        address: String
    ): NetworkResult<UserSession>

    /**
     * Submits a request to self-deactivate the prosumer account.
     *
     * @param nic The prosumer's NIC.
     * @param reason Optional reason for deactivation.
     * @param remarks Optional feedback remarks.
     * @return [NetworkResult] containing the updated [UserSession].
     */
    suspend fun requestDeactivation(nic: String, reason: String?, remarks: String?): NetworkResult<UserSession>

    /**
     * Changes authenticated user's account password on the backend server.
     *
     * @param currentPassword Current account password.
     * @param newPassword Desired new complex password.
     * @param confirmNewPassword Confirmation of new password.
     * @return [NetworkResult] containing updated [UserSession] or error.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ): NetworkResult<UserSession>

    /**
     * Permanently deletes authenticated user's account upon verifying registered email.
     * Clears local SQLite session and SharedPreferences on success.
     *
     * @param confirmEmail The email address entered for confirmation.
     * @return [NetworkResult] with success or error details.
     */
    suspend fun deleteAccount(confirmEmail: String): NetworkResult<Unit>

    /**
     * Retrieves the currently active user session from local Room SQLite.
     *
     * @return The cached [UserSession] if logged in; otherwise null.
     */
    suspend fun getActiveSession(): UserSession?

    /**
     * Observes the active user session as a reactive Flow.
     *
     * @return A Flow emitting changes to the cached [UserSession].
     */
    fun observeActiveSession(): Flow<UserSession?>

    /**
     * Clears the active session from local Room SQLite (logging out).
     */
    suspend fun clearSession()
}
