/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Android Activity displaying the Prosumer profile with read-only default mode, edit toggle, dirty-checking, and locked coordinates.
 */

package com.sliit.ssmts.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityProfileBinding
import com.sliit.ssmts.domain.model.UserSession
import com.sliit.ssmts.ui.ViewModelFactory
import com.sliit.ssmts.ui.auth.LoginActivity
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.InputValidator
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Profile management activity for solar prosumers.
 * Features:
 * - Read-only by default on screen load
 * - Edit mode toggle icon in header
 * - Strictly locked / immutable facility location coordinates
 * - Real-time dirty checking for Save button (enabled only when modified from DB baseline)
 * - Cancel edit action reverting to baseline values in single line button
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels { ViewModelFactory(this) }

    // Edit Mode State
    private var isEditMode: Boolean = false

    // Baseline original values from DB for dirty checking
    private var originalFullName: String = ""
    private var originalPhone: String = ""
    private var currentAddress: String = ""

    private val dirtyCheckTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            checkDirtyState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupTextWatchers()
        applyEditModeUi(false)
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnToggleEdit.setOnClickListener {
            toggleEditMode()
        }

        binding.btnCancelEdit.setOnClickListener {
            cancelEditMode()
        }

        binding.btnSaveProfile.setOnClickListener {
            attemptSaveProfile()
        }

        binding.btnRequestDeactivation.setOnClickListener {
            showDeactivationDialog()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout {
                Toast.makeText(this, getString(R.string.msg_logged_out), Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etProfileFullName.addTextChangedListener(dirtyCheckTextWatcher)
        binding.etProfilePhone.addTextChangedListener(dirtyCheckTextWatcher)
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        applyEditModeUi(isEditMode)

        if (isEditMode) {
            Toast.makeText(this, getString(R.string.msg_edit_mode_enabled), Toast.LENGTH_SHORT).show()
            binding.etProfileFullName.requestFocus()
        } else {
            revertInputsToOriginal()
            clearErrors()
            Toast.makeText(this, getString(R.string.msg_edit_mode_cancelled), Toast.LENGTH_SHORT).show()
        }
        checkDirtyState()
    }

    private fun cancelEditMode() {
        isEditMode = false
        applyEditModeUi(false)
        revertInputsToOriginal()
        clearErrors()
        checkDirtyState()
        Toast.makeText(this, getString(R.string.msg_edit_mode_cancelled), Toast.LENGTH_SHORT).show()
    }

    private fun applyEditModeUi(editable: Boolean) {
        // Editable fields enabled only in edit mode
        binding.etProfileFullName.isEnabled = editable
        binding.etProfilePhone.isEnabled = editable

        if (editable) {
            binding.llEditActions.visibility = View.VISIBLE
            binding.btnCancelEdit.visibility = View.VISIBLE
            checkDirtyState()
        } else {
            binding.llEditActions.visibility = View.GONE
            binding.btnCancelEdit.visibility = View.GONE
            binding.btnSaveProfile.visibility = View.GONE
            binding.btnSaveProfile.isEnabled = false
        }
    }

    private fun revertInputsToOriginal() {
        binding.etProfileFullName.setText(originalFullName)
        binding.etProfilePhone.setText(originalPhone)
    }

    /**
     * Dirty state checker: Displays and enables the "Save Profile Changes" button
     * ONLY when the screen is in edit mode, at least one field differs from the DB baseline,
     * and all inputs satisfy validation criteria.
     */
    private fun checkDirtyState() {
        if (!isEditMode) {
            binding.llEditActions.visibility = View.GONE
            binding.btnCancelEdit.visibility = View.GONE
            binding.btnSaveProfile.visibility = View.GONE
            binding.btnSaveProfile.isEnabled = false
            return
        }

        val currentFullName = binding.etProfileFullName.text?.toString()?.trim().orEmpty()
        val currentPhone = binding.etProfilePhone.text?.toString()?.trim().orEmpty()

        val hasChanged = (currentFullName != originalFullName) ||
                (currentPhone != originalPhone)

        val isNameValid = InputValidator.isValidFullName(currentFullName)
        val isPhoneValid = InputValidator.isValidPhone(currentPhone)

        val canSave = hasChanged && isNameValid && isPhoneValid

        binding.llEditActions.visibility = View.VISIBLE
        binding.btnCancelEdit.visibility = View.VISIBLE

        if (hasChanged) {
            binding.btnSaveProfile.visibility = View.VISIBLE
            binding.btnSaveProfile.isEnabled = canSave
        } else {
            binding.btnSaveProfile.visibility = View.GONE
            binding.btnSaveProfile.isEnabled = false
        }
    }

    private fun clearErrors() {
        binding.tilProfileFullName.error = null
        binding.tilProfilePhone.error = null
    }

    private fun attemptSaveProfile() {
        clearErrors()

        val fullName = binding.etProfileFullName.text?.toString()?.trim().orEmpty()
        val phone = binding.etProfilePhone.text?.toString()?.trim().orEmpty()

        var hasError = false

        if (!InputValidator.isValidFullName(fullName)) {
            binding.tilProfileFullName.error = getString(R.string.error_full_name_required)
            hasError = true
        }

        if (!InputValidator.isValidPhone(phone)) {
            binding.tilProfilePhone.error = getString(R.string.error_phone_invalid)
            hasError = true
        }

        if (!hasError) {
            viewModel.updateProfile(fullName, phone, currentAddress)
        }
    }

    private fun showDeactivationDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.dialog_deactivate_reason_hint)
            setPadding(48, 32, 48, 32)
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_muted))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_deactivate_title))
            .setMessage(getString(R.string.dialog_deactivate_message))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_confirm_deactivation)) { _, _ ->
                val reason = input.text?.toString()?.trim()
                viewModel.requestDeactivation(reason, null)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collect { state ->
                        when (state) {
                            is ProfileUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is ProfileUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                populateProfileData(state.session)
                            }
                            is ProfileUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.updateState.collect { state ->
                        when (state) {
                            is ProfileUpdateState.Idle -> {
                                checkDirtyState()
                            }
                            is ProfileUpdateState.Loading -> {
                                binding.btnSaveProfile.isEnabled = false
                                binding.btnCancelEdit.isEnabled = false
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is ProfileUpdateState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnCancelEdit.isEnabled = true
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_SHORT).show()

                                // Update baseline values on successful save
                                originalFullName = binding.etProfileFullName.text?.toString()?.trim().orEmpty()
                                originalPhone = binding.etProfilePhone.text?.toString()?.trim().orEmpty()

                                // Exit edit mode
                                isEditMode = false
                                applyEditModeUi(false)
                                checkDirtyState()
                                viewModel.resetUpdateState()
                                viewModel.loadProfile()
                            }
                            is ProfileUpdateState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnCancelEdit.isEnabled = true
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                checkDirtyState()
                                viewModel.resetUpdateState()
                            }
                        }
                    }
                }

                launch {
                    viewModel.deactivationState.collect { state ->
                        when (state) {
                            is DeactivationState.Idle -> {}
                            is DeactivationState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is DeactivationState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                navigateToLogin()
                            }
                            is DeactivationState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateProfileData(session: UserSession) {
        // Account & Identity Information
        binding.tvProfileNic.text = session.nic
        binding.tvProfileUsername.text = session.username
        binding.tvProfileRole.text = session.role
        binding.tvProfileAddress.text = if (session.address.isNotBlank()) session.address else "Not Specified"
        binding.tvStatusBadge.text = session.status.uppercase()

        if (session.status.equals(Constants.STATUS_ACTIVE, ignoreCase = true)) {
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvStatusBadge.setTextColor(getColor(R.color.status_active_text))
        } else {
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvStatusBadge.setTextColor(getColor(R.color.status_pending_text))
        }

        // Strictly Immutable Facility Location Coordinates
        val lat = session.latitude
        val lon = session.longitude
        if (lat != null && lon != null) {
            binding.tvProfileLatitude.text = String.format(Locale.US, "%.5f° %s", Math.abs(lat), if (lat >= 0) "N" else "S")
            binding.tvProfileLongitude.text = String.format(Locale.US, "%.5f° %s", Math.abs(lon), if (lon >= 0) "E" else "W")
        } else {
            binding.tvProfileLatitude.text = "6.92710° N (Default Cluster)"
            binding.tvProfileLongitude.text = "79.86120° E (Default Cluster)"
        }

        // Editable Baseline Profile Information from DB
        originalFullName = session.fullName.trim()
        originalPhone = session.phone.trim()
        currentAddress = session.address.trim()

        if (!isEditMode) {
            binding.etProfileFullName.setText(session.fullName)
            binding.etProfilePhone.setText(session.phone)
        }

        checkDirtyState()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
