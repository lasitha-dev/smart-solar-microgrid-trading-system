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
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PASSWORD_COMPLEX_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{6,}$")

    /**
     * Validates email address format.
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return EMAIL_PATTERN.matcher(email.trim()).matches()
    }

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
     * Validates password strength (minimum 6 characters, at least 1 uppercase, 1 lowercase, 1 number, and 1 symbol).
     */
    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        return PASSWORD_COMPLEX_PATTERN.matcher(password).matches()
    }

    fun hasMinLength(password: String?): Boolean = !password.isNullOrEmpty() && password.length >= 6
    fun hasUppercase(password: String?): Boolean = !password.isNullOrEmpty() && password.any { it.isUpperCase() }
    fun hasLowercase(password: String?): Boolean = !password.isNullOrEmpty() && password.any { it.isLowerCase() }
    fun hasNumber(password: String?): Boolean = !password.isNullOrEmpty() && password.any { it.isDigit() }
    fun hasSymbol(password: String?): Boolean = !password.isNullOrEmpty() && password.any { !it.isLetterOrDigit() }
}
