/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for prosumer self-registration requests from the mobile application.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload submitted to POST /api/prosumers.
 *
 * @property nic National Identity Card number.
 * @property username Requested unique username.
 * @property password Plaintext password to hash on server.
 * @property fullName Full legal name.
 * @property phone Contact phone number.
 */
data class RegisterRequest(
    @SerializedName("nic")
    val nic: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null
)
