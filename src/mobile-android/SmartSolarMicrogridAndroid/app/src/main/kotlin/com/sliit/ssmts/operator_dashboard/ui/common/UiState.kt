/**
 * Description: Generic sealed interface representing reactive UI states across the Member 4 subsystem.
 */
package com.sliit.ssmts.operator_dashboard.ui.common

/**
 * Encapsulates the UI state representation for reactive ViewModels adhering to the Open/Closed Principle.
 *
 * @param T The type of data encapsulated by a successful state.
 */
sealed interface UiState<out T> {

    /**
     * Initial uninitialized or resting state prior to operation invocation.
     */
    data object Idle : UiState<Nothing>

    /**
     * In-flight asynchronous processing state.
     */
    data object Loading : UiState<Nothing>

    /**
     * Successful completion of an operation encapsulating the produced data payload.
     *
     * @property data The domain or presentation payload.
     */
    data class Success<out T>(val data: T) : UiState<T>

    /**
     * Failure state containing an explanatory message and optional structured error code.
     *
     * @property message Human-readable error explanation for user display.
     * @property errorCode Optional taxonomy error code (e.g., ERR_RESERVATION_NOT_FOUND).
     */
    data class Error(
        val message: String,
        val errorCode: String? = null
    ) : UiState<Nothing>
}

/**
 * True if this state instance represents Loading.
 */
val <T> UiState<T>.isLoading: Boolean
    get() = this is UiState.Loading

/**
 * True if this state instance represents Success.
 */
val <T> UiState<T>.isSuccess: Boolean
    get() = this is UiState.Success

/**
 * True if this state instance represents Error.
 */
val <T> UiState<T>.isError: Boolean
    get() = this is UiState.Error

/**
 * Returns the encapsulated data payload if Success, or null otherwise.
 */
fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data
