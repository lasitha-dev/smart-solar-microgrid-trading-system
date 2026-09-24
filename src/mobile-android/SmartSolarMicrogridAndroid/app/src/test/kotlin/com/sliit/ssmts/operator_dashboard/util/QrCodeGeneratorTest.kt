/**
 * Description: Unit test suite for QrCodeGenerator verifying canonical payload syntax,
 * delimiter injection resistance, non-null bitmap dimensions, and high-contrast pixel rendering.
 */
package com.sliit.ssmts.operator_dashboard.util

import android.graphics.Bitmap
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
open class QrCodeGeneratorTest {

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
     * Verifies that buildCanonicalPayload trims surrounding whitespace from fields.
     */
    @Test
    fun buildCanonicalPayload_trimsWhitespace() {
        val payload = QrCodeGenerator.buildCanonicalPayload(
            reservationId = "  RES-999  ",
            prosumerNic = "  200012345678  ",
            stationId = "  ST-01  ",
            scheduledDateTimeIso = "  2026-09-18T10:00:00Z  ",
            signature = "  SIG_ABC  "
        )

        assertEquals("SSMTS-QR|RES-999|200012345678|ST-01|2026-09-18T10:00:00Z|SIG_ABC", payload)
    }

    /**
     * Verifies delimiter injection is rejected if reservation ID contains the payload delimiter.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_reservationIdWithDelimiter_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES|INJECTION",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies delimiter injection is rejected if prosumer NIC contains delimiter.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_nicWithDelimiter_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "1995|12345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies delimiter injection is rejected if station ID contains delimiter.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_stationIdWithDelimiter_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "199512345678",
            stationId = "STATION|01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies delimiter injection is rejected if signature contains delimiter.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_signatureWithDelimiter_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig|123"
        )
    }

    /**
     * Verifies that blank reservation ID throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankReservationId_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "   ",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies that blank prosumer NIC throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankNic_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies that blank station ID throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankStationId_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "199512345678",
            stationId = "",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = "sig123"
        )
    }

    /**
     * Verifies that blank scheduled ISO date throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankScheduledIso_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "",
            signature = "sig123"
        )
    }

    /**
     * Verifies that blank signature throws an IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException::class)
    fun buildCanonicalPayload_blankSignature_throwsException() {
        QrCodeGenerator.buildCanonicalPayload(
            reservationId = "RES-01",
            prosumerNic = "199512345678",
            stationId = "STATION-01",
            scheduledDateTimeIso = "2026-09-16T14:30:00Z",
            signature = ""
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
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    /**
     * Asserts generation succeeds at the minimum allowable dimension boundary (64px).
     */
    @Test
    fun generateQrBitmap_minimumDimensionBoundary_succeeds() {
        val payload = "SSMTS-QR|RES-MIN|199012345678|STATION-01|2026-09-16T10:00:00Z|MOCKSIG"
        val dimension = 64

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
     * Verifies that custom foreground and background colors are accurately applied to the QR bitmap.
     */
    @Test
    fun generateQrBitmap_customColors_appliesSpecifiedColors() {
        val payload = "SSMTS-QR|RES-COLOR|199012345678|STATION-01|2026-09-16T10:00:00Z|MOCKSIG"
        val dimension = 128
        val customFg = Color.BLUE
        val customBg = Color.YELLOW

        val bitmap = QrCodeGenerator.generateQrBitmap(
            payload = payload,
            dimensionPx = dimension,
            foregroundColor = customFg,
            backgroundColor = customBg
        )

        var hasFgPixel = false
        var hasBgPixel = false

        for (x in 0 until dimension) {
            for (y in 0 until dimension) {
                val pixel = bitmap.getPixel(x, y)
                if (pixel == customFg) hasFgPixel = true
                if (pixel == customBg) hasBgPixel = true
                if (hasFgPixel && hasBgPixel) break
            }
        }

        assertTrue("Generated QR bitmap must contain custom foreground pixels", hasFgPixel)
        assertTrue("Generated QR bitmap must contain custom background pixels", hasBgPixel)
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
