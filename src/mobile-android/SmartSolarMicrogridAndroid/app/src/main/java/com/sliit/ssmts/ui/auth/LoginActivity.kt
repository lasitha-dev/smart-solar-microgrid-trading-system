/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android Activity providing authentication for Prosumers and Grid Operators.
 */

package com.sliit.ssmts.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityLoginBinding
import com.sliit.ssmts.ui.ViewModelFactory
import com.sliit.ssmts.ui.home.HomeActivity
import com.sliit.ssmts.ui.profile.ProfileActivity
import com.sliit.ssmts.util.Constants
import kotlinx.coroutines.launch

/**
 * Login screen supporting username/NIC credentials authentication.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels { ViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()

        // Auto-login if valid active session exists in SQLite
        viewModel.checkExistingSession {
            routeAfterLogin()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun attemptLogin() {
        binding.tilIdentifier.error = null
        binding.tilPassword.error = null
        binding.tvErrorMessage.visibility = View.GONE

        val identifier = binding.etIdentifier.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var hasError = false

        if (identifier.isBlank()) {
            binding.tilIdentifier.error = getString(R.string.error_identifier_required)
            hasError = true
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            hasError = true
        }

        if (!hasError) {
            viewModel.login(identifier, password)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> {
                            setLoading(false)
                        }
                        is AuthUiState.Loading -> {
                            setLoading(true)
                        }
                        is AuthUiState.Success -> {
                            setLoading(false)
                            Toast.makeText(
                                this@LoginActivity,
                                state.message ?: getString(R.string.login_title),
                                Toast.LENGTH_SHORT
                            ).show()
                            routeAfterLogin()
                        }
                        is AuthUiState.PendingActivation -> {
                            setLoading(false)
                            val intent = Intent(this@LoginActivity, PendingActivationActivity::class.java).apply {
                                putExtra(Constants.EXTRA_NIC, state.nic)
                                putExtra(Constants.EXTRA_STATUS_MESSAGE, state.message)
                            }
                            startActivity(intent)
                            viewModel.resetLoginState()
                        }
                        is AuthUiState.Deactivated -> {
                            setLoading(false)
                            showErrorBanner(state.message)
                        }
                        is AuthUiState.Error -> {
                            setLoading(false)
                            showErrorBanner(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etIdentifier.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun showErrorBanner(message: String) {
        binding.tvErrorMessage.text = message
        binding.tvErrorMessage.visibility = View.VISIBLE
    }

    private fun routeAfterLogin() {
        lifecycleScope.launch {
            val session = com.sliit.ssmts.util.SessionManager(this@LoginActivity).getActiveSession()
            if (session?.role.equals("GridOperator", ignoreCase = true)) {
                navigateToOperatorDashboard()
            } else {
                navigateToHome()
            }
        }
    }

    private fun navigateToOperatorDashboard() {
        val intent = Intent(this, com.sliit.ssmts.operator_dashboard.ui.dashboard.OperatorDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
