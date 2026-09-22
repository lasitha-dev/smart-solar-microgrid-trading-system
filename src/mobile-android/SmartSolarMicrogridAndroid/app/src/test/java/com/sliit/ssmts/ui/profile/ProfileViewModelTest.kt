/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Unit test suite verifying ProfileViewModel profile management, password changing, and account deletion flows.
 */

package com.sliit.ssmts.ui.profile

import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.testutil.FakeAuthRepository
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var sampleSession: UserSession

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        sampleSession = UserSession(
            token = "jwt_profile_token",
            userId = "usr_001",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            phone = "0771234567",
            email = "john@microgrid.lk",
            address = "No 10, Colombo",
            latitude = 6.9271,
            longitude = 79.8612,
            role = "Prosumer",
            status = Constants.STATUS_ACTIVE
        )
        fakeRepository.currentActiveSession = sampleSession
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_LoadsCachedSessionAndEmitsSuccessState() = runTest(testDispatcher) {
        // Act
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        // Assert
        val state = viewModel.profileState.value
        assertTrue("Expected ProfileUiState.Success but got $state", state is ProfileUiState.Success)
        val success = state as ProfileUiState.Success
        assertEquals("John Silva", success.session.fullName)
        assertEquals("199512345678", success.session.nic)
    }

    @Test
    fun updateProfile_WithValidDetails_UpdatesProfileStateAndEmitsSuccess() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        val updatedSession = sampleSession.copy(
            fullName = "John Silva Updated",
            phone = "0779998888",
            address = "No 25, Kandy Road"
        )
        fakeRepository.updateProfileResult = NetworkResult.Success(updatedSession, "Profile updated successfully.")

        // Act
        viewModel.updateProfile("John Silva Updated", "0779998888", "No 25, Kandy Road")
        advanceUntilIdle()

        // Assert
        val updateState = viewModel.updateState.value
        assertTrue("Expected ProfileUpdateState.Success but got $updateState", updateState is ProfileUpdateState.Success)

        val profileState = viewModel.profileState.value
        assertTrue("Expected ProfileUiState.Success but got $profileState", profileState is ProfileUiState.Success)
        assertEquals("John Silva Updated", (profileState as ProfileUiState.Success).session.fullName)
    }

    @Test
    fun updateProfile_WithNetworkError_EmitsUpdateErrorState() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.updateProfileResult = NetworkResult.Error(400, "Validation failed for phone number.")

        // Act
        viewModel.updateProfile("John Silva", "invalid_phone", "No 10, Colombo")
        advanceUntilIdle()

        // Assert
        val updateState = viewModel.updateState.value
        assertTrue("Expected ProfileUpdateState.Error but got $updateState", updateState is ProfileUpdateState.Error)
        assertEquals("Validation failed for phone number.", (updateState as ProfileUpdateState.Error).message)
    }

    @Test
    fun requestDeactivation_WithValidReason_EmitsSuccessAndClearsSession() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        val deactivatedSession = sampleSession.copy(status = Constants.STATUS_DEACTIVATED)
        fakeRepository.deactivationResult = NetworkResult.Success(
            deactivatedSession,
            "Account self-deactivation request completed successfully."
        )

        // Act
        viewModel.requestDeactivation("Relocating solar facility", "No longer generating power")
        advanceUntilIdle()

        // Assert
        val deactivationState = viewModel.deactivationState.value
        assertTrue("Expected DeactivationState.Success but got $deactivationState", deactivationState is DeactivationState.Success)
        assertTrue(fakeRepository.clearSessionCalled)
    }

    @Test
    fun changePassword_WithValidCredentials_EmitsSuccessState() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.changePasswordResult = NetworkResult.Success(
            sampleSession,
            "Password changed successfully. Please sign in with your new password."
        )

        // Act
        viewModel.changePassword("OldPassword123!", "NewPassword123!@#", "NewPassword123!@#")
        advanceUntilIdle()

        // Assert
        val changePasswordState = viewModel.changePasswordState.value
        assertTrue("Expected ChangePasswordState.Success but got $changePasswordState", changePasswordState is ChangePasswordState.Success)
        assertEquals(
            "Password changed successfully. Please sign in with your new password.",
            (changePasswordState as ChangePasswordState.Success).message
        )
    }

    @Test
    fun changePassword_WithInvalidCurrentPassword_EmitsErrorState() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.changePasswordResult = NetworkResult.Error(
            400,
            "The current password you entered is incorrect."
        )

        // Act
        viewModel.changePassword("WrongCurrentPass!", "NewPassword123!@#", "NewPassword123!@#")
        advanceUntilIdle()

        // Assert
        val changePasswordState = viewModel.changePasswordState.value
        assertTrue("Expected ChangePasswordState.Error but got $changePasswordState", changePasswordState is ChangePasswordState.Error)
        assertEquals(
            "The current password you entered is incorrect.",
            (changePasswordState as ChangePasswordState.Error).message
        )
    }

    @Test
    fun deleteAccount_WithMatchingEmail_EmitsSuccessState() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.deleteAccountResult = NetworkResult.Success(
            Unit,
            "Your account has been deleted successfully."
        )

        // Act
        viewModel.deleteAccount("john@microgrid.lk")
        advanceUntilIdle()

        // Assert
        val deleteAccountState = viewModel.deleteAccountState.value
        assertTrue("Expected DeleteAccountState.Success but got $deleteAccountState", deleteAccountState is DeleteAccountState.Success)
        assertEquals(
            "Your account has been deleted successfully.",
            (deleteAccountState as DeleteAccountState.Success).message
        )
    }

    @Test
    fun deleteAccount_WithMismatchedEmail_EmitsErrorState() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.deleteAccountResult = NetworkResult.Error(
            400,
            "The confirmation email you entered does not match your registered email address."
        )

        // Act
        viewModel.deleteAccount("wrong_email@microgrid.lk")
        advanceUntilIdle()

        // Assert
        val deleteAccountState = viewModel.deleteAccountState.value
        assertTrue("Expected DeleteAccountState.Error but got $deleteAccountState", deleteAccountState is DeleteAccountState.Error)
        assertEquals(
            "The confirmation email you entered does not match your registered email address.",
            (deleteAccountState as DeleteAccountState.Error).message
        )
    }

    @Test
    fun logout_ClearsRepositorySessionAndInvokesCallback() = runTest(testDispatcher) {
        // Arrange
        val viewModel = ProfileViewModel(fakeRepository)
        advanceUntilIdle()

        var loggedOutCallbackInvoked = false

        // Act
        viewModel.logout {
            loggedOutCallbackInvoked = true
        }
        advanceUntilIdle()

        // Assert
        assertTrue(loggedOutCallbackInvoked)
        assertTrue(fakeRepository.clearSessionCalled)
    }
}
