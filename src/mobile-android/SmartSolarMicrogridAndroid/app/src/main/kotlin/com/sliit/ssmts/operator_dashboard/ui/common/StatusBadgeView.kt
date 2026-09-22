/**
 * Description: Custom color-coded status badge TextView displaying standardized status styles,
 * background containers, and typography according to FR-M4-03.4 design specifications.
 */
package com.sliit.ssmts.operator_dashboard.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.google.android.material.textview.MaterialTextView
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus

/**
 * Custom TextView component rendering the standardized color-coded status badges for reservations.
 */
class StatusBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {

    /**
     * Currently rendered reservation status.
     */
    var currentStatus: ReservationStatus? = null
        private set

    init {
        gravity = Gravity.CENTER
        includeFontPadding = false

        val padHorizontal = resources.getDimensionPixelSize(R.dimen.badge_padding_horizontal)
        val padVertical = resources.getDimensionPixelSize(R.dimen.badge_padding_vertical)
        setPadding(padHorizontal, padVertical, padHorizontal, padVertical)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StatusBadgeView, defStyleAttr, 0)
            val badgeStatusOrdinal = typedArray.getInt(R.styleable.StatusBadgeView_badgeStatus, -1)
            typedArray.recycle()

            when (badgeStatusOrdinal) {
                0 -> setStatus(ReservationStatus.PENDING)
                1 -> setStatus(ReservationStatus.APPROVED)
                2 -> setStatus(ReservationStatus.COMPLETED)
                3 -> setStatus(ReservationStatus.CANCELLED)
                else -> setStatus(ReservationStatus.PENDING)
            }
        } else {
            setStatus(ReservationStatus.PENDING)
        }
    }

    /**
     * Applies color, background, and localized text based on the given ReservationStatus enum.
     *
     * @param status The target reservation status to render.
     */
    fun setStatus(status: ReservationStatus) {
        this.currentStatus = status
        when (status) {
            ReservationStatus.COMPLETED -> {
                setText(R.string.status_completed)
                setTextColor(ContextCompat.getColor(context, R.color.status_completed_text))
                setBackgroundResource(R.drawable.bg_status_completed)
            }
            ReservationStatus.APPROVED -> {
                setText(R.string.status_approved)
                setTextColor(ContextCompat.getColor(context, R.color.status_approved_text))
                setBackgroundResource(R.drawable.bg_status_approved)
            }
            ReservationStatus.PENDING -> {
                setText(R.string.status_pending)
                setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                setBackgroundResource(R.drawable.bg_status_pending)
            }
            ReservationStatus.CANCELLED -> {
                setText(R.string.status_cancelled)
                setTextColor(ContextCompat.getColor(context, R.color.status_cancelled_text))
                setBackgroundResource(R.drawable.bg_status_cancelled)
            }
        }
    }

    /**
     * Overload that parses a string representation into ReservationStatus and applies badge styling.
     *
     * @param statusString String representation of status ('Approved', 'Completed', etc.).
     */
    fun setStatus(statusString: String?) {
        setStatus(ReservationStatus.fromString(statusString))
    }
}
