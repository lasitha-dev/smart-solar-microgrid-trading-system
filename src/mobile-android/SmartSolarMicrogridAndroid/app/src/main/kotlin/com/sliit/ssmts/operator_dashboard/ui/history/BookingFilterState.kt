/**
 * Description: Type-safe enumeration of interactive filter chip states for booking history (FR-M4-03.3).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import com.sliit.ssmts.R

/**
 * Represents the 5 distinct filter states for energy trading reservations.
 *
 * @property filterValue Filter parameter dispatched to repository queries.
 * @property chipId View resource identifier of the corresponding FilterChip.
 */
enum class BookingFilterState(val filterValue: String, val chipId: Int) {
    /** Matches all reservation records regardless of status. */
    ALL("All", R.id.chipFilterAll),

    /** Filter for reservations awaiting operator or administrative review. */
    PENDING("Pending", R.id.chipFilterPending),

    /** Filter for approved reservations ready for physical energy transfer. */
    APPROVED("Approved", R.id.chipFilterApproved),

    /** Filter for finalized energy trading transactions. */
    COMPLETED("Completed", R.id.chipFilterCompleted),

    /** Filter for invalidated or cancelled booking requests. */
    CANCELLED("Cancelled", R.id.chipFilterCancelled);

    companion object {
        /**
         * Resolves the BookingFilterState matching the provided chip view ID, defaulting to ALL if unknown.
         *
         * @param id View identifier of the selected chip.
         * @return Resolved BookingFilterState enum constant.
         */
        fun fromChipId(id: Int): BookingFilterState {
            return entries.firstOrNull { it.chipId == id } ?: ALL
        }
    }
}
