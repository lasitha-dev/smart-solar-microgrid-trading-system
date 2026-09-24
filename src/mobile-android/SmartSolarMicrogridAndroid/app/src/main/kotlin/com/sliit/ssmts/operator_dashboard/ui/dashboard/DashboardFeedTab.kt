/**
 * Description: Tab enumeration representing active vs pending queue views in the operational dashboard feed (FR-M4-02).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

/**
 * Enumerates the operational feeds available for monitoring on the dashboard.
 */
enum class DashboardFeedTab {
    /**
     * Active reservations scheduled for the current calendar date (FR-M4-02.1).
     */
    TODAY_ACTIVE,

    /**
     * Pending reservations awaiting operator verification or administrative processing (FR-M4-02.2).
     */
    PENDING_QUEUE
}
