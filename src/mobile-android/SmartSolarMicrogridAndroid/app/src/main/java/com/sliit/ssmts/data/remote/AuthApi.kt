/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Retrofit REST API interface defining authentication, registration, and prosumer endpoints.
 */

package com.sliit.ssmts.data.remote

import com.sliit.ssmts.data.remote.dto.ApiResponse
import com.sliit.ssmts.data.remote.dto.DeactivationRequest
import com.sliit.ssmts.data.remote.dto.LoginRequest
import com.sliit.ssmts.data.remote.dto.LoginResponse
import com.sliit.ssmts.data.remote.dto.ProfileUpdateRequest
import com.sliit.ssmts.data.remote.dto.RegisterRequest
import com.sliit.ssmts.data.remote.dto.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit contract for Member 1 API endpoints hosted on the central ASP.NET Core server.
 */
interface AuthApi {

    /**
     * Authenticates user credentials and returns JWT bearer token and profile details.
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * Registers a new solar prosumer account with initial status 'PendingActivation'.
     */
    @POST("api/prosumers")
    suspend fun registerProsumer(
        @Body request: RegisterRequest
    ): Response<ApiResponse<UserResponse>>

    /**
     * Retrieves prosumer profile information by NIC.
     */
    @GET("api/prosumers/{nic}")
    suspend fun getProfile(
        @Path("nic") nic: String
    ): Response<ApiResponse<UserResponse>>

    /**
     * Updates permitted profile fields (fullName, phone) for the specified NIC.
     */
    @PUT("api/prosumers/{nic}")
    suspend fun updateProfile(
        @Path("nic") nic: String,
        @Body request: ProfileUpdateRequest
    ): Response<ApiResponse<UserResponse>>

    /**
     * Requests self-deactivation of a prosumer account.
     */
    @PATCH("api/prosumers/{nic}/request-deactivation")
    suspend fun requestDeactivation(
        @Path("nic") nic: String,
        @Body request: DeactivationRequest? = null
    ): Response<ApiResponse<UserResponse>>
}
