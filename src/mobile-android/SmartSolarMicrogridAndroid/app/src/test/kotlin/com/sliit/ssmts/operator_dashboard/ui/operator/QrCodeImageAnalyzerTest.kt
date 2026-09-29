/**
 * Name: A.L.M Athulathmudali
 * IT no: IT21129544
 * Description: Unit test suite for QrCodeImageAnalyzer validating frame decoding lifecycle,
 * scanning toggle suppression, and safe error handling (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Asserts QrCodeImageAnalyzer frame processing safety and control toggles.
 */
class QrCodeImageAnalyzerTest {

    /**
     * Asserts that an ImageProxy is safely closed when scanning is paused.
     */
    @Test
    fun analyze_whenScanningDisabled_closesImageProxyImmediately() {
        var scannedText: String? = null
        val analyzer = QrCodeImageAnalyzer { scannedText = it }
        analyzer.setScanningEnabled(false)

        val mockImageProxy = mockk<ImageProxy>(relaxed = true)

        analyzer.analyze(mockImageProxy)

        verify(exactly = 1) { mockImageProxy.close() }
        org.junit.Assert.assertNull(scannedText)
    }

    /**
     * Asserts that malformed or non-QR frame closes the image proxy cleanly without throwing.
     */
    @Test
    fun analyze_emptyFrame_closesImageProxySafely() {
        var scannedText: String? = null
        val analyzer = QrCodeImageAnalyzer { scannedText = it }

        val mockPlane = mockk<ImageProxy.PlaneProxy>()
        val buffer = ByteBuffer.allocate(64 * 64)
        every { mockPlane.buffer } returns buffer
        every { mockPlane.rowStride } returns 64
        every { mockPlane.pixelStride } returns 1

        val mockInfo = mockk<ImageInfo>()
        every { mockInfo.rotationDegrees } returns 0

        val mockImageProxy = mockk<ImageProxy>(relaxed = true)
        every { mockImageProxy.planes } returns arrayOf(mockPlane)
        every { mockImageProxy.width } returns 64
        every { mockImageProxy.height } returns 64
        every { mockImageProxy.imageInfo } returns mockInfo

        analyzer.analyze(mockImageProxy)

        verify(atLeast = 1) { mockImageProxy.close() }
        org.junit.Assert.assertNull(scannedText)
    }

    /**
     * Asserts that analyzer instantiation succeeds with valid callback.
     */
    @Test
    fun analyzerInstance_instantiatesCleanly() {
        val analyzer = QrCodeImageAnalyzer { }
        assertNotNull(analyzer)
    }
}
