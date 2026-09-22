/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android Activity displaying informative notice when prosumer registration is awaiting Backoffice approval.
 */

package com.sliit.ssmts.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sliit.ssmts.R
import com.sliit.ssmts.data.repository.AuthRepositoryImpl
import com.sliit.ssmts.databinding.ActivityPendingActivationBinding
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.NetworkResult
import kotlinx.coroutines.launch

/**
 * Screen presented when an account has 'PendingActivation' status.
 * Explains that registration is pending Backoffice administrator review.
 */
class PendingActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingActivationBinding
    private var registeredNic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registeredNic = intent.getStringExtra(Constants.EXTRA_NIC).orEmpty()
        val customMessage = intent.getStringExtra(Constants.EXTRA_STATUS_MESSAGE)

        if (registeredNic.isNotBlank()) {
            binding.tvRegisteredNic.text = registeredNic
        } else {
            binding.tvRegisteredNic.text = getString(R.string.pending_badge)
        }

        if (!customMessage.isNullOrBlank()) {
            binding.tvStatusResult.text = customMessage
            binding.tvStatusResult.visibility = View.VISIBLE
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCheckStatus.setOnClickListener {
            checkActivationStatus()
        }

        binding.btnBackToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun checkActivationStatus() {
        if (registeredNic.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_still_pending), Toast.LENGTH_SHORT).show()
            return
        }

        binding.pbCheckStatus.visibility = View.VISIBLE
        binding.btnCheckStatus.isEnabled = false
        binding.tvStatusResult.visibility = View.GONE

        lifecycleScope.launch {
            val repository = AuthRepositoryImpl(applicationContext)
            val result = repository.getProfile(registeredNic)

            binding.pbCheckStatus.visibility = View.GONE
            binding.btnCheckStatus.isEnabled = true

            when (result) {
                is NetworkResult.Success -> {
                    val status = result.data.status
                    if (status.equals(Constants.STATUS_ACTIVE, ignoreCase = true)) {
                        Toast.makeText(
                            this@PendingActivationActivity,
                            getString(R.string.msg_account_active_now),
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToLogin()
                    } else {
                        binding.tvStatusResult.text = getString(R.string.msg_still_pending)
                        binding.tvStatusResult.visibility = View.VISIBLE
                    }
                }
                is NetworkResult.Error -> {
                    binding.tvStatusResult.text = getString(R.string.msg_still_pending)
                    binding.tvStatusResult.visibility = View.VISIBLE
                }
                is NetworkResult.Exception -> {
                    binding.tvStatusResult.text = getString(R.string.error_network_connection)
                    binding.tvStatusResult.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
