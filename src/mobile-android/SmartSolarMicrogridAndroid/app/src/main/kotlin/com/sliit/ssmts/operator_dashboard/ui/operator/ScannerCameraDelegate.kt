/**
 * Name: A.L.M Athulathmudali
 * IT no: IT21129544
 * Description: Hardware camera delegate encapsulating CameraX lifecycle bindings, surface provider
 * integration, torch toggle control, and defensive exception handling (Rule 3 SRP delegate).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

/**
 * Manages CameraX hardware provider lifecycle, preview surface binding, and torch illumination.
 *
 * @property context Host context used for acquiring ProcessCameraProvider and MainExecutor.
 */
class ScannerCameraDelegate(
    private val context: Context
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null

    /**
     * Indicates whether the device flashlight/torch is currently active.
     */
    var isTorchEnabled: Boolean = false
        private set

    private var qrAnalyzer: QrCodeImageAnalyzer? = null

    /**
     * Initializes CameraX lifecycle provider and binds the preview and QR analysis use cases to the viewfinder.
     *
     * @param lifecycleOwner Android LifecycleOwner to bind the camera session to.
     * @param surfaceProvider Surface provider receiving the viewfinder camera feed.
     * @param onQrCodeDetected Optional callback invoked when a QR code is detected in camera frames.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onQrCodeDetected: ((String) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

                if (onQrCodeDetected != null) {
                    val analyzer = QrCodeImageAnalyzer(onQrCodeDetected)
                    qrAnalyzer = analyzer
                    val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
                        }
                    useCases.add(imageAnalysis)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                cameraControl = camera?.cameraControl
            } catch (_: Exception) {
                // Defensive fallback prevents crashes on emulators or unsupported camera hardware
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Toggles frame decoding analysis on and off to avoid duplicate scans while dialogs are open.
     *
     * @param enabled True to enable QR detection; false to pause.
     */
    fun setScanningEnabled(enabled: Boolean) {
        qrAnalyzer?.setScanningEnabled(enabled)
    }

    /**
     * Toggles the device flashlight on and off, updating internal torch state safely.
     *
     * @return New torch state (true if enabled, false if disabled).
     */
    fun toggleTorch(): Boolean {
        isTorchEnabled = !isTorchEnabled
        try {
            cameraControl?.enableTorch(isTorchEnabled)
        } catch (_: Exception) {
            // Devices without flash hardware fail gracefully
        }
        return isTorchEnabled
    }

    /**
     * Unbinds all active camera use-cases from the lifecycle provider to release hardware resources.
     */
    fun unbindAll() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
            // Defensive teardown
        }
    }
}
