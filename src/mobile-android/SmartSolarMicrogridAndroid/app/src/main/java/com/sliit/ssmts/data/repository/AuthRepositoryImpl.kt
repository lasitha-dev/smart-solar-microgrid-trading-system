/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Repository implementation coordinating Retrofit remote API calls with Room SQLite local storage.
 */

package com.sliit.ssmts.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sliit.ssmts.data.remote.AuthApi
import com.sliit.ssmts.data.remote.RetrofitClient
import com.sliit.ssmts.data.remote.dto.ApiResponse
import com.sliit.ssmts.data.remote.dto.DeactivationRequest
import com.sliit.ssmts.data.remote.dto.LoginRequest
import com.sliit.ssmts.data.remote.dto.ProfileUpdateRequest
import com.sliit.ssmts.data.remote.dto.RegisterRequest
import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.domain.repository.IAuthRepository
import com.sliit.ssmts.util.NetworkResult
import com.sliit.ssmts.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of [IAuthRepository] integrating Retrofit and Room SQLite persistence.
 */
class AuthRepositoryImpl(
    private val context: Context,
    private val authApi: AuthApi = RetrofitClient.getAuthApi(context),
    private val sessionManager: SessionManager = SessionManager(context)
) : IAuthRepository {

    private val gson = Gson()

    override suspend fun login(identifier: String, password: String): NetworkResult<UserSession> =
        withContext(Dispatchers.IO) {
            try {
                val request = LoginRequest(identifier = identifier.trim(), password = password)
                val response = authApi.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val data = apiResponse.data

                    if (apiResponse.success && data != null) {
                        val session = UserSession(
                            token = data.token,
                            userId = data.userId,
                            nic = data.nic,
                            username = data.username,
                            fullName = data.fullName,
                            phone = data.phone.orEmpty(),
                            address = data.address.orEmpty(),
                            latitude = data.latitude,
                            longitude = data.longitude,
                            role = data.role,
                            status = data.status,
                            expiresAt = data.expiresAt
                        )

                        // Cache active session in local SQLite
                        sessionManager.saveSession(session)

                        NetworkResult.Success(session, apiResponse.message)
                    } else {
                        NetworkResult.Error(
                            response.code(),
                            apiResponse.message ?: "Authentication failed."
                        )
                    }
                } else {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                    NetworkResult.Error(response.code(), errorMessage)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }

    override suspend fun registerProsumer(
        nic: String,
        username: String,
        password: String,
        fullName: String,
        phone: String,
        address: String,
        latitude: Double?,
        longitude: Double?
    ): NetworkResult<UserSession> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                nic = nic.trim(),
                username = username.trim(),
                password = password,
                fullName = fullName.trim(),
                phone = phone.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude
            )

            val response = authApi.registerProsumer(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.data

                if (apiResponse.success && data != null) {
                    val registeredUser = UserSession(
                        token = "",
                        userId = data.id ?: "",
                        nic = data.nic,
                        username = data.username,
                        fullName = data.fullName,
                        phone = data.phone,
                        address = data.address.orEmpty(),
                        latitude = data.latitude,
                        longitude = data.longitude,
                        role = data.role,
                        status = data.status
                    )
                    NetworkResult.Success(registeredUser, apiResponse.message)
                } else {
                    NetworkResult.Error(
                        response.code(),
                        apiResponse.message ?: "Registration failed."
                    )
                }
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                NetworkResult.Error(response.code(), errorMessage)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun getProfile(nic: String): NetworkResult<UserSession> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.getProfile(nic.trim())

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    val data = apiResponse.data

                    if (apiResponse.success && data != null) {
                        val activeSession = sessionManager.getActiveSession()
                        val updatedSession = UserSession(
                            token = activeSession?.token ?: "",
                            userId = data.id ?: activeSession?.userId ?: "",
                            nic = data.nic,
                            username = data.username,
                            fullName = data.fullName,
                            phone = data.phone,
                            address = data.address ?: activeSession?.address.orEmpty(),
                            latitude = data.latitude ?: activeSession?.latitude,
                            longitude = data.longitude ?: activeSession?.longitude,
                            role = data.role,
                            status = data.status
                        )

                        // Update local cache
                        sessionManager.updateProfileCache(
                            data.fullName,
                            data.phone,
                            data.address ?: activeSession?.address.orEmpty()
                        )
                        sessionManager.updateStatusCache(data.status)

                        NetworkResult.Success(updatedSession, apiResponse.message)
                    } else {
                        NetworkResult.Error(
                            response.code(),
                            apiResponse.message ?: "Failed to fetch profile."
                        )
                    }
                } else {
                    val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                    NetworkResult.Error(response.code(), errorMessage)
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }

    override suspend fun updateProfile(
        nic: String,
        fullName: String,
        phone: String,
        address: String
    ): NetworkResult<UserSession> = withContext(Dispatchers.IO) {
        try {
            val request = ProfileUpdateRequest(
                fullName = fullName.trim(),
                phone = phone.trim(),
                address = address.trim()
            )
            val response = authApi.updateProfile(nic.trim(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.data

                if (apiResponse.success && data != null) {
                    val activeSession = sessionManager.getActiveSession()
                    val updatedSession = UserSession(
                        token = activeSession?.token ?: "",
                        userId = data.id ?: activeSession?.userId ?: "",
                        nic = data.nic,
                        username = data.username,
                        fullName = data.fullName,
                        phone = data.phone,
                        address = data.address ?: address.trim(),
                        latitude = data.latitude ?: activeSession?.latitude,
                        longitude = data.longitude ?: activeSession?.longitude,
                        role = data.role,
                        status = data.status
                    )

                    // Sync local SQLite cache
                    sessionManager.updateProfileCache(
                        data.fullName,
                        data.phone,
                        data.address ?: address.trim()
                    )

                    NetworkResult.Success(updatedSession, apiResponse.message)
                } else {
                    NetworkResult.Error(
                        response.code(),
                        apiResponse.message ?: "Profile update failed."
                    )
                }
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                NetworkResult.Error(response.code(), errorMessage)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun requestDeactivation(
        nic: String,
        reason: String?,
        remarks: String?
    ): NetworkResult<UserSession> = withContext(Dispatchers.IO) {
        try {
            val request = DeactivationRequest(reason = reason?.trim(), remarks = remarks?.trim())
            val response = authApi.requestDeactivation(nic.trim(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.data

                if (apiResponse.success && data != null) {
                    val activeSession = sessionManager.getActiveSession()
                    val updatedSession = UserSession(
                        token = activeSession?.token ?: "",
                        userId = data.id ?: activeSession?.userId ?: "",
                        nic = data.nic,
                        username = data.username,
                        fullName = data.fullName,
                        role = data.role,
                        status = data.status
                    )

                    // Update status in SQLite or clear session
                    sessionManager.updateStatusCache(data.status)

                    NetworkResult.Success(updatedSession, apiResponse.message)
                } else {
                    NetworkResult.Error(
                        response.code(),
                        apiResponse.message ?: "Deactivation request failed."
                    )
                }
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string(), response.code())
                NetworkResult.Error(response.code(), errorMessage)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun getActiveSession(): UserSession? = sessionManager.getActiveSession()

    override fun observeActiveSession(): Flow<UserSession?> = sessionManager.observeActiveSession()

    override suspend fun clearSession() = sessionManager.clearSession()

    /**
     * Parses error response JSON payload into a human-readable message string.
     */
    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        if (errorBody.isNullOrBlank()) {
            return when (statusCode) {
                400 -> "Invalid request parameters."
                401 -> "Invalid username/NIC or password."
                403 -> "Account is pending activation or deactivated."
                404 -> "Requested resource was not found."
                409 -> "A conflict occurred (duplicate NIC or username)."
                else -> "An error occurred (HTTP $statusCode)."
            }
        }

        return try {
            val errorResponse = gson.fromJson(errorBody, ApiResponse::class.java)
            errorResponse.message ?: "Server returned error ($statusCode)."
        } catch (_: Exception) {
            errorBody
        }
    }
}
