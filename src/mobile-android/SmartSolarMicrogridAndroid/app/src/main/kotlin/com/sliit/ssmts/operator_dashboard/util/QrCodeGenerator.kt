/**
 * Description: Native QR code generator utility utilizing ZXing to render cryptographically
 * structured, high-contrast QR tokens for approved microgrid energy reservations.
 */
package com.sliit.ssmts.operator_dashboard.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Utility responsible for constructing canonical QR payloads and generating high-contrast QR bitmaps.
 */
object QrCodeGenerator {

    const val PAYLOAD_PREFIX = "SSMTS-QR"
    const val DELIMITER = "|"
    const val DEFAULT_DIMENSION_PX = 512
    const val MIN_DIMENSION_PX = 64

    /**
     * Constructs the canonical QR token string from approved reservation attributes.
     *
     * @param reservationId Remote MongoDB reservation identifier.
     * @param prosumerNic Prosumer National Identity Card identifier.
     * @param stationId Solar microgrid station hub identifier.
     * @param scheduledDateTimeIso ISO-8601 representation of the scheduled transfer slot.
     * @param signature Cryptographic HMAC-SHA256 signature string.
     * @return Canonical delimited payload string.
     * @throws IllegalArgumentException If any argument is blank or contains the payload delimiter.
     */
    fun buildCanonicalPayload(
        reservationId: String,
        prosumerNic: String,
        stationId: String,
        scheduledDateTimeIso: String,
        signature: String
    ): String {
        require(reservationId.isNotBlank()) { "Reservation ID cannot be blank." }
        require(prosumerNic.isNotBlank()) { "Prosumer NIC cannot be blank." }
        require(stationId.isNotBlank()) { "Station ID cannot be blank." }
        require(scheduledDateTimeIso.isNotBlank()) { "Scheduled date-time cannot be blank." }
        require(signature.isNotBlank()) { "Signature cannot be blank." }

        val components = listOf(reservationId, prosumerNic, stationId, scheduledDateTimeIso, signature)
        for (component in components) {
            require(!component.contains(DELIMITER)) {
                "Payload field cannot contain delimiter '$DELIMITER': $component"
            }
        }

        return listOf(
            PAYLOAD_PREFIX,
            reservationId.trim(),
            prosumerNic.trim(),
            stationId.trim(),
            scheduledDateTimeIso.trim(),
            signature.trim()
        ).joinToString(DELIMITER)
    }

    /**
     * Renders a high-contrast QR code Bitmap from a structured payload string.
     *
     * @param payload The raw string token to encode into the QR code matrix.
     * @param dimensionPx The square dimension (width and height) in pixels.
     * @param foregroundColor The pixel color for QR dark modules (defaults to Color.BLACK).
     * @param backgroundColor The pixel color for QR light modules (defaults to Color.WHITE).
     * @return Initialized Bitmap containing the rendered QR code.
     * @throws IllegalArgumentException If payload is blank or dimension is below minimum bounds.
     */
    fun generateQrBitmap(
        payload: String,
        dimensionPx: Int = DEFAULT_DIMENSION_PX,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        require(payload.isNotBlank()) { "QR payload string cannot be blank." }
        require(dimensionPx >= MIN_DIMENSION_PX) {
            "QR code dimension must be at least $MIN_DIMENSION_PX pixels; requested $dimensionPx"
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 1)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, dimensionPx, dimensionPx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Builds the canonical payload and renders the corresponding QR code Bitmap in a single invocation.
     *
     * @param reservationId Remote reservation identifier.
     * @param prosumerNic Prosumer NIC identifier.
     * @param stationId Solar station identifier.
     * @param scheduledDateTimeIso Scheduled transfer slot in ISO format.
     * @param signature Cryptographic HMAC-SHA256 signature.
     * @param dimensionPx Dimension in pixels for the generated Bitmap.
     * @return Rendered high-contrast Bitmap ready for display or sharing.
     */
    fun generateReservationQrBitmap(
        reservationId: String,
        prosumerNic: String,
        stationId: String,
        scheduledDateTimeIso: String,
        signature: String,
        dimensionPx: Int = DEFAULT_DIMENSION_PX
    ): Bitmap {
        val payload = buildCanonicalPayload(
            reservationId = reservationId,
            prosumerNic = prosumerNic,
            stationId = stationId,
            scheduledDateTimeIso = scheduledDateTimeIso,
            signature = signature
        )
        return generateQrBitmap(payload = payload, dimensionPx = dimensionPx)
    }
}
