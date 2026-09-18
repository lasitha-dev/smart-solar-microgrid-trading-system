/**
 * Description: Native CameraX QR viewfinder activity managing camera lifecycle, framing guides,
 * torch toggle, and defensive runtime permission verification (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ActivityOperatorScannerBinding

/**
 * Native camera viewfinder handling hardware access, torch activation, and permission safety.
 */
class OperatorScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorScannerBinding

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    var isTorchEnabled: Boolean = false
        private set

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handleCameraPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        checkAndRequestPermissions()
    }

    private fun setupListeners() {
        binding.btnScannerBack.setOnClickListener {
            finish()
        }

        binding.btnGrantPermission.setOnClickListener {
            requestCameraPermission()
        }

        binding.btnTorchToggle.setOnClickListener {
            toggleTorch()
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewViewFinder.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )
                cameraControl = camera?.cameraControl
            } catch (_: Exception) {
                // Defensive fallback prevents crashes on emulators or unsupported camera hardware
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Toggles the device flashlight on and off.
     */
    fun toggleTorch() {
        isTorchEnabled = !isTorchEnabled
        try {
            cameraControl?.enableTorch(isTorchEnabled)
        } catch (_: Exception) {
            // Devices without flash hardware fail gracefully
        }
        val tintColor = if (isTorchEnabled) {
            ContextCompat.getColor(this, R.color.color_secondary)
        } else {
            ContextCompat.getColor(this, R.color.color_secondary_variant)
        }
        binding.btnTorchToggle.setColorFilter(tintColor)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
            // Defensive teardown
        }
    }
}
