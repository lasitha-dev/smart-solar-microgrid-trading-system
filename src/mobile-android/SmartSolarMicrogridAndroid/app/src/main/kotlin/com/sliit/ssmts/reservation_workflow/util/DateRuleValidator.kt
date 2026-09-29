/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Client-side validation for 7-day advance booking and 12-hour modification rules.
 */

package com.sliit.ssmts.reservation_workflow.util

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object DateRuleValidator {

    /**
     * Checks if the target date is within the next 7 days from today.
     * @param targetDate The date to validate.
     * @return true if valid (<= 7 days).
     */
    fun isWithin7Days(targetDate: Date): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val maxDate = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, 7)
        }.time

        val target = Calendar.getInstance().apply {
            time = targetDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        return !target.before(today) && !target.after(maxDate)
    }

    /**
     * Checks if there are at least 12 hours remaining before the scheduled time.
     * @param scheduledTime The scheduled date/time of the reservation.
     * @return true if valid (>= 12 hours remaining).
     */
    fun has12HoursRemaining(scheduledTime: Date): Boolean {
        val now = Date()
        val diffInMillis = scheduledTime.time - now.time
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        return diffInHours >= 12
    }
}
