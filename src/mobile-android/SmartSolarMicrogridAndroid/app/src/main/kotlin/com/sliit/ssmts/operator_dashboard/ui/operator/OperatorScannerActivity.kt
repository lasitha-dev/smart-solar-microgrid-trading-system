/**
 * Description: Native CameraX QR viewfinder activity managing camera lifecycle, framing guides,
 * torch toggle, and defensive runtime permission verification (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.repository.OperatorVerificationRepositoryImpl
import com.sliit.ssmts.operator_dashboard.databinding.ActivityOperatorScannerBinding
import com.sliit.ssmts.operator_dashboard.util.QrParseResult
import com.sliit.ssmts.operator_dashboard.util.QrPayloadParser
import kotlinx.coroutines.launch

/**
 * Native camera viewfinder handling hardware access, torch activation, and permission safety.
 */
class OperatorScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorScannerBinding

    var viewModelFactory: ViewModelProvider.Factory? = null

    val viewModel: OperatorScannerViewModel by viewModels {
        viewModelFactory ?: run {
            val database = SsmtsDatabase.getInstance(applicationContext)
            val api = ApiClient.createOperatorDashboardApi("https://10.0.2.2:7143/")
            val repository = OperatorVerificationRepositoryImpl(
                api = api,
                reservationDao = database.reservationCacheDao(),
                auditDao = database.operatorAuditDao()
            )
            OperatorScannerViewModel.Factory(repository)
        }
    }

    val cameraDelegate: ScannerCameraDelegate by lazy {
        ScannerCameraDelegate(this)
    }

    val modalCoordinator: ScannerModalCoordinator by lazy {
        ScannerModalCoordinator(this, supportFragmentManager)
    }

    val isTorchEnabled: Boolean
        get() = cameraDelegate.isTorchEnabled

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleCameraPermissionResult(isGranted)
    }

    /**
     * Initializes activity layout, permission checks, camera bindings, and UI state collectors.
     *
     * @param savedInstanceState Saved bundle state if restoring.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        checkAndRequestPermissions()
        observeViewModel()
    }

    var onPayloadProcessedListener: ((String) -> Unit)? = null

    private fun setupListeners() {
        onPayloadProcessedListener = { payload ->
            viewModel.verifyQrToken(payload)
        }

        binding.btnScannerBack.setOnClickListener {
            finish()
        }

        binding.btnGrantPermission.setOnClickListener {
            requestCameraPermission()
        }

        binding.btnTorchToggle.setOnClickListener {
            toggleTorch()
        }

        binding.btnFastTestQr.setOnClickListener {
            showFastTestDialog()
        }

        binding.btnPermissionUseFastTest.setOnClickListener {
            showFastTestDialog()
        }
    }

    /**
     * Checks if camera hardware permission is granted; requests dynamically if missing.
     */
    fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                binding.cardPermissionDenied.isVisible = false
                startCamera()
            }
            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Handles the outcome of the camera hardware permission request and updates UI state defensively.
     *
     * @param isGranted True if camera permission was granted by the user; false otherwise.
     */
    fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            binding.cardPermissionDenied.isVisible = false
            startCamera()
        } else {
            binding.cardPermissionDenied.isVisible = true
        }
    }

    /**
     * Initializes CameraX lifecycle provider and binds preview use case to viewfinder surface.
     */
    private fun startCamera() {
        cameraDelegate.startCamera(this, binding.previewViewFinder.surfaceProvider)
    }

    /**
     * Toggles the device flashlight on and off.
     */
    fun toggleTorch() {
        val enabled = cameraDelegate.toggleTorch()
        val tintColor = if (enabled) {
            ContextCompat.getColor(this, R.color.color_secondary)
        } else {
            ContextCompat.getColor(this, R.color.color_secondary_variant)
        }
        binding.btnTorchToggle.setColorFilter(tintColor)
    }

    /**
     * Displays the Viva Fast Test QR selection dialog for single-device demonstration (Rule 6.3).
     */
    fun showFastTestDialog() {
        modalCoordinator.showFastTestDialog { payload ->
            processScannedPayload(payload)
        }
    }

    /**
     * Processes a detected or simulated QR payload string, validating syntax defensively
     * before delegating to the verification listener or ViewModel.
     *
     * @param payload Raw QR payload string to validate and process.
     * @return True if payload passes client syntax validation; false if rejected.
     */
    fun processScannedPayload(payload: String): Boolean {
        return when (val parseResult = QrPayloadParser.validateAndParse(payload)) {
            is QrParseResult.Success -> {
                Toast.makeText(
                    this,
                    getString(R.string.scanner_payload_injected_toast, parseResult.payload.reservationId),
                    Toast.LENGTH_SHORT
                ).show()
                onPayloadProcessedListener?.invoke(payload)
                true
            }
            is QrParseResult.Failure -> {
                Toast.makeText(
                    this,
                    getString(R.string.scanner_payload_malformed_toast),
                    Toast.LENGTH_LONG
                ).show()
                false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: ScannerUiState) {
        binding.layoutVerificationLoading.isVisible = (state is ScannerUiState.Verifying || state is ScannerUiState.Finalizing)
        modalCoordinator.dispatchState(
            state = state,
            onReset = { viewModel.resetScannerState() },
            onFinalize = { id, kwh, notes -> viewModel.finalizeEnergyTransfer(id, kwh, notes) }
        )
    }

    /**
     * Unbinds active camera use-cases to ensure camera hardware resources are safely released.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraDelegate.unbindAll()
    }
}
