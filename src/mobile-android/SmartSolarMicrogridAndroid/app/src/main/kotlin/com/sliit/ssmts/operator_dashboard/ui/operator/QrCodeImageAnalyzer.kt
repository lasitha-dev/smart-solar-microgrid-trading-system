/**
 * Name: A.L.M Athulathmudali
 * IT no: IT21129544
 * Description: Real-time CameraX ImageAnalysis analyzer decoding QR code frames via ZXing MultiFormatReader
 * and dispatching discovered payload tokens to the scanner delegate (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ImageAnalysis analyzer that inspects camera stream frames for QR codes using ZXing.
 *
 * @property onQrCodeDetected Callback invoked when a QR code token is successfully decoded from a camera frame.
 */
class QrCodeImageAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val isScanningEnabled = AtomicBoolean(true)
    private var lastScannedTimestamp = 0L
    private val debounceIntervalMs = 2500L

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    /**
     * Analyzes an incoming camera frame buffer, extracts luminance data, and attempts ZXing QR decoding.
     *
     * @param imageProxy The camera frame image proxy provided by CameraX.
     */
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (!isScanningEnabled.get() || (now - lastScannedTimestamp) < debounceIntervalMs) {
            imageProxy.close()
            return
        }

        try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = imageProxy.width
            val height = imageProxy.height

            val data = ByteArray(width * height)
            if (rowStride == width && pixelStride == 1) {
                buffer.get(data, 0, width * height)
            } else {
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    if (pixelStride == 1) {
                        buffer.get(data, row * width, width)
                    } else {
                        for (col in 0 until width) {
                            data[row * width + col] = buffer.get(row * rowStride + col * pixelStride)
                        }
                    }
                }
            }

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val source = if (rotationDegrees == 90 || rotationDegrees == 270) {
                val rotatedData = rotateYuvDegree(data, width, height, rotationDegrees)
                PlanarYUVLuminanceSource(
                    rotatedData,
                    height,
                    width,
                    0,
                    0,
                    height,
                    width,
                    false
                )
            } else {
                PlanarYUVLuminanceSource(
                    data,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
            }

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            val qrText = result.text

            if (!qrText.isNullOrBlank()) {
                lastScannedTimestamp = now
                onQrCodeDetected(qrText)
            }
        } catch (_: NotFoundException) {
            // Expected for frames that do not contain a readable QR code
        } catch (_: Exception) {
            // Defensive handling for frame conversion edge cases
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }

    /**
     * Rotates planar YUV luminance byte array by 90 or 270 degrees to match device orientation.
     *
     * @param data Raw grayscale pixel buffer.
     * @param width Source buffer width.
     * @param height Source buffer height.
     * @param rotation Clockwise rotation angle in degrees (90 or 270).
     * @return Transposed and rotated grayscale byte buffer.
     */
    private fun rotateYuvDegree(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        val rotated = ByteArray(data.size)
        if (rotation == 90) {
            var i = 0
            for (x in 0 until width) {
                for (y in height - 1 downTo 0) {
                    val srcIndex = y * width + x
                    if (srcIndex < data.size && i < rotated.size) {
                        rotated[i++] = data[srcIndex]
                    }
                }
            }
        } else if (rotation == 270) {
            var i = 0
            for (x in width - 1 downTo 0) {
                for (y in 0 until height) {
                    val srcIndex = y * width + x
                    if (srcIndex < data.size && i < rotated.size) {
                        rotated[i++] = data[srcIndex]
                    }
                }
            }
        } else {
            return data
        }
        return rotated
    }

    /**
     * Pauses or resumes active frame decoding analysis.
     *
     * @param enabled True to resume QR detection; false to pause while modals or loading spinners are active.
     */
    fun setScanningEnabled(enabled: Boolean) {
        isScanningEnabled.set(enabled)
    }
}
