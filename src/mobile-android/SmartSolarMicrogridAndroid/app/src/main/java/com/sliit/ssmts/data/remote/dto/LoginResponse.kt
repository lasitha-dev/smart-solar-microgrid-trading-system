/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object representing the authentication response payload containing JWT token.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response model received upon successful authentication from POST /api/auth/login.
 *
 * @property token Signed JWT bearer token.
 * @property tokenType Token type prefix (e.g. "Bearer").
 * @property userId Unique MongoDB user identifier.
 * @property nic National Identity Card number.
 * @property username Account username.
 * @property fullName Full legal name.
 * @property role Assigned user role (e.g. "Prosumer").
 * @property status Account lifecycle status (e.g. "Active").
 * @property expiresAt UTC expiration timestamp.
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("tokenType")
    val tokenType: String = "Bearer",

    @SerializedName("userId")
    val userId: String,

    @SerializedName("nic")
    val nic: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("role")
    val role: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("expiresAt")
    val expiresAt: String? = null
)
