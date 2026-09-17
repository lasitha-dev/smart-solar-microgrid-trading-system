/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Dialog fragment to confirm cancellation of a reservation.
 */

package com.sliit.ssmts.reservation_workflow.ui.manage

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.reservation_workflow.ui.common.RuleViolationBanner

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
        
        btnDismiss.setOnClickListener {
            dismiss()
        }
        
        btnConfirm.setOnClickListener {
            val listener = activity as? OnCancelConfirmedListener
            if (listener != null) {
                listener.onCancelConfirmed()
                dismiss()
            } else {
                Toast.makeText(context, "Activity must implement OnCancelConfirmedListener", Toast.LENGTH_SHORT).show()
            }
        }
        
        return view
    }
    
    interface OnCancelConfirmedListener {
        fun onCancelConfirmed()
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
    }
}
