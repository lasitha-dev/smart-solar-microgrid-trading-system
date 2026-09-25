/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Dialog fragment to update a reservation date/time with 12-hour and 7-day rule enforcement.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import android.app.DatePickerDialog
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
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.reservation_workflow.util.DateRuleValidator
import com.sliit.ssmts.reservation_workflow.util.DateTimeFormatter
import java.util.Calendar
import java.util.Date

class UpdateReservationDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_update_reservation, container, false)
        
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val btnDismiss = view.findViewById<Button>(R.id.btnDismiss)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmUpdate)
        val btnSelectNewDate = view.findViewById<Button>(R.id.btnSelectNewDate)
        val cardRuleViolation = view.findViewById<MaterialCardView>(R.id.cardRuleViolation)
        val tvRuleViolationMessage = view.findViewById<TextView>(R.id.tvRuleViolationMessage)
        val tvNewSlotInfo = view.findViewById<TextView>(R.id.tvNewSlotInfo)
        
        btnDismiss.setOnClickListener {
            dismiss()
        }

        val scheduledDateMillis = arguments?.getLong(ARG_SCHEDULED_MILLIS, 0L) ?: 0L
        if (scheduledDateMillis > 0L) {
            val scheduledDate = Date(scheduledDateMillis)
            val canModify = DateRuleValidator.has12HoursRemaining(scheduledDate)
            if (!canModify) {
                cardRuleViolation.visibility = View.VISIBLE
                tvRuleViolationMessage.setText(R.string.err_12_hour_rule_update)
                btnSelectNewDate.isEnabled = false
                btnConfirm.isEnabled = false
            }
        }
        
        var selectedDate: Date? = null

        btnSelectNewDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val picked = calendar.time
                    if (DateRuleValidator.isWithin7Days(picked)) {
                        selectedDate = picked
                        btnConfirm.isEnabled = true
                        tvNewSlotInfo.text = DateTimeFormatter.toDisplayString(picked)
                        btnSelectNewDate.text = getString(R.string.label_selected_date, "${year}-${month + 1}-${dayOfMonth}")
                    } else {
                        Toast.makeText(requireContext(), "Reservation must be within 7 days from today.", Toast.LENGTH_SHORT).show()
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                // Strict 7-day maximum date limit
                datePicker.maxDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
                show()
            }
        }
        
        btnConfirm.setOnClickListener {
            if (selectedDate != null) {
                val listener = activity as? OnUpdateConfirmedListener
                if (listener != null) {
                    listener.onUpdateConfirmed(selectedDate!!)
                    dismiss()
                } else {
                    Toast.makeText(context, "Activity must implement OnUpdateConfirmedListener", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        return view
    }
    
    interface OnUpdateConfirmedListener {
        fun onUpdateConfirmed(newDate: Date)
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    companion object {
        const val TAG = "UpdateReservationDialog"
        private const val ARG_SCHEDULED_MILLIS = "arg_scheduled_millis"

        fun newInstance(scheduledDateMillis: Long): UpdateReservationDialog {
            val dialog = UpdateReservationDialog()
            val args = Bundle().apply {
                putLong(ARG_SCHEDULED_MILLIS, scheduledDateMillis)
            }
            dialog.arguments = args
            return dialog
        }
    }
}
