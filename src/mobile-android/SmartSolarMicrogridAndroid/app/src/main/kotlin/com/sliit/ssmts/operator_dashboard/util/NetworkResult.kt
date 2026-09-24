/**
 * Description: Sealed hierarchy representing network and repository operation results (Success, Error, Exception).
 */
package com.sliit.ssmts.operator_dashboard.util

/**
 * Description: Encapsulates asynchronous network responses with type-safe outcome branching.
 */
sealed class NetworkResult<out T> {

    /**
     * Represents a successful network or database operation carrying the returned domain payload.
     */
    data class Success<out T>(val data: T) : NetworkResult<T>()

    /**
     * Represents an API error response containing an optional machine-readable error code and human-readable message.
     */
    data class Error(val code: String? = null, val message: String) : NetworkResult<Nothing>()

    /**
     * Represents an unexpected execution exception such as network timeout or parsing failure.
     */
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()
}
