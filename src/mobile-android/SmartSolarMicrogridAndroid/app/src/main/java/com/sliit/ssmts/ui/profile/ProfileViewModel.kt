/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: ViewModel managing Prosumer profile viewing, editing, deactivation, and logout workflows.
 */

package com.sliit.ssmts.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.domain.repository.IAuthRepository
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for profile data loading.
 */
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val session: UserSession) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/**
 * UI state for profile update operations.
 */
sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val message: String) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

/**
 * UI state for account deactivation requests.
 */
sealed class DeactivationState {
    object Idle : DeactivationState()
    object Loading : DeactivationState()
    data class Success(val message: String) : DeactivationState()
    data class Error(val message: String) : DeactivationState()
}

/**
 * UI state for change password operations.
 */
sealed class ChangePasswordState {
    object Idle : ChangePasswordState()
    object Loading : ChangePasswordState()
    data class Success(val message: String) : ChangePasswordState()
    data class Error(val message: String) : ChangePasswordState()
}

/**
 * UI state for permanent account deletion.
 */
sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    data class Success(val message: String) : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

/**
 * ViewModel orchestrating profile management, password changing, and self-account deletion.
 */
class ProfileViewModel(
    private val repository: IAuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    private val _deactivationState = MutableStateFlow<DeactivationState>(DeactivationState.Idle)
    val deactivationState: StateFlow<DeactivationState> = _deactivationState.asStateFlow()

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * Loads the profile from SQLite local storage, then syncs with remote Web API.
     */
    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading

            // 1. Read cached session from SQLite
            val cachedSession = repository.getActiveSession()
            if (cachedSession != null) {
                _profileState.value = ProfileUiState.Success(cachedSession)
            }

            // 2. Fetch fresh profile from API
            if (cachedSession != null && cachedSession.nic.isNotBlank()) {
                when (val result = repository.getProfile(cachedSession.nic)) {
                    is NetworkResult.Success -> {
                        _profileState.value = ProfileUiState.Success(result.data)
                    }
                    is NetworkResult.Error -> {
                        // Keep displaying cached SQLite session if API returns an error
                    }
                    is NetworkResult.Exception -> {
                        // Keep displaying cached SQLite session if connection fails
                    }
                }
            } else if (cachedSession == null) {
                _profileState.value = ProfileUiState.Error("No active session found. Please log in.")
            }
        }
    }

    /**
     * Updates permitted profile fields (fullName, phone, address) on the backend server.
     */
    fun updateProfile(fullName: String, phone: String, address: String) {
        viewModelScope.launch {
            val session = (_profileState.value as? ProfileUiState.Success)?.session
                ?: repository.getActiveSession()

            if (session == null || session.nic.isBlank()) {
                _updateState.value = ProfileUpdateState.Error("Session not loaded. Please log in again.")
                return@launch
            }

            if (session.status.equals("Deactivated", ignoreCase = true)) {
                _updateState.value = ProfileUpdateState.Error("Deactivated accounts cannot modify profile information.")
                return@launch
            }

            _updateState.value = ProfileUpdateState.Loading

            when (val result = repository.updateProfile(session.nic, fullName, phone, address)) {
                is NetworkResult.Success -> {
                    _profileState.value = ProfileUiState.Success(result.data)
                    _updateState.value = ProfileUpdateState.Success(result.message ?: "Profile updated successfully.")
                }
                is NetworkResult.Error -> {
                    _updateState.value = ProfileUpdateState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    _updateState.value = ProfileUpdateState.Error(
                        result.throwable.localizedMessage ?: "Network error updating profile."
                    )
                }
            }
        }
    }

    /**
     * Sends account self-deactivation request to the server and retains session with Deactivated status.
     */
    fun requestDeactivation(reason: String?, remarks: String?) {
        val currentSession = (_profileState.value as? ProfileUiState.Success)?.session
        if (currentSession == null) {
            _deactivationState.value = DeactivationState.Error("Session not loaded.")
            return
        }

        if (currentSession.status.equals("Deactivated", ignoreCase = true)) {
            _deactivationState.value = DeactivationState.Error("Your account is already deactivated.")
            return
        }

        _deactivationState.value = DeactivationState.Loading

        viewModelScope.launch {
            when (val result = repository.requestDeactivation(currentSession.nic, reason, remarks)) {
                is NetworkResult.Success -> {
                    _profileState.value = ProfileUiState.Success(result.data)
                    _deactivationState.value = DeactivationState.Success(
                        result.message ?: "Account deactivated successfully."
                    )
                }
                is NetworkResult.Error -> {
                    _deactivationState.value = DeactivationState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    _deactivationState.value = DeactivationState.Error(
                        result.throwable.localizedMessage ?: "Error processing deactivation."
                    )
                }
            }
        }
    }

    /**
     * Clears local SQLite session on logout.
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.clearSession()
            onLoggedOut()
        }
    }

    /**
     * Changes password via remote API and clears local session on success.
     */
     fun changePassword(current: String, newPass: String, confirmPass: String) {
         val currentSession = (_profileState.value as? ProfileUiState.Success)?.session
         if (currentSession != null && currentSession.status.equals("Deactivated", ignoreCase = true)) {
             _changePasswordState.value = ChangePasswordState.Error("Deactivated accounts cannot change password.")
             return
         }

         viewModelScope.launch {
             _changePasswordState.value = ChangePasswordState.Loading
             when (val result = repository.changePassword(current, newPass, confirmPass)) {
                 is NetworkResult.Success -> {
                     _changePasswordState.value = ChangePasswordState.Success(
                         result.message ?: "Password changed successfully."
                     )
                 }
                 is NetworkResult.Error -> {
                     _changePasswordState.value = ChangePasswordState.Error(result.message)
                 }
                 is NetworkResult.Exception -> {
                     _changePasswordState.value = ChangePasswordState.Error(
                         result.throwable.localizedMessage ?: "Network error changing password."
                     )
                 }
             }
         }
     }

    /**
     * Sends account deletion request to the server and clears session on success.
     */
    fun deleteAccount(confirmEmail: String) {
        val currentSession = (_profileState.value as? ProfileUiState.Success)?.session
        if (currentSession != null && currentSession.status.equals("Deactivated", ignoreCase = true)) {
            _deleteAccountState.value = DeleteAccountState.Error("Deactivated accounts cannot delete account.")
            return
        }

        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            when (val result = repository.deleteAccount(confirmEmail)) {
                is NetworkResult.Success -> {
                    _deleteAccountState.value = DeleteAccountState.Success(
                        result.message ?: "Your account has been permanently deleted."
                    )
                }
                is NetworkResult.Error -> {
                    _deleteAccountState.value = DeleteAccountState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    _deleteAccountState.value = DeleteAccountState.Error(
                        result.throwable.localizedMessage ?: "Network error deleting account."
                    )
                }
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}
