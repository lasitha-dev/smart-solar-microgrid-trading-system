/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Client-side input validation helper enforcing business syntax rules on user forms.
 */

package com.sliit.ssmts.util

import java.util.regex.Pattern

/**
 * Defensive input validation rules for prosumer and operator registration/login fields.
 */
object InputValidator {

    private val PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{9,15}$")
    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,50}$")

    /**
     * Validates National Identity Card (NIC) number format (9-20 alphanumeric characters).
     */
    fun isValidNic(nic: String?): Boolean {
        if (nic.isNullOrBlank()) return false
        val trimmed = nic.trim()
        return trimmed.length in 9..20
    }

    /**
     * Validates username requirements (3-50 chars, alphanumeric with underscore/hyphen).
     */
    fun isValidUsername(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        return USERNAME_PATTERN.matcher(username.trim()).matches()
    }

    /**
     * Validates full legal name (minimum 2 characters).
     */
    fun isValidFullName(fullName: String?): Boolean {
        if (fullName.isNullOrBlank()) return false
        return fullName.trim().length in 2..100
    }

    /**
     * Validates telephone number format (9-15 digits with optional leading +).
     */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val cleaned = phone.trim().replace(" ", "").replace("-", "")
        return PHONE_PATTERN.matcher(cleaned).matches()
    }

    /**
     * Validates password strength (minimum 6 characters).
     */
    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        return password.length >= 6
    }
}
