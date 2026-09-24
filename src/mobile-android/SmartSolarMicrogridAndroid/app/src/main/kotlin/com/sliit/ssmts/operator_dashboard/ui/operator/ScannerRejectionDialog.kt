/**
 * Description: Modular dialog helper formatting and presenting operator verification
 * rejection alerts with mapped domain error codes (FR-M4-06.3).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sliit.ssmts.R

/**
 * Utility displaying explicit error explanation dialogs on server verification rejection.
 */
object ScannerRejectionDialog {

    /**
     * Maps central API rejection codes to localized descriptive string explanations.
     *
     * @param context Host Android context.
     * @param errorCode Machine-readable rejection code from central API.
     * @param fallbackMessage Default message if code is unmapped.
     * @return Localized user-friendly error message.
     */
    fun mapErrorCodeToMessage(
        context: Context,
        errorCode: String?,
        fallbackMessage: String
    ): String {
        return when (errorCode) {
            "ERR_RESERVATION_ALREADY_COMPLETED" -> context.getString(R.string.err_already_completed)
            "ERR_INVALID_QR_SIGNATURE" -> context.getString(R.string.err_invalid_signature)
            "ERR_RESERVATION_NOT_FOUND" -> context.getString(R.string.err_reservation_not_found)
            "ERR_RESERVATION_NOT_APPROVED" -> context.getString(R.string.err_not_approved)
            "ERR_OUTSIDE_OPERATIONAL_WINDOW" -> context.getString(R.string.err_outside_window)
            "ERR_MALFORMED_QR" -> context.getString(R.string.err_malformed_qr)
            else -> fallbackMessage
        }
    }

    /**
     * Presents an error dialog mapping structured backend error codes to user-facing strings.
     *
     * @param context Host Android context.
     * @param errorCode Machine-readable rejection code from central API.
     * @param message Fallback error message.
     * @param onDismiss Callback executed when the operator dismisses the dialog.
     * @return The displayed AlertDialog instance.
     */
    fun show(
        context: Context,
        errorCode: String?,
        message: String,
        onDismiss: () -> Unit
    ): androidx.appcompat.app.AlertDialog {
        val displayMessage = mapErrorCodeToMessage(context, errorCode, message)

        return MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.qr_error_title))
            .setMessage(displayMessage)
            .setPositiveButton(context.getString(R.string.btn_dismiss)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                onDismiss()
            }
            .show()
    }
}
