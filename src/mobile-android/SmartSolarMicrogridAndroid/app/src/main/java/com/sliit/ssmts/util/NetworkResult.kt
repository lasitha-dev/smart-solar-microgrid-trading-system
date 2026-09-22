/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Sealed class hierarchy representing async operation and network states.
 */

package com.sliit.ssmts.util

/**
 * Encapsulates the state and payload of a remote network operation.
 *
 * @param T The type of the data returned on success.
 */
sealed class NetworkResult<out T> {

    /**
     * Represents a successful network operation.
     *
     * @property data The encapsulated payload data.
     * @property message An optional server message.
     */
    data class Success<out T>(
        val data: T,
        val message: String? = null
    ) : NetworkResult<T>()

    /**
     * Represents an HTTP error response from the server (e.g. 400, 401, 403, 409).
     *
     * @property statusCode The HTTP status code returned.
     * @property message The descriptive error reason.
     */
    data class Error(
        val statusCode: Int,
        val message: String
    ) : NetworkResult<Nothing>()

    /**
     * Represents a local network connection failure or unexpected exception.
     *
     * @property throwable The exception encountered during execution.
     */
    data class Exception(
        val throwable: Throwable
    ) : NetworkResult<Nothing>()
}
