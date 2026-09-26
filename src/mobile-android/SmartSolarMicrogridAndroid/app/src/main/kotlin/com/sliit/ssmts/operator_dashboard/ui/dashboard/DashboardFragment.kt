/**
 * Description: Operational Dashboard Fragment rendering live counters, active spotlight card,
 * countdown timers, offline indicator banner, and real-time operational booking feeds (FR-M4-01, FR-M4-02).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.repository.DashboardRepositoryImpl
import com.sliit.ssmts.databinding.FragmentDashboardBinding
import com.sliit.ssmts.operator_dashboard.domain.model.ActiveSpotlightReservation
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.TimeFormatter
import com.sliit.ssmts.util.SessionManager
import kotlinx.coroutines.launch

/**
 * Fragment rendering live operational metrics cards, active booking spotlight, and real-time booking feeds.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var feedAdapter: BookingsFeedAdapter

    private val viewModel: DashboardViewModel by viewModels {
        val appContext = requireContext().applicationContext
        val database = SsmtsDatabase.getInstance(appContext)
        val sessionManager = SessionManager(appContext)
        val api = ApiClient.createOperatorDashboardApi(
            baseUrl = ApiClient.getBaseUrl(appContext),
            tokenProvider = { sessionManager.getAuthToken() }
        )
        val repository = DashboardRepositoryImpl(api, database.reservationCacheDao())
        val operatorId = sessionManager.getActiveUserId()
        DashboardViewModel.Factory(repository, operatorId)
    }

    /**
     * Inflates the fragment dashboard ViewBinding hierarchy.
     *
     * @param inflater The LayoutInflater object to inflate views.
     * @param container Optional parent container view.
     * @param savedInstanceState Previous saved state if available.
     * @return The root View of the inflated layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initializes UI interactions and observers once the view hierarchy is created.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Previous saved state if available.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeViewModel()
        viewModel.refresh()
    }

    private fun setupInteractions() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.solar_amber_primary,
            R.color.emerald_accent
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnRetrySync.setOnClickListener {
            viewModel.refresh()
        }

        binding.btnErrorRetry.setOnClickListener {
            viewModel.loadMetrics(forceRefresh = true)
        }

        feedAdapter = BookingsFeedAdapter(
            onItemClick = null,
            onApproveClick = { reservation ->
                showApproveConfirmation(reservation)
            },
            onRejectClick = { reservation ->
                showRejectConfirmation(reservation)
            }
        )
        binding.rvBookingsFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookingsFeed.adapter = feedAdapter

        binding.toggleGroupFeed.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTabTodayActive -> viewModel.selectFeedTab(DashboardFeedTab.TODAY_ACTIVE)
                    R.id.btnTabPendingQueue -> viewModel.selectFeedTab(DashboardFeedTab.PENDING_QUEUE)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        binding.swipeRefreshLayout.isRefreshing = refreshing
                    }
                }
                launch {
                    viewModel.todayActiveBookings.collect { list ->
                        binding.btnTabTodayActive.text = getString(R.string.tab_today_active_format, list.size)
                    }
                }
                launch {
                    viewModel.pendingQueueBookings.collect { list ->
                        binding.btnTabPendingQueue.text = getString(R.string.tab_pending_queue_format, list.size)
                    }
                }
                launch {
                    viewModel.feedReservations.collect { reservations ->
                        feedAdapter.submitList(reservations)
                        val isEmpty = reservations.isEmpty()
                        binding.rvBookingsFeed.isVisible = !isEmpty
                        binding.layoutFeedEmpty.isVisible = isEmpty
                        if (isEmpty) {
                            val emptyMsgRes = if (viewModel.selectedFeedTab.value == DashboardFeedTab.TODAY_ACTIVE) {
                                R.string.feed_empty_active
                            } else {
                                R.string.feed_empty_pending
                            }
                            binding.tvFeedEmptyMessage.setText(emptyMsgRes)
                        }
                    }
                }
            }
        }
    }

    private fun renderUiState(state: UiState<DashboardMetrics>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.layoutError.isVisible = false
            }
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                binding.layoutError.isVisible = false
                bindMetrics(state.data)
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                binding.layoutError.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
            is UiState.Idle -> {
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun bindMetrics(metrics: DashboardMetrics) {
        // Counter Cards
        binding.tvPendingCount.text = metrics.pendingReservationsCount.toString()
        binding.tvApprovedFutureCount.text = metrics.approvedFutureReservationsCount.toString()
        binding.tvCompletedTodayCount.text = metrics.completedTodayCount.toString()

        // Offline Banner (FR-M4-01.5)
        binding.cardOfflineBanner.isVisible = metrics.isOfflineCached
        if (metrics.isOfflineCached) {
            val syncFormatted = TimeFormatter.formatSyncTime(metrics.lastSyncedAtMillis)
            binding.tvLastSynced.text = getString(R.string.banner_last_synced_format, syncFormatted)
        }

        // Active Spotlight Widget (FR-M4-01.3)
        bindSpotlight(metrics.activeSpotlight)
    }

    private fun bindSpotlight(spotlight: ActiveSpotlightReservation?) {
        if (spotlight != null) {
            binding.layoutSpotlightContent.isVisible = true
            binding.tvSpotlightEmpty.isVisible = false

            binding.tvSpotlightStation.text = spotlight.stationName
            binding.tvSpotlightBay.text = getString(R.string.spotlight_bay_prefix, spotlight.allocatedBayId)
            binding.tvSpotlightEstimatedPower.text = getString(R.string.spotlight_estimated_prefix, spotlight.estimatedKwh)

            val countdownText = TimeFormatter.formatCountdown(spotlight.scheduledTimeMillis)
            binding.tvSpotlightCountdown.text = getString(R.string.spotlight_countdown_prefix, countdownText)
        } else {
            binding.layoutSpotlightContent.isVisible = false
            binding.tvSpotlightEmpty.isVisible = true
        }
    }

    /**
     * Prompts the operator with a confirmation dialog before committing reservation approval.
     *
     * @param reservation Reservation domain entity awaiting operator validation.
     */
    private fun showApproveConfirmation(reservation: Reservation) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_operator_approval)
            .setMessage(getString(R.string.confirm_approve_booking))
            .setPositiveButton(R.string.confirm) { _, _ ->
                binding.progressBar.isVisible = true
                viewModel.approveReservation(reservation.id) { success, errorMsg ->
                    binding.progressBar.isVisible = false
                    if (success) {
                        Snackbar.make(binding.root, R.string.msg_approve_success, Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, errorMsg ?: "Approval failed.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Prompts the operator with a confirmation and reason dialog before rejecting a pending reservation.
     *
     * @param reservation Reservation domain entity awaiting operator rejection.
     */
    private fun showRejectConfirmation(reservation: Reservation) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Reason for rejection (optional)"
            setTextColor(requireContext().getColor(R.color.text_primary))
            setHintTextColor(requireContext().getColor(R.color.text_secondary))
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject Reservation")
            .setMessage("Are you sure you want to reject this reservation for ${reservation.prosumerNic}? The allocated slot and battery bay will be released.")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                binding.progressBar.isVisible = true
                viewModel.rejectReservation(reservation.id, if (reason.isNotBlank()) reason else null) { success, errorMsg ->
                    binding.progressBar.isVisible = false
                    if (success) {
                        Snackbar.make(binding.root, "Reservation rejected successfully.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, errorMsg ?: "Rejection failed.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Nullifies ViewBinding reference to prevent view-hierarchy memory leaks per Section 3 mandates.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
