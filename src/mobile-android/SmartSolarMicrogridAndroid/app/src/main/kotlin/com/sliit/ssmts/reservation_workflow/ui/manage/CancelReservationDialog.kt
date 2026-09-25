/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Dialog fragment to confirm cancellation of a reservation with 12-hour rule check.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.reservation_workflow.util.DateRuleValidator
import java.util.Date

class CancelReservationDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_cancel_reservation, container, false)
        
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val btnDismiss = view.findViewById<Button>(R.id.btnDismiss)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmCancel)
        val cardRuleViolation = view.findViewById<MaterialCardView>(R.id.cardRuleViolation)
        val tvRuleViolationMessage = view.findViewById<TextView>(R.id.tvRuleViolationMessage)
        val etCancelReason = view.findViewById<TextInputEditText>(R.id.etCancelReason)
        
        btnDismiss.setOnClickListener {
            dismiss()
        }

        val scheduledDateMillis = arguments?.getLong(ARG_SCHEDULED_MILLIS, 0L) ?: 0L
        if (scheduledDateMillis > 0L) {
            val scheduledDate = Date(scheduledDateMillis)
            val canCancel = DateRuleValidator.has12HoursRemaining(scheduledDate)
            if (!canCancel) {
                cardRuleViolation.visibility = View.VISIBLE
                tvRuleViolationMessage.setText(R.string.err_12_hour_rule_cancel)
                btnConfirm.isEnabled = false
            }
        }
        
        btnConfirm.setOnClickListener {
            val listener = activity as? OnCancelConfirmedListener
            if (listener != null) {
                val reason = etCancelReason.text?.toString()?.trim()
                listener.onCancelConfirmed(if (reason.isNullOrEmpty()) null else reason)
                dismiss()
            } else {
                Toast.makeText(context, "Activity must implement OnCancelConfirmedListener", Toast.LENGTH_SHORT).show()
            }
        }
        
        return view
    }
    
    interface OnCancelConfirmedListener {
        fun onCancelConfirmed(reason: String? = null)
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    companion object {
        const val TAG = "CancelReservationDialog"
        private const val ARG_SCHEDULED_MILLIS = "arg_scheduled_millis"

        fun newInstance(scheduledDateMillis: Long): CancelReservationDialog {
            val dialog = CancelReservationDialog()
            val args = Bundle().apply {
                putLong(ARG_SCHEDULED_MILLIS, scheduledDateMillis)
            }
            dialog.arguments = args
            return dialog
        }
    }
}
