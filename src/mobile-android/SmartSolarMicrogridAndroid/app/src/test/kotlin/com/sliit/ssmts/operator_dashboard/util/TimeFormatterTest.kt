/**
 * Description: Unit test suite for TimeFormatter verifying countdown timer computation,
 * relative sync time formatting, and ISO date parsing.
 */
package com.sliit.ssmts.operator_dashboard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit test validating TimeFormatter functions.
 */
class TimeFormatterTest {

    /**
     * Verifies that target timestamps in the past return 'Now'.
     */
    @Test
    fun formatCountdown_targetInPast_returnsNow() {
        val now = 1000000L
        val past = now - 5000L
        val formatted = TimeFormatter.formatCountdown(targetMillis = past, nowMillis = now)
        assertEquals("Now", formatted)
    }

    /**
     * Verifies that target timestamp equal to current time returns 'Now'.
     */
    @Test
    fun formatCountdown_targetEqualToNow_returnsNow() {
        val now = 1000000L
        val formatted = TimeFormatter.formatCountdown(targetMillis = now, nowMillis = now)
        assertEquals("Now", formatted)
    }

    /**
     * Verifies countdown format when remaining time is greater than 1 hour.
     */
    @Test
    fun formatCountdown_greaterThanOneHour_formatsHoursMinutesSeconds() {
        val now = 1000000L
        val diff = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(15) + TimeUnit.SECONDS.toMillis(30)
        val target = now + diff

        val formatted = TimeFormatter.formatCountdown(targetMillis = target, nowMillis = now)
        assertEquals("02h 15m 30s", formatted)
    }

    /**
     * Verifies countdown format when remaining time is under 1 hour.
     */
    @Test
    fun formatCountdown_underOneHour_formatsMinutesSeconds() {
        val now = 1000000L
        val diff = TimeUnit.MINUTES.toMillis(8) + TimeUnit.SECONDS.toMillis(45)
        val target = now + diff

        val formatted = TimeFormatter.formatCountdown(targetMillis = target, nowMillis = now)
        assertEquals("08m 45s", formatted)
    }

    /**
     * Verifies that non-positive sync time returns 'Never'.
     */
    @Test
    fun formatSyncTime_zeroOrNegative_returnsNever() {
        assertEquals("Never", TimeFormatter.formatSyncTime(0L))
        assertEquals("Never", TimeFormatter.formatSyncTime(-100L))
    }

    /**
     * Verifies that valid epoch millis produces HH:mm:ss string.
     */
    @Test
    fun formatSyncTime_validMillis_returnsFormattedTime() {
        val formatted = TimeFormatter.formatSyncTime(1773720000000L)
        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}:\\d{2}")))
    }

    /**
     * Verifies ISO date formatting.
     */
    @Test
    fun formatIsoToReadable_validIso_returnsReadableDate() {
        val iso = "2026-09-17T14:30:00Z"
        val formatted = TimeFormatter.formatIsoToReadable(iso)
        assertTrue(formatted.contains("Sep 17, 2026"))
    }

    /**
     * Verifies ISO formatting handles null/blank gracefully.
     */
    @Test
    fun formatIsoToReadable_nullOrBlank_returnsFallback() {
        assertEquals("N/A", TimeFormatter.formatIsoToReadable(null))
        assertEquals("N/A", TimeFormatter.formatIsoToReadable("   "))
    }
}
