/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Request DTO for self-account deletion requiring email confirmation.
 */

package com.sliit.ssmts.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Payload sent to delete the authenticated user's account.
 */
data class DeleteAccountRequest(
    @SerializedName("confirmEmail")
    val confirmEmail: String
)
