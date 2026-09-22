/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android Activity providing the Solar Prosumer registration form with Interactive Map Location Selection.
 */

package com.sliit.ssmts.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityRegisterBinding
import com.sliit.ssmts.ui.ViewModelFactory
import com.sliit.ssmts.ui.location.LocationPickerActivity
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.InputValidator
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Prosumer self-registration activity.
 * Incorporates address and facility location coordinates selection via dedicated interactive Map.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels { ViewModelFactory(this) }

    // Facility Location Coordinates State
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var isLocationConfirmed: Boolean = false

    // Activity Result Launcher for Interactive Location Picker Screen
    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val lat = result.data?.getDoubleExtra(Constants.EXTRA_LATITUDE, Double.NaN) ?: Double.NaN
            val lon = result.data?.getDoubleExtra(Constants.EXTRA_LONGITUDE, Double.NaN) ?: Double.NaN

            if (!lat.isNaN() && !lon.isNaN() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                selectedLatitude = lat
                selectedLongitude = lon
                isLocationConfirmed = true
                updateLocationUi()
                binding.tvErrorMessage.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupLiveValidationWatchers()
        observeViewModel()
        updateLocationUi()
    }

    private fun setupListeners() {
        binding.btnSelectLocationOnMap.setOnClickListener {
            openLocationPickerScreen()
        }

        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            finish()
        }
    }

    /**
     * Attaches real-time TextWatchers to enforce live validation on Password and Confirm Password fields.
     */
    private fun setupLiveValidationWatchers() {
        val passwordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pass = s?.toString().orEmpty()
                val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

                if (pass.isNotEmpty()) {
                    if (pass.length < 6) {
                        binding.tilPassword.error = getString(R.string.error_password_short)
                    } else if (!InputValidator.isValidPassword(pass)) {
                        binding.tilPassword.error = getString(R.string.error_password_complex)
                    } else {
                        binding.tilPassword.error = null
                    }
                } else {
                    binding.tilPassword.error = null
                }

                if (confirm.isNotEmpty()) {
                    if (pass != confirm) {
                        binding.tilConfirmPassword.error = getString(R.string.error_passwords_mismatch)
                    } else {
                        binding.tilConfirmPassword.error = null
                    }
                }
            }
        }

        val confirmPasswordWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val confirm = s?.toString().orEmpty()
                val pass = binding.etPassword.text?.toString().orEmpty()

                if (confirm.isNotEmpty()) {
                    if (confirm != pass) {
                        binding.tilConfirmPassword.error = getString(R.string.error_passwords_mismatch)
                    } else {
                        binding.tilConfirmPassword.error = null
                    }
                } else {
                    binding.tilConfirmPassword.error = null
                }
            }
        }

        binding.etPassword.addTextChangedListener(passwordWatcher)
        binding.etConfirmPassword.addTextChangedListener(confirmPasswordWatcher)
    }

    private fun openLocationPickerScreen() {
        val intent = Intent(this, LocationPickerActivity::class.java).apply {
            if (selectedLatitude != null && selectedLongitude != null) {
                putExtra(Constants.EXTRA_LATITUDE, selectedLatitude!!)
                putExtra(Constants.EXTRA_LONGITUDE, selectedLongitude!!)
            }
        }
        locationPickerLauncher.launch(intent)
    }

    private fun updateLocationUi() {
        val lat = selectedLatitude
        val lon = selectedLongitude

        if (isLocationConfirmed && lat != null && lon != null) {
            binding.tvLocationStatus.text = String.format(
                Locale.US,
                "✓ CONFIRMED: Lat %.5f°, Lon %.5f°",
                lat,
                lon
            )
            binding.tvLocationStatus.setBackgroundResource(R.drawable.bg_badge_confirmed)
            binding.tvLocationStatus.setTextColor(getColor(R.color.status_active_text))
            binding.ivLocationConfirmedCheck.visibility = View.VISIBLE
            binding.btnSelectLocationOnMap.text = getString(R.string.btn_reselect_location_on_map)
        } else {
            binding.tvLocationStatus.text = getString(R.string.location_not_selected)
            binding.tvLocationStatus.setBackgroundResource(R.drawable.bg_card_surface)
            binding.tvLocationStatus.setTextColor(getColor(R.color.solar_amber_light))
            binding.ivLocationConfirmedCheck.visibility = View.GONE
            binding.btnSelectLocationOnMap.text = getString(R.string.btn_select_facility_on_map)
        }
    }

    private fun attemptRegister() {
        clearErrors()

        val nic = binding.etNic.text?.toString()?.trim().orEmpty()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val address = binding.etAddress.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var hasError = false

        if (!InputValidator.isValidNic(nic)) {
            binding.tilNic.error = getString(R.string.error_nic_invalid)
            hasError = true
        }

        if (!InputValidator.isValidUsername(username)) {
            binding.tilUsername.error = getString(R.string.error_username_short)
            hasError = true
        }

        if (!InputValidator.isValidFullName(fullName)) {
            binding.tilFullName.error = getString(R.string.error_full_name_required)
            hasError = true
        }

        if (!InputValidator.isValidPhone(phone)) {
            binding.tilPhone.error = getString(R.string.error_phone_invalid)
            hasError = true
        }

        if (email.isBlank()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            hasError = true
        } else if (!InputValidator.isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            hasError = true
        }

        if (address.isBlank() || address.length < 3) {
            binding.tilAddress.error = getString(R.string.error_address_required)
            hasError = true
        }

        if (!isLocationConfirmed || selectedLatitude == null || selectedLongitude == null) {
            binding.tvErrorMessage.text = getString(R.string.error_location_required)
            binding.tvErrorMessage.visibility = View.VISIBLE
            hasError = true
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            hasError = true
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            hasError = true
        } else if (!InputValidator.isValidPassword(password)) {
            binding.tilPassword.error = getString(R.string.error_password_complex)
            hasError = true
        }

        if (confirmPassword.isBlank()) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_required)
            hasError = true
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_mismatch)
            hasError = true
        }

        if (!hasError) {
            viewModel.registerProsumer(
                nic = nic,
                username = username,
                password = password,
                fullName = fullName,
                phone = phone,
                email = email,
                address = address,
                latitude = selectedLatitude,
                longitude = selectedLongitude
            )
        }
    }

    private fun clearErrors() {
        binding.tilNic.error = null
        binding.tilUsername.error = null
        binding.tilFullName.error = null
        binding.tilPhone.error = null
        binding.tilEmail.error = null
        binding.tilAddress.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvErrorMessage.visibility = View.GONE
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is RegisterUiState.Idle -> {
                            setLoading(false)
                        }
                        is RegisterUiState.Loading -> {
                            setLoading(true)
                        }
                        is RegisterUiState.Success -> {
                            setLoading(false)
                            val intent = Intent(this@RegisterActivity, PendingActivationActivity::class.java).apply {
                                putExtra(Constants.EXTRA_NIC, state.user.nic)
                                putExtra(Constants.EXTRA_STATUS_MESSAGE, state.message)
                            }
                            startActivity(intent)
                            viewModel.resetRegisterState()
                            finish()
                        }
                        is RegisterUiState.Error -> {
                            setLoading(false)
                            binding.tvErrorMessage.text = state.message
                            binding.tvErrorMessage.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.etNic.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etAddress.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.btnSelectLocationOnMap.isEnabled = !isLoading
    }
}
