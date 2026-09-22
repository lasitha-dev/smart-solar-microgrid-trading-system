/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android ViewModel managing authentication, registration, and session state flows.
 */

package com.sliit.ssmts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.domain.repository.IAuthRepository
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed class representing possible UI states for user authentication.
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val session: UserSession, val message: String?) : AuthUiState()
    data class PendingActivation(val nic: String, val message: String) : AuthUiState()
    data class Deactivated(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * Sealed class representing possible UI states for prosumer registration.
 */
sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val user: UserSession, val message: String?) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

/**
 * ViewModel managing Login and Prosumer Registration business flows.
 */
class AuthViewModel(
    private val repository: IAuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    /**
     * Executes login request against backend API and caches session in Room SQLite upon success.
     */
    fun login(identifier: String, password: String) {
        _loginState.value = AuthUiState.Loading

        viewModelScope.launch {
            when (val result = repository.login(identifier, password)) {
                is NetworkResult.Success -> {
                    _loginState.value = AuthUiState.Success(result.data, result.message)
                }
                is NetworkResult.Error -> {
                    val msg = result.message.lowercase()
                    if (result.statusCode == 403 && msg.contains("pending")) {
                        _loginState.value = AuthUiState.PendingActivation(identifier.trim(), result.message)
                    } else if (result.statusCode == 403 && msg.contains("deactivated")) {
                        _loginState.value = AuthUiState.Deactivated(result.message)
                    } else {
                        _loginState.value = AuthUiState.Error(result.message)
                    }
                }
                is NetworkResult.Exception -> {
                    _loginState.value = AuthUiState.Error(
                        result.throwable.localizedMessage ?: "Network connection failed. Please check backend server."
                    )
                }
            }
        }
    }

    /**
     * Executes prosumer registration request against backend API.
     */
    fun registerProsumer(
        nic: String,
        username: String,
        password: String,
        fullName: String,
        phone: String,
        email: String,
        address: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        _registerState.value = RegisterUiState.Loading

        viewModelScope.launch {
            when (val result = repository.registerProsumer(
                nic,
                username,
                password,
                fullName,
                phone,
                email,
                address,
                latitude,
                longitude
            )) {
                is NetworkResult.Success -> {
                    _registerState.value = RegisterUiState.Success(result.data, result.message)
                }
                is NetworkResult.Error -> {
                    _registerState.value = RegisterUiState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    _registerState.value = RegisterUiState.Error(
                        result.throwable.localizedMessage ?: "Failed to connect to server. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Verifies if an active session exists in local SQLite storage.
     */
    fun checkExistingSession(onSessionFound: (UserSession) -> Unit) {
        viewModelScope.launch {
            val session = repository.getActiveSession()
            if (session != null && session.token.isNotBlank() && session.status == Constants.STATUS_ACTIVE) {
                onSessionFound(session)
            }
        }
    }

    /**
     * Resets authentication UI states.
     */
    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    /**
     * Resets registration UI states.
     */
    fun resetRegisterState() {
        _registerState.value = RegisterUiState.Idle
    }
}
