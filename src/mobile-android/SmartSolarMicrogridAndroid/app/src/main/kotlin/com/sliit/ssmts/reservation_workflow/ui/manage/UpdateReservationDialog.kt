/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Dialog fragment to update a reservation date/time.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.reservation_workflow.ui.common.RuleViolationBanner
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
        
        btnDismiss.setOnClickListener {
            dismiss()
        }
        
        var selectedDate: Date? = null

        btnSelectNewDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    btnConfirm.isEnabled = true
                    btnSelectNewDate.text = "Date Selected: $year-${month+1}-$dayOfMonth"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
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
    }
}
