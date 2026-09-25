/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Helper utility to display rule violation banners (e.g. 12-hour rule block).
 */

package com.sliit.ssmts.reservation_workflow.ui.common

import android.view.View
import com.google.android.material.snackbar.Snackbar

object RuleViolationBanner {
    
    fun show(view: View, message: String) {
        // Displays a highly visible red banner for business rule violations
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(android.graphics.Color.parseColor("#D32F2F")) // Material Red
            .setTextColor(android.graphics.Color.WHITE)
            .show()
    }
}
