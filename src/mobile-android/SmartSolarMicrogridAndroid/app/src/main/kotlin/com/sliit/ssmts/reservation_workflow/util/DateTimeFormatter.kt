/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Helper to format dates to/from ISO 8601 strings.
 */

package com.sliit.ssmts.reservation_workflow.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeFormatter {
    private const val ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val DISPLAY_FORMAT = "MMM dd, yyyy - hh:mm a"

    fun toIso8601(date: Date): String {
        val sdf = SimpleDateFormat(ISO_8601_FORMAT, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    fun fromIso8601(isoString: String): Date? {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            val cleanIso = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
            sdf.parse(cleanIso)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toDisplayString(date: Date): String {
        val sdf = SimpleDateFormat(DISPLAY_FORMAT, Locale.getDefault())
        return sdf.format(date)
    }
}
