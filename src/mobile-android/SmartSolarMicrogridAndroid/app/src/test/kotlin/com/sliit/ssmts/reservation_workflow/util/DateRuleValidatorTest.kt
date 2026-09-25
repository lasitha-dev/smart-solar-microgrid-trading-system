/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for Android client-side date validation rules.
 */

package com.sliit.ssmts.reservation_workflow.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class DateRuleValidatorTest {

    @Test
    fun `isWithin7Days returns false when date is 8 days ahead`() {
        val targetDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 8)
        }.time

        val result = DateRuleValidator.isWithin7Days(targetDate)

        assertFalse("Should be false for > 7 days", result)
    }

    @Test
    fun `isWithin7Days returns true when date is 3 days ahead`() {
        val targetDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
        }.time

        val result = DateRuleValidator.isWithin7Days(targetDate)

        assertTrue("Should be true for <= 7 days", result)
    }

    @Test
    fun `has12HoursRemaining returns false when 5 hours away`() {
        val targetDate = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 5)
        }.time

        val result = DateRuleValidator.has12HoursRemaining(targetDate)

        assertFalse("Should be false when < 12 hours away", result)
    }

    @Test
    fun `has12HoursRemaining returns true when 24 hours away`() {
        val targetDate = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 24)
        }.time

        val result = DateRuleValidator.has12HoursRemaining(targetDate)

        assertTrue("Should be true when >= 12 hours away", result)
    }
}
