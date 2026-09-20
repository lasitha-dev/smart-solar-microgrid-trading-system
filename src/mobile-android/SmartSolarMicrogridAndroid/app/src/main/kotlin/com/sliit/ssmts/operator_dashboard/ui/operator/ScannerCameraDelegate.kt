/**
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

    /**
     * Initializes CameraX lifecycle provider and binds the preview use case to the viewfinder surface.
     *
     * @param lifecycleOwner Android LifecycleOwner to bind the camera session to.
     * @param surfaceProvider Surface provider receiving the viewfinder camera feed.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                cameraControl = camera?.cameraControl
            } catch (_: Exception) {
                // Defensive fallback prevents crashes on emulators or unsupported camera hardware
            }
        }, ContextCompat.getMainExecutor(context))
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
