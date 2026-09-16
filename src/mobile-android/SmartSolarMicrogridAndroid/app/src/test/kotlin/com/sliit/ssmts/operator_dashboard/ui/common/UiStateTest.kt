/**
 * Description: Unit test suite for UiState sealed interface verifying state transitions,
 * property extensions, pattern matching exhaustiveness, and error payload propagation.
 */
package com.sliit.ssmts.operator_dashboard.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test verifying UiState behavior and helper extensions.
 */
class UiStateTest {

    /**
     * Verifies Idle state properties.
     */
    @Test
    fun idleState_verifiesProperties() {
        val state: UiState<String> = UiState.Idle
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.dataOrNull())
    }

    /**
     * Verifies Loading state properties.
     */
    @Test
    fun loadingState_verifiesProperties() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
        assertNull(state.dataOrNull())
    }

    /**
     * Verifies Success state properties and payload unwrapping.
     */
    @Test
    fun successState_verifiesPayloadAndExtensions() {
        val payload = "Test Payload"
        val state: UiState<String> = UiState.Success(payload)

        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
        assertEquals(payload, state.dataOrNull())
        assertEquals(payload, (state as UiState.Success).data)
    }

    /**
     * Verifies Error state properties and error code propagation.
     */
    @Test
    fun errorState_verifiesMessageAndCode() {
        val message = "Reservation not found"
        val code = "ERR_RESERVATION_NOT_FOUND"
        val state: UiState<String> = UiState.Error(message = message, errorCode = code)

        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
        assertNull(state.dataOrNull())

        val error = state as UiState.Error
        assertEquals(message, error.message)
        assertEquals(code, error.errorCode)
    }

    /**
     * Verifies exhaustive pattern matching when mapping states.
     */
    @Test
    fun patternMatching_evaluatesExhaustively() {
        val states: List<UiState<Int>> = listOf(
            UiState.Idle,
            UiState.Loading,
            UiState.Success(42),
            UiState.Error("Failed")
        )

        val labels = states.map { state ->
            when (state) {
                is UiState.Idle -> "IDLE"
                is UiState.Loading -> "LOADING"
                is UiState.Success -> "SUCCESS_${state.data}"
                is UiState.Error -> "ERROR_${state.message}"
            }
        }

        assertEquals(listOf("IDLE", "LOADING", "SUCCESS_42", "ERROR_Failed"), labels)
    }
}
