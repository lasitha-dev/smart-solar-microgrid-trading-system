/**
 * Description: Time and countdown formatting utility providing human-readable representations
 * for upcoming booking countdowns and offline cache synchronization timestamps.
 */
package com.sliit.ssmts.operator_dashboard.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Utility containing formatting logic for timestamps, countdown timers, and sync times.
 */
object TimeFormatter {

    /**
     * Formats the remaining duration between the current time and a scheduled booking slot.
     *
     * @param targetMillis Target scheduled timestamp in epoch milliseconds.
     * @param nowMillis Current timestamp in epoch milliseconds (defaults to System.currentTimeMillis()).
     * @return Formatted countdown string (e.g. "02h 15m 30s", "05m 12s", or "Now").
     */
    fun formatCountdown(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = targetMillis - nowMillis
        if (diffMillis <= 0) {
            return "Now"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        }
    }

    /**
     * Formats an epoch timestamp into a clean 24-hour sync time string.
     *
     * @param millis Epoch timestamp in milliseconds.
     * @return Formatted time string (e.g., "14:35:10").
     */
    fun formatSyncTime(millis: Long): String {
        if (millis <= 0) return "Never"
        val format = SimpleDateFormat("HH:mm:ss", Locale.US)
        return format.format(Date(millis))
    }

    /**
     * Parses an ISO date-time string and returns a human-readable display string.
     *
     * @param isoString ISO-8601 formatted date-time string.
     * @return Human-readable display string or original string if parsing fails.
     */
    fun formatIsoToReadable(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "N/A"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(isoString) ?: return isoString
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.US)
            formatter.format(date)
        } catch (_: Exception) {
            isoString
        }
    }
}
