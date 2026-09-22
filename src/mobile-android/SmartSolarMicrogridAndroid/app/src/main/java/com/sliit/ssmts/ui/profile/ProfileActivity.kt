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

    // Facility Location Coordinates for Map View
    private var currentLatitude: Double = 6.9271
    private var currentLongitude: Double = 79.8612

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
        // Back to Home Dashboard
        binding.btnProfileBack.setOnClickListener {
            finish()
        }

        binding.btnToggleEdit.setOnClickListener {
            toggleEditMode()
        }

        binding.btnCancelEdit.setOnClickListener {
            cancelEditMode()
        }

        binding.btnSaveProfile.setOnClickListener {
            attemptSaveProfile()
        }

        // Open Change Password Dialog
        binding.btnOpenChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // View Facility Location on Interactive Map
        binding.btnViewLocationOnMap.setOnClickListener {
            val intent = Intent(this, com.sliit.ssmts.ui.location.LocationPickerActivity::class.java).apply {
                putExtra(Constants.EXTRA_LATITUDE, currentLatitude)
                putExtra(Constants.EXTRA_LONGITUDE, currentLongitude)
                putExtra(Constants.EXTRA_READ_ONLY, true)
            }
            startActivity(intent)
        }

        // Request Deactivation Dialog
        binding.btnRequestDeactivation.setOnClickListener {
            showDeactivationDialog()
        }

        // Delete Account Dialog
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
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
        val currentSession = (viewModel.profileState.value as? ProfileUiState.Success)?.session
        if (currentSession?.status.equals("Deactivated", ignoreCase = true)) {
            Toast.makeText(this, "Deactivated accounts cannot modify profile information.", Toast.LENGTH_SHORT).show()
            return
        }

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

        val currentSession = (viewModel.profileState.value as? ProfileUiState.Success)?.session
        if (currentSession?.status.equals("Deactivated", ignoreCase = true)) {
            Toast.makeText(this, "Deactivated accounts cannot modify profile information.", Toast.LENGTH_SHORT).show()
            return
        }

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

    private fun showDeleteAccountDialog() {
        val currentSession = (viewModel.profileState.value as? ProfileUiState.Success)?.session
        if (currentSession?.status.equals("Deactivated", ignoreCase = true)) {
            Toast.makeText(this, "Deactivated accounts cannot delete account.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = com.sliit.ssmts.databinding.DialogDeleteAccountBinding.inflate(layoutInflater)
        val registeredEmail = binding.tvProfileEmail.text?.toString()?.trim().orEmpty()

        dialogBinding.tvDeleteTargetEmail.text = if (registeredEmail.isNotBlank()) registeredEmail else "user@smartgrid.lk"
        dialogBinding.tvDeleteMatchStatus.text = getString(R.string.error_email_mismatch)
        dialogBinding.tvDeleteMatchStatus.setTextColor(getColor(R.color.error_red))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_confirm_delete_account), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val deleteBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            deleteBtn.isEnabled = false
            deleteBtn.setTextColor(getColor(R.color.error_red))

            dialogBinding.etDeleteConfirmEmail.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val typed = s?.toString()?.trim().orEmpty()
                    val isMatched = typed.equals(registeredEmail, ignoreCase = true)

                    if (isMatched && typed.isNotBlank()) {
                        dialogBinding.tvDeleteMatchStatus.text = getString(R.string.msg_email_matched)
                        dialogBinding.tvDeleteMatchStatus.setTextColor(getColor(R.color.solar_amber_primary))
                        deleteBtn.isEnabled = true
                    } else {
                        dialogBinding.tvDeleteMatchStatus.text = getString(R.string.error_email_mismatch)
                        dialogBinding.tvDeleteMatchStatus.setTextColor(getColor(R.color.error_red))
                        deleteBtn.isEnabled = false
                    }
                }
            })

            deleteBtn.setOnClickListener {
                val typed = dialogBinding.etDeleteConfirmEmail.text?.toString()?.trim().orEmpty()
                if (typed.equals(registeredEmail, ignoreCase = true) && typed.isNotBlank()) {
                    dialog.dismiss()
                    viewModel.deleteAccount(typed)
                }
            }
        }

        dialog.show()
    }

    private fun showDeactivationDialog() {
        val currentSession = (viewModel.profileState.value as? ProfileUiState.Success)?.session
        if (currentSession?.status.equals("Deactivated", ignoreCase = true)) {
            Toast.makeText(this, "Your account is already deactivated.", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this).apply {
            hint = getString(R.string.dialog_deactivate_reason_hint)
            setPadding(48, 32, 48, 32)
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_muted))
        }

        val role = binding.tvProfileRole.text.toString()
        val deactivationMessage = if (role.contains("Grid Operator", ignoreCase = true)) {
            "Are you sure you want to deactivate your grid operator account? You will not be able to manage or operate the microgrid until reactivated by Backoffice."
        } else {
            getString(R.string.dialog_deactivate_message)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_deactivate_title))
            .setMessage(deactivationMessage)
            .setView(input)
            .setPositiveButton(getString(R.string.btn_confirm_deactivation)) { _, _ ->
                val reason = input.text?.toString()?.trim()
                viewModel.requestDeactivation(reason, null)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val currentSession = (viewModel.profileState.value as? ProfileUiState.Success)?.session
        if (currentSession?.status.equals("Deactivated", ignoreCase = true)) {
            Toast.makeText(this, "Deactivated accounts cannot change password.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = com.sliit.ssmts.databinding.DialogChangePasswordBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.btn_submit_change_password), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // TextWatchers for live complexity & mismatch validation inside dialog
        dialogBinding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newPass = s?.toString().orEmpty()
                val confirm = dialogBinding.etConfirmNewPassword.text?.toString().orEmpty()

                if (newPass.isNotEmpty()) {
                    if (newPass.length < 6) {
                        dialogBinding.tilNewPassword.error = getString(R.string.error_password_short)
                    } else if (!InputValidator.isValidPassword(newPass)) {
                        dialogBinding.tilNewPassword.error = getString(R.string.error_password_complex)
                    } else {
                        dialogBinding.tilNewPassword.error = null
                    }
                } else {
                    dialogBinding.tilNewPassword.error = null
                }

                if (confirm.isNotEmpty()) {
                    if (confirm != newPass) {
                        dialogBinding.tilConfirmNewPassword.error = getString(R.string.error_passwords_mismatch)
                    } else {
                        dialogBinding.tilConfirmNewPassword.error = null
                    }
                }
            }
        })

        dialogBinding.etConfirmNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val confirm = s?.toString().orEmpty()
                val newPass = dialogBinding.etNewPassword.text?.toString().orEmpty()

                if (confirm.isNotEmpty()) {
                    if (confirm != newPass) {
                        dialogBinding.tilConfirmNewPassword.error = getString(R.string.error_passwords_mismatch)
                    } else {
                        dialogBinding.tilConfirmNewPassword.error = null
                    }
                } else {
                    dialogBinding.tilConfirmNewPassword.error = null
                }
            }
        })

        dialog.setOnShowListener {
            val submitBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            submitBtn.setOnClickListener {
                val current = dialogBinding.etCurrentPassword.text?.toString().orEmpty()
                val newPass = dialogBinding.etNewPassword.text?.toString().orEmpty()
                val confirm = dialogBinding.etConfirmNewPassword.text?.toString().orEmpty()

                var hasError = false

                if (current.isBlank()) {
                    dialogBinding.tilCurrentPassword.error = getString(R.string.error_current_password_required)
                    hasError = true
                } else {
                    dialogBinding.tilCurrentPassword.error = null
                }

                if (newPass.isBlank()) {
                    dialogBinding.tilNewPassword.error = getString(R.string.error_new_password_required)
                    hasError = true
                } else if (newPass.length < 6) {
                    dialogBinding.tilNewPassword.error = getString(R.string.error_password_short)
                    hasError = true
                } else if (!InputValidator.isValidPassword(newPass)) {
                    dialogBinding.tilNewPassword.error = getString(R.string.error_password_complex)
                    hasError = true
                } else {
                    dialogBinding.tilNewPassword.error = null
                }

                if (confirm.isBlank()) {
                    dialogBinding.tilConfirmNewPassword.error = getString(R.string.error_password_required)
                    hasError = true
                } else if (newPass != confirm) {
                    dialogBinding.tilConfirmNewPassword.error = getString(R.string.error_passwords_mismatch)
                    hasError = true
                } else {
                    dialogBinding.tilConfirmNewPassword.error = null
                }

                if (!hasError) {
                    dialog.dismiss()
                    viewModel.changePassword(current, newPass, confirm)
                }
            }
        }

        dialog.show()
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
                                // Retain active session without logging out; reload profile with Deactivated status
                                viewModel.loadProfile()
                            }
                            is DeactivationState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.changePasswordState.collect { state ->
                        when (state) {
                            is ChangePasswordState.Idle -> {}
                            is ChangePasswordState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is ChangePasswordState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetChangePasswordState()
                                // Auto-logout and redirect to Login
                                navigateToLogin()
                            }
                            is ChangePasswordState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetChangePasswordState()
                            }
                        }
                    }
                }

                launch {
                    viewModel.deleteAccountState.collect { state ->
                        when (state) {
                            is DeleteAccountState.Idle -> {}
                            is DeleteAccountState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is DeleteAccountState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetDeleteAccountState()
                                navigateToLogin()
                            }
                            is DeleteAccountState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ProfileActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetDeleteAccountState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateProfileData(session: UserSession) {
        val isDeactivated = session.status.equals("Deactivated", ignoreCase = true)

        // Dynamic Header
        binding.tvProfileHeader.text = if (session.role.equals("GridOperator", ignoreCase = true)) {
            "Grid Operator Profile"
        } else {
            getString(R.string.profile_title)
        }

        binding.tvProfileSubheader.text = if (session.role.equals("GridOperator", ignoreCase = true)) {
            "Manage your grid operator identity and credentials"
        } else {
            getString(R.string.profile_subtitle)
        }

        // Account & Identity Information
        binding.tvProfileNic.text = session.nic
        binding.tvProfileUsername.text = session.username
        binding.tvProfileRole.text = when (session.role) {
            "GridOperator" -> "Grid Operator"
            "Backoffice" -> "Backoffice Officer"
            "Administrator" -> "System Administrator"
            else -> getString(R.string.profile_role_prosumer)
        }
        binding.tvProfileEmail.text = if (session.email.isNotBlank()) session.email else "Not Specified"
        binding.tvProfileAddress.text = if (session.address.isNotBlank()) session.address else "Not Specified"
        binding.tvStatusBadge.text = session.status.uppercase()

        if (session.status.equals(Constants.STATUS_ACTIVE, ignoreCase = true)) {
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvStatusBadge.setTextColor(getColor(R.color.status_active_text))
        } else if (isDeactivated) {
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvStatusBadge.setTextColor(getColor(R.color.error_red))
        } else {
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            binding.tvStatusBadge.setTextColor(getColor(R.color.status_pending_text))
        }

        // Disable or restrict mutation operations if deactivated
        if (isDeactivated) {
            isEditMode = false
            applyEditModeUi(false)
            binding.btnToggleEdit.visibility = View.GONE
            binding.btnOpenChangePassword.isEnabled = false
            binding.btnOpenChangePassword.alpha = 0.5f
            binding.btnDeleteAccount.isEnabled = false
            binding.btnDeleteAccount.alpha = 0.5f
            binding.btnRequestDeactivation.isEnabled = false
            binding.btnRequestDeactivation.alpha = 0.5f
            binding.btnRequestDeactivation.text = "Account Deactivated"
        } else {
            binding.btnToggleEdit.visibility = View.VISIBLE
            binding.btnOpenChangePassword.isEnabled = true
            binding.btnOpenChangePassword.alpha = 1.0f
            binding.btnDeleteAccount.isEnabled = true
            binding.btnDeleteAccount.alpha = 1.0f
            binding.btnRequestDeactivation.isEnabled = true
            binding.btnRequestDeactivation.alpha = 1.0f
            binding.btnRequestDeactivation.text = getString(R.string.btn_request_deactivation)
        }

        // Strictly Immutable Facility Location Coordinates for Map Viewer
        val lat = session.latitude
        val lon = session.longitude
        if (lat != null && lon != null) {
            currentLatitude = lat
            currentLongitude = lon
        } else {
            currentLatitude = 6.9271
            currentLongitude = 79.8612
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
