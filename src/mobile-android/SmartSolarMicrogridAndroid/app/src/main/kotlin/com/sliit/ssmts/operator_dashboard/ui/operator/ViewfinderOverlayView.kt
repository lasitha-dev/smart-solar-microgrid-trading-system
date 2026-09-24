/**
 * Description: Custom overlay rendering viewfinder framing box, transparent cutout,
 * and corner alignment guides for the camera QR scanner (FR-M4-05.2).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.sliit.ssmts.R

/**
 * Visual viewfinder overlay providing framing guides and dimmed backdrop around the QR target.
 */
class ViewfinderOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val scrimPaint = Paint().apply {
        color = 0x99000000.toInt() // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val cornerPaint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.color_secondary_variant)
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.scanner_corner_thickness)
        strokeCap = Paint.Cap.ROUND
    }

    private val framingRect = RectF()
    private val cornerLength = resources.getDimension(R.dimen.scanner_corner_length)
    private val framingSize = resources.getDimension(R.dimen.scanner_framing_size)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Computes the centered framing square coordinates when the view dimensions change.
     *
     * @param w Current view width.
     * @param h Current view height.
     * @param oldw Old view width.
     * @param oldh Old view height.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val left = (w - framingSize) / 2f
        val top = (h - framingSize) / 2f
        framingRect.set(left, top, left + framingSize, top + framingSize)
    }

    /**
     * Renders the darkened scrim, transparent scanning aperture cutout, and illuminated corner brackets.
     *
     * @param canvas The Canvas on which the viewfinder guide is drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw dark background scrim
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        // Cut out transparent center scanning aperture
        canvas.drawRect(framingRect, eraserPaint)

        // Draw 4 corner alignment markers
        val halfStroke = cornerPaint.strokeWidth / 2f
        val l = framingRect.left + halfStroke
        val t = framingRect.top + halfStroke
        val r = framingRect.right - halfStroke
        val b = framingRect.bottom - halfStroke

        // Top-Left Corner
        canvas.drawLine(l, t, l + cornerLength, t, cornerPaint)
        canvas.drawLine(l, t, l, t + cornerLength, cornerPaint)

        // Top-Right Corner
        canvas.drawLine(r, t, r - cornerLength, t, cornerPaint)
        canvas.drawLine(r, t, r, t + cornerLength, cornerPaint)

        // Bottom-Left Corner
        canvas.drawLine(l, b, l + cornerLength, b, cornerPaint)
        canvas.drawLine(l, b, l, b - cornerLength, cornerPaint)

        // Bottom-Right Corner
        canvas.drawLine(r, b, r - cornerLength, b, cornerPaint)
        canvas.drawLine(r, b, r, b - cornerLength, cornerPaint)
    }
}
