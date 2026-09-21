/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Generic response envelope DTO matching backend ApiResponseDto structure.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Standard JSON envelope returned by the Web API.
 *
 * @param T The payload type encapsulated in the response.
 * @property success Indicates whether the operation succeeded.
 * @property message Informational or error description.
 * @property data The encapsulated payload (or null if failed).
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null
)
