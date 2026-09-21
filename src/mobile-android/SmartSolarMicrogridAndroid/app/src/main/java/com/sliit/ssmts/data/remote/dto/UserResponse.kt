/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object representing a sanitized user account received from the Web API.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Sanitized user account model returned by registration, profile query, and lifecycle endpoints.
 *
 * @property id MongoDB document ID.
 * @property nic National Identity Card number.
 * @property username Account username.
 * @property fullName Full legal name.
 * @property phone Contact phone number.
 * @property role Assigned role (e.g. "Prosumer").
 * @property status Current lifecycle status (e.g. "PendingActivation", "Active", "Deactivated").
 * @property createdAt UTC creation timestamp.
 * @property updatedAt UTC update timestamp.
 */
data class UserResponse(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("nic")
    val nic: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String,

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

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)
