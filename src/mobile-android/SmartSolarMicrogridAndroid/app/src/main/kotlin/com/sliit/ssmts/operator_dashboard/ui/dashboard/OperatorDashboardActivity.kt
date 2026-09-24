/**
 * Description: Main launcher activity hosting the Operational Dashboard fragment,
 * providing top toolbar navigation to Booking History and the floating FAB to the native Camera QR Scanner (FR-M4-01, FR-M4-03, FR-M4-04).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sliit.ssmts.R
import com.sliit.ssmts.databinding.ActivityOperatorDashboardBinding
import com.sliit.ssmts.operator_dashboard.ui.history.BookingHistoryActivity
import com.sliit.ssmts.operator_dashboard.ui.operator.OperatorScannerActivity

import androidx.lifecycle.lifecycleScope
import com.sliit.ssmts.util.SessionManager
import kotlinx.coroutines.launch

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
        loadOperatorDetails()
    }

    /**
     * Loads active operator credentials from local SQLite session storage.
     */
    private fun loadOperatorDetails() {
        lifecycleScope.launch {
            val session = SessionManager(this@OperatorDashboardActivity).getActiveSession()
            if (session != null) {
                val displayName = if (session.fullName.isNotBlank()) session.fullName else session.username
                binding.toolbarDashboard.subtitle = "Operator: $displayName (${session.nic})"
            }
        }
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
                R.id.action_logout -> {
                    logoutOperator()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Clears local SQLite session credentials and routes back to LoginActivity.
     */
    private fun logoutOperator() {
        lifecycleScope.launch {
            SessionManager(this@OperatorDashboardActivity).clearSession()
            val intent = Intent(this@OperatorDashboardActivity, com.sliit.ssmts.ui.auth.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
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
