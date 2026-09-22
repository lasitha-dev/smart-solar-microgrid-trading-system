/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Global constants for networking endpoints, preferences keys, and intent parameters.
 */

package com.sliit.ssmts.util

/**
 * Application-wide constants.
 */
object Constants {
    /**
     * Default base URL for connecting to the local ASP.NET Core Web API from Android Emulator.
     * Note: '10.0.2.2' maps directly to '127.0.0.1' on the host machine.
     */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"

    /**
     * SharedPreferences file name.
     */
    const val PREFS_NAME = "ssmts_app_prefs"

    /**
     * Preference key for custom server base URL override.
     */
    const val KEY_CUSTOM_BASE_URL = "key_custom_base_url"

    /**
     * Intent Extra key for passing prosumer NIC between screens.
     */
    const val EXTRA_NIC = "extra_nic"

    /**
     * Intent Extra key for passing status message banners.
     */
    const val EXTRA_STATUS_MESSAGE = "extra_status_message"

    /**
     * Intent Extra keys for passing facility coordinates to/from Location Picker.
     */
    const val EXTRA_LATITUDE = "extra_latitude"
    const val EXTRA_LONGITUDE = "extra_longitude"
    const val EXTRA_READ_ONLY = "extra_read_only"

    /**
     * Roles
     */
    const val ROLE_PROSUMER = "Prosumer"
    const val ROLE_GRID_OPERATOR = "GridOperator"
    const val ROLE_BACKOFFICE = "Backoffice"

    /**
     * Statuses
     */
    const val STATUS_ACTIVE = "Active"
    const val STATUS_PENDING_ACTIVATION = "PendingActivation"
    const val STATUS_DEACTIVATED = "Deactivated"
}
