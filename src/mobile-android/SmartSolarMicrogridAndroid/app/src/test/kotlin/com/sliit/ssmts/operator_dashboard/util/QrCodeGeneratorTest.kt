/**
 * Description: Unit test suite for QrCodeGenerator verifying canonical payload syntax,
 * delimiter injection resistance, non-null bitmap dimensions, and high-contrast pixel rendering.
 */
package com.sliit.ssmts.operator_dashboard.util

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test validating QrCodeGenerator behavior against specification constraints.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QrCodeGeneratorTest {

    /**
     * Verifies that buildCanonicalPayload formats an exact 6-part string prefixed with SSMTS-QR.
     */
    @Test
    fun buildCanonicalPayload_validInputs_formatsExactPayload() {
        val resId = "RES-12345"
        val nic = "199512345678"
        val stationId = "STATION-COLOMBO"
        val scheduledIso = "2026-09-16T14:30:00Z"
        val signature = "f4c82b9a76d1e3c8"

        val payload = QrCodeGenerator.buildCanonicalPayload(
            reservationId = resId,
            prosumerNic = nic,
            stationId = stationId,
            scheduledDateTimeIso = scheduledIso,
            signature = signature
        )

        assertEquals("SSMTS-QR|RES-12345|199512345678|STATION-COLOMBO|2026-09-16T14:30:00Z|f4c82b9a76d1e3c8", payload)

        val parts = payload.split(QrCodeGenerator.DELIMITER)
        assertEquals(6, parts.size)
        assertEquals("SSMTS-QR", parts[0])
        assertEquals(resId, parts[1])
        assertEquals(nic, parts[2])
        assertEquals(stationId, parts[3])
        assertEquals(scheduledIso, parts[4])
        assertEquals(signature, parts[5])
    }

    /**
     * Verifies delimiter injection is rejected if any field contains the payload delimiter.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_fieldWithDelimiter_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES|INJECTION",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies that blank input arguments throw an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankField_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Asserts the generator outputs a non-null Bitmap with exact matching width and height dimensions.
     */
    @Test
    fun generateQrBitmap_validPayload_producesNonNullBitmapWithExactDimensions() {
        val payload = "SSMTS-QR|RES-001|199012345678|STATION-01|2026-09-16T10:00:00Z|MOCKSIG"
        val dimension = 256

        val bitmap = QrCodeGenerator.generateQrBitmap(payload = payload, dimensionPx = dimension)

        assertNotNull(bitmap)
        assertEquals(dimension, bitmap.width)
        assertEquals(dimension, bitmap.height)
    }

    /**
     * Verifies that the rendered QR bitmap contains both dark (foreground) and light (background) pixels,
     * ensuring proper visual contrast.
     */
    @Test
    fun generateQrBitmap_verifiesHighContrastPixels() {
        val payload = "SSMTS-QR|RES-CONTRAST|199012345678|STATION-01|2026-09-16T10:00:00Z|MOCKSIG"
        val dimension = 128

        val bitmap = QrCodeGenerator.generateQrBitmap(
            payload = payload,
            dimensionPx = dimension,
            foregroundColor = Color.BLACK,
            backgroundColor = Color.WHITE
        )

        var hasDarkPixel = false
        var hasLightPixel = false

        for (x in 0 until dimension) {
            for (y in 0 until dimension) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == Color.BLACK) hasDarkPixel = true
                if (pixel == Color.WHITE) hasLightPixel = true
                if (hasDarkPixel && hasLightPixel) break
            }
        }

        assertTrue("Generated QR bitmap must contain foreground dark pixels", hasDarkPixel)
        assertTrue("Generated QR bitmap must contain background light pixels", hasLightPixel)
    }

    /**
     * Verifies that attempting to render a QR code with a blank payload throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun generateQrBitmap_blankPayload_throwsException() {
        QrCodeGenerator.generateQrBitmap(payload = "   ", dimensionPx = 256)
    }

    /**
     * Verifies that attempting to render a QR code with dimension below minimum bounds throws IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun generateQrBitmap_dimensionBelowMinimum_throwsException() {
        QrCodeGenerator.generateQrBitmap(payload = "TEST_PAYLOAD", dimensionPx = 32)
    }

    /**
     * Verifies the convenience method generates a valid bitmap end-to-end.
     */
    @Test
    fun generateReservationQrBitmap_endToEnd_returnsValidBitmap() {
        val bitmap = QrCodeGenerator.generateReservationQrBitmap(
            reservationId = "RES-E2E",
            prosumerNic = "200012345678",
            stationId = "STATION-HUB",
            scheduledDateTimeIso = "2026-09-16T12:00:00Z",
            signature = "E2ESIG",
            dimensionPx = 200
        )

        assertNotNull(bitmap)
        assertEquals(200, bitmap.width)
        assertEquals(200, bitmap.height)
    }
}
