/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object representing user authentication payload for Retrofit API.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body model for the POST /api/auth/login endpoint.
 *
 * @property identifier Username or NIC of the user.
 * @property password Plaintext password for authentication.
 */
data class LoginRequest(
    @SerializedName("identifier")
    val identifier: String,

    @SerializedName("password")
    val password: String
)
