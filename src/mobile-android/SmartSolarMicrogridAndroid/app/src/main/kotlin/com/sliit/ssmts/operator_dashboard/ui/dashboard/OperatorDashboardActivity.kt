/**
 * Description: Main launcher activity hosting the Operational Dashboard fragment,
 * providing top toolbar navigation to Booking History and the floating FAB to the native Camera QR Scanner (FR-M4-01, FR-M4-03, FR-M4-04).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ActivityOperatorDashboardBinding
import com.sliit.ssmts.operator_dashboard.ui.history.BookingHistoryActivity
import com.sliit.ssmts.operator_dashboard.ui.operator.OperatorScannerActivity

/**
 * Primary activity entry point for the Grid Operator component.
 * Hosts DashboardFragment within a CoordinatorLayout and manages high-level operator workflows.
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding

    /**
     * Initializes the view hierarchy, toolbar menu listeners, and scanner FAB navigation.
     *
     * @param savedInstanceState Saved bundle state if restoring.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFab()
    }

    /**
     * Configures the top application toolbar and attaches menu item click listeners.
     */
    private fun setupToolbar() {
        binding.toolbarDashboard.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_history -> {
                    navigateToBookingHistory()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Configures the floating action button to launch the CameraX QR code scanner viewfinder.
     */
    private fun setupFab() {
        binding.fabScanQr.setOnClickListener {
            navigateToScanner()
        }
    }

    /**
     * Dispatches intent to open the BookingHistoryActivity with search and filter capabilities.
     */
    private fun navigateToBookingHistory() {
        val intent = Intent(this, BookingHistoryActivity::class.java)
        startActivity(intent)
    }

    /**
     * Dispatches intent to open the native OperatorScannerActivity for camera QR scanning and verification.
     */
    private fun navigateToScanner() {
        val intent = Intent(this, OperatorScannerActivity::class.java)
        startActivity(intent)
    }
}
