/**
 * Description: Activity assembling the searchable, filterable reservation history screen with debounced search,
 * 5-state filter chips, empty states, and pull-to-refresh synchronization (FR-M4-03).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.data.local.SsmtsDatabase
import com.sliit.ssmts.operator_dashboard.data.remote.ApiClient
import com.sliit.ssmts.operator_dashboard.data.repository.DashboardRepositoryImpl
import com.sliit.ssmts.databinding.ActivityBookingHistoryBinding
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.ui.common.UiState
import com.sliit.ssmts.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity presenting the full booking history feed with search, status filtering, and live sync.
 */
class BookingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingHistoryBinding
    private lateinit var historyAdapter: BookingHistoryAdapter

    private val viewModel: BookingHistoryViewModel by viewModels {
        val database = SsmtsDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val api = ApiClient.createOperatorDashboardApi(
            baseUrl = "https://10.0.2.2:7143/",
            tokenProvider = { sessionManager.getAuthToken() }
        )
        val repository = DashboardRepositoryImpl(api, database.reservationCacheDao())
        BookingHistoryViewModel.Factory(repository)
    }

    /**
     * Initializes activity layout, toolbar, RecyclerView, search inputs, and state collectors.
     *
     * @param savedInstanceState Saved bundle state if restoring.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearchAndFilters()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarHistory.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = BookingHistoryAdapter()
        binding.rvBookingHistory.apply {
            layoutManager = LinearLayoutManager(this@BookingHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupSearchAndFilters() {
        binding.etSearchQuery.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }

        binding.layoutFilterChips.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipFilterAll
            val filterState = BookingFilterState.fromChipId(checkedId)
            viewModel.onStatusFilterSelected(filterState.filterValue)
        }

        binding.btnHistoryRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.color_primary,
            R.color.color_secondary
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.historyUiState.collect { state ->
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

    private fun renderUiState(state: UiState<List<Reservation>>) {
        when (state) {
            is UiState.Loading -> {
                if (!binding.swipeRefreshLayout.isRefreshing) {
                    binding.progressBarHistory.isVisible = true
                }
                binding.layoutHistoryEmpty.isVisible = false
                binding.layoutHistoryError.isVisible = false
            }
            is UiState.Success -> {
                binding.progressBarHistory.isVisible = false
                binding.layoutHistoryError.isVisible = false

                historyAdapter.submitList(state.data)

                val isEmpty = state.data.isEmpty()
                binding.rvBookingHistory.isVisible = !isEmpty
                binding.layoutHistoryEmpty.isVisible = isEmpty
            }
            is UiState.Error -> {
                binding.progressBarHistory.isVisible = false
                binding.rvBookingHistory.isVisible = false
                binding.layoutHistoryEmpty.isVisible = false
                binding.layoutHistoryError.isVisible = true
                binding.tvHistoryErrorMessage.text = state.message
            }
            is UiState.Idle -> {
                binding.progressBarHistory.isVisible = false
            }
        }
    }
}
