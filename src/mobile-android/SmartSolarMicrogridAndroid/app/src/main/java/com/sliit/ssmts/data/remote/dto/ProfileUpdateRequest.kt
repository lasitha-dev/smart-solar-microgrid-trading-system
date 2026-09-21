/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for updating permitted prosumer profile fields.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload submitted to PUT /api/prosumers/{nic}.
 *
 * @property fullName Updated full legal name.
 * @property phone Updated contact phone number.
 */
data class ProfileUpdateRequest(
    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("address")
    val address: String
)
