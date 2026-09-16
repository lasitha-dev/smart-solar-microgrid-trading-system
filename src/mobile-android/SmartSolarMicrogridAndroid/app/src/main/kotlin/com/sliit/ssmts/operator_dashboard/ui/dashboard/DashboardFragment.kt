/**
 * Description: Operational Dashboard Fragment rendering live counters, active spotlight card,
 * countdown timers, offline indicator banner, and pull-to-refresh interactions (FR-M4-01).
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
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.repository.DashboardRepositoryImpl
import com.sliit.ssmts.operator_dashboard.databinding.FragmentDashboardBinding
import com.sliit.ssmts.operator_dashboard.domain.model.ActiveSpotlightReservation
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fragment rendering live operational metrics cards and active booking spotlight widget.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

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
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnRetrySync.setOnClickListener {
            viewModel.refresh()
        }

        binding.btnErrorRetry.setOnClickListener {
            viewModel.loadMetrics(forceRefresh = true)
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
            val syncFormatted = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(metrics.lastSyncedAtMillis))
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

            val countdownText = calculateCountdown(spotlight.scheduledTimeMillis)
            binding.tvSpotlightCountdown.text = getString(R.string.spotlight_countdown_prefix, countdownText)
        } else {
            binding.layoutSpotlightContent.isVisible = false
            binding.tvSpotlightEmpty.isVisible = true
        }
    }

    private fun calculateCountdown(targetMillis: Long): String {
        val diffMillis = targetMillis - System.currentTimeMillis()
        if (diffMillis <= 0) {
            return "Now"
        }
        val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis) % 60
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    /**
     * Nullifies ViewBinding reference to prevent view-hierarchy memory leaks per Section 3 mandates.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
