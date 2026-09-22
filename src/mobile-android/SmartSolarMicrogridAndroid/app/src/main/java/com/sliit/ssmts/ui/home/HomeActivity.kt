/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android Activity providing the Prosumer Home Dashboard with quick navigation to Profile and microgrid overview.
 */

package com.sliit.ssmts.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityHomeBinding
import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.ui.ViewModelFactory
import com.sliit.ssmts.ui.auth.LoginActivity
import com.sliit.ssmts.ui.profile.ProfileActivity
import com.sliit.ssmts.ui.profile.ProfileUiState
import com.sliit.ssmts.ui.profile.ProfileViewModel
import com.sliit.ssmts.util.Constants
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Prosumer Home Dashboard.
 * Serves as the landing page after successful prosumer authentication.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: ProfileViewModel by viewModels { ViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile data in case it was updated on ProfileActivity
        viewModel.loadProfile()
    }

    private fun setupListeners() {
        // Navigate to Profile Activity
        binding.cardNavigateProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Member 4 Operator Dashboard Console
        binding.cardNavigateOperatorConsole.setOnClickListener {
            val intent = Intent(this, com.sliit.ssmts.operator_dashboard.ui.dashboard.OperatorDashboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading -> {}
                        is ProfileUiState.Success -> {
                            populateHomeData(state.session)
                        }
                        is ProfileUiState.Error -> {
                            // If session invalid, redirect to login
                            if (state.message.contains("No active session", ignoreCase = true)) {
                                val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateHomeData(session: UserSession) {
        binding.tvWelcomeName.text = if (session.fullName.isNotBlank()) session.fullName else session.username
        binding.tvHomeNic.text = "NIC: ${session.nic}"
        binding.tvHomeStatusBadge.text = session.status.uppercase()

        // Dynamic Role Badge
        val isOperatorOrAdmin = session.role.equals("GridOperator", ignoreCase = true) ||
                session.role.equals("Administrator", ignoreCase = true) ||
                session.role.equals("Backoffice", ignoreCase = true)

        binding.cardNavigateOperatorConsole.visibility = if (isOperatorOrAdmin) android.view.View.VISIBLE else android.view.View.GONE

        binding.tvHomeRoleBadge.text = when (session.role) {
            "GridOperator" -> "GRID OPERATOR NODE"
            "Backoffice" -> "BACKOFFICE OFFICER"
            "Administrator" -> "SYSTEM ADMINISTRATOR"
            else -> getString(R.string.home_role_badge)
        }

        if (session.status.equals(Constants.STATUS_ACTIVE, ignoreCase = true)) {
            binding.tvHomeStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvHomeStatusBadge.setTextColor(getColor(R.color.status_active_text))
        } else {
            binding.tvHomeStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvHomeStatusBadge.setTextColor(getColor(R.color.status_pending_text))
        }
    }
}
