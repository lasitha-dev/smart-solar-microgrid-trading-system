/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Fake test repository implementing IAuthRepository for ViewModel unit testing.
 */

package com.sliit.ssmts.testutil

import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.domain.repository.IAuthRepository
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controllable in-memory test double for [IAuthRepository].
 */
class FakeAuthRepository : IAuthRepository {

    var loginResult: NetworkResult<UserSession>? = null
    var registerResult: NetworkResult<UserSession>? = null
    var getProfileResult: NetworkResult<UserSession>? = null
    var updateProfileResult: NetworkResult<UserSession>? = null
    var deactivationResult: NetworkResult<UserSession>? = null
    var changePasswordResult: NetworkResult<UserSession>? = null
    var deleteAccountResult: NetworkResult<Unit>? = null

    var currentActiveSession: UserSession? = null
    private val sessionFlow = MutableStateFlow<UserSession?>(null)
    var clearSessionCalled = false

    override suspend fun login(identifier: String, password: String): NetworkResult<UserSession> {
        val result = loginResult ?: NetworkResult.Error(401, "Invalid credentials")
        if (result is NetworkResult.Success) {
            currentActiveSession = result.data
            sessionFlow.value = result.data
        }
        return result
    }

    override suspend fun registerProsumer(
        nic: String,
        username: String,
        password: String,
        fullName: String,
        phone: String,
        email: String,
        address: String,
        latitude: Double?,
        longitude: Double?
    ): NetworkResult<UserSession> {
        return registerResult ?: NetworkResult.Error(400, "Registration failed")
    }

    override suspend fun getProfile(nic: String): NetworkResult<UserSession> {
        return getProfileResult ?: currentActiveSession?.let { NetworkResult.Success(it, "Profile loaded") }
            ?: NetworkResult.Error(404, "User not found")
    }

    override suspend fun updateProfile(
        nic: String,
        fullName: String,
        phone: String,
        address: String
    ): NetworkResult<UserSession> {
        val result = updateProfileResult ?: currentActiveSession?.copy(
            fullName = fullName,
            phone = phone,
            address = address
        )?.let { NetworkResult.Success(it, "Profile updated successfully.") }
        ?: NetworkResult.Error(400, "Update failed")

        if (result is NetworkResult.Success) {
            currentActiveSession = result.data
            sessionFlow.value = result.data
        }
        return result
    }

    override suspend fun requestDeactivation(
        nic: String,
        reason: String?,
        remarks: String?
    ): NetworkResult<UserSession> {
        return deactivationResult ?: NetworkResult.Error(400, "Deactivation failed")
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ): NetworkResult<UserSession> {
        return changePasswordResult ?: NetworkResult.Error(400, "Password change failed")
    }

    override suspend fun deleteAccount(confirmEmail: String): NetworkResult<Unit> {
        return deleteAccountResult ?: NetworkResult.Error(400, "Account deletion failed")
    }

    override suspend fun getActiveSession(): UserSession? {
        return currentActiveSession
    }

    override fun observeActiveSession(): Flow<UserSession?> {
        return sessionFlow.asStateFlow()
    }

    override suspend fun clearSession() {
        clearSessionCalled = true
        currentActiveSession = null
        sessionFlow.value = null
    }
}
