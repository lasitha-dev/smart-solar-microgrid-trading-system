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
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.repository.DashboardRepositoryImpl
import com.sliit.ssmts.operator_dashboard.databinding.FragmentDashboardBinding
import com.sliit.ssmts.operator_dashboard.domain.model.ActiveSpotlightReservation
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.operator_dashboard.util.TimeFormatter
import kotlinx.coroutines.launch

/**
 * Fragment rendering live operational metrics cards, active booking spotlight, and real-time booking feeds.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var feedAdapter: BookingsFeedAdapter

    private val viewModel: DashboardViewModel by viewModels {
        val database = SsmtsDatabase.getInstance(requireContext().applicationContext)
        val api = ApiClient.createOperatorDashboardApi("https://10.0.2.2:7143/")
        val repository = DashboardRepositoryImpl(api, database.reservationCacheDao())
        DashboardViewModel.Factory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteractions()
        observeViewModel()
    }

    private fun setupInteractions() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.color_primary,
            R.color.color_secondary
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

        feedAdapter = BookingsFeedAdapter()
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
     * Nullifies ViewBinding reference to prevent view-hierarchy memory leaks per Section 3 mandates.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
