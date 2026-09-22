/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Unit test suite verifying AuthViewModel authentication, registration, and state flow transitions.
 */

package com.sliit.ssmts.ui.auth

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
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_WithValidCredentials_EmitsSuccessState() = runTest(testDispatcher) {
        // Arrange
        val expectedSession = UserSession(
            token = "jwt_auth_token_mock",
            userId = "usr_001",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            role = "Prosumer",
            status = Constants.STATUS_ACTIVE
        )
        fakeRepository.loginResult = NetworkResult.Success(expectedSession, "Login successful")

        // Act
        viewModel.login("prosumer_john", "Password123!")
        advanceUntilIdle()

        // Assert
        val state = viewModel.loginState.value
        assertTrue("Expected AuthUiState.Success but got $state", state is AuthUiState.Success)
        val success = state as AuthUiState.Success
        assertEquals("jwt_auth_token_mock", success.session.token)
        assertEquals("prosumer_john", success.session.username)
        assertEquals(Constants.STATUS_ACTIVE, success.session.status)
    }

    @Test
    fun login_WithInvalidCredentials_EmitsErrorState() = runTest(testDispatcher) {
        // Arrange
        fakeRepository.loginResult = NetworkResult.Error(401, "Invalid username/NIC or password.")

        // Act
        viewModel.login("prosumer_john", "WrongPassword!")
        advanceUntilIdle()

        // Assert
        val state = viewModel.loginState.value
        assertTrue("Expected AuthUiState.Error but got $state", state is AuthUiState.Error)
        assertEquals("Invalid username/NIC or password.", (state as AuthUiState.Error).message)
    }

    @Test
    fun login_WithPendingActivationStatus_EmitsPendingActivationState() = runTest(testDispatcher) {
        // Arrange
        fakeRepository.loginResult = NetworkResult.Error(
            403,
            "Your account is currently pending activation. Please wait for Backoffice administrator approval."
        )

        // Act
        viewModel.login("199512345678", "Password123!")
        advanceUntilIdle()

        // Assert
        val state = viewModel.loginState.value
        assertTrue("Expected AuthUiState.PendingActivation but got $state", state is AuthUiState.PendingActivation)
        val pending = state as AuthUiState.PendingActivation
        assertEquals("199512345678", pending.nic)
        assertTrue(pending.message.contains("pending activation"))
    }

    @Test
    fun login_WithDeactivatedStatus_EmitsDeactivatedState() = runTest(testDispatcher) {
        // Arrange
        fakeRepository.loginResult = NetworkResult.Error(
            403,
            "Your account has been deactivated. Please contact support."
        )

        // Act
        viewModel.login("prosumer_john", "Password123!")
        advanceUntilIdle()

        // Assert
        val state = viewModel.loginState.value
        assertTrue("Expected AuthUiState.Deactivated but got $state", state is AuthUiState.Deactivated)
        val deactivated = state as AuthUiState.Deactivated
        assertTrue(deactivated.message.contains("deactivated"))
    }

    @Test
    fun registerProsumer_WithValidDetails_EmitsSuccessState() = runTest(testDispatcher) {
        // Arrange
        val registeredUser = UserSession(
            token = "",
            userId = "usr_002",
            nic = "200012345678",
            username = "new_prosumer",
            fullName = "New Prosumer",
            role = "Prosumer",
            status = Constants.STATUS_PENDING_ACTIVATION
        )
        fakeRepository.registerResult = NetworkResult.Success(
            registeredUser,
            "Prosumer registered successfully. Your account is pending Backoffice approval."
        )

        // Act
        viewModel.registerProsumer(
            nic = "200012345678",
            username = "new_prosumer",
            password = "Password123!",
            fullName = "New Prosumer",
            phone = "0771234567",
            email = "new@microgrid.lk",
            address = "No 12, Colombo",
            latitude = 6.9271,
            longitude = 79.8612
        )
        advanceUntilIdle()

        // Assert
        val state = viewModel.registerState.value
        assertTrue("Expected RegisterUiState.Success but got $state", state is RegisterUiState.Success)
        val success = state as RegisterUiState.Success
        assertEquals("200012345678", success.user.nic)
        assertEquals(Constants.STATUS_PENDING_ACTIVATION, success.user.status)
    }

    @Test
    fun registerProsumer_WithDuplicateNICorEmail_EmitsErrorState() = runTest(testDispatcher) {
        // Arrange
        fakeRepository.registerResult = NetworkResult.Error(
            409,
            "A user with NIC '199512345678' is already registered in the system."
        )

        // Act
        viewModel.registerProsumer(
            nic = "199512345678",
            username = "duplicate_user",
            password = "Password123!",
            fullName = "Duplicate User",
            phone = "0771234567",
            email = "dup@microgrid.lk",
            address = "No 12, Colombo"
        )
        advanceUntilIdle()

        // Assert
        val state = viewModel.registerState.value
        assertTrue("Expected RegisterUiState.Error but got $state", state is RegisterUiState.Error)
        assertEquals("A user with NIC '199512345678' is already registered in the system.", (state as RegisterUiState.Error).message)
    }

    @Test
    fun checkExistingSession_WhenActiveSessionPresent_TriggersCallback() = runTest(testDispatcher) {
        // Arrange
        val activeSession = UserSession(
            token = "persisted_jwt_token",
            userId = "usr_001",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            role = "Prosumer",
            status = Constants.STATUS_ACTIVE
        )
        fakeRepository.currentActiveSession = activeSession
        var callbackInvoked = false
        var capturedSession: UserSession? = null

        // Act
        viewModel.checkExistingSession { session ->
            callbackInvoked = true
            capturedSession = session
        }
        advanceUntilIdle()

        // Assert
        assertTrue(callbackInvoked)
        assertEquals("199512345678", capturedSession?.nic)
    }

    @Test
    fun resetLoginState_And_resetRegisterState_ResetsToIdle() = runTest(testDispatcher) {
        // Arrange
        fakeRepository.loginResult = NetworkResult.Error(401, "Error")
        viewModel.login("a", "b")
        advanceUntilIdle()
        assertTrue(viewModel.loginState.value is AuthUiState.Error)

        // Act
        viewModel.resetLoginState()
        viewModel.resetRegisterState()

        // Assert
        assertEquals(AuthUiState.Idle, viewModel.loginState.value)
        assertEquals(RegisterUiState.Idle, viewModel.registerState.value)
    }
}
