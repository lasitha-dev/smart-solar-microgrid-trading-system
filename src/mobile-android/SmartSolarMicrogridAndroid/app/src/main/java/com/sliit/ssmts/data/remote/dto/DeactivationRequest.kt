/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Data transfer object for prosumer account self-deactivation requests.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload submitted to PATCH /api/prosumers/{nic}/request-deactivation.
 *
 * @property reason Optional stated reason for account deactivation.
 * @property remarks Optional feedback comments.
 */
data class DeactivationRequest(
    @SerializedName("reason")
    val reason: String? = null,

    @SerializedName("remarks")
    val remarks: String? = null
)
