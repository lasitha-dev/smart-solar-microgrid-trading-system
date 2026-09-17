/**
 * Description: ListAdapter implementation utilizing DiffUtil to bind and render historical energy
 * trading reservations with estimated and actual metered kWh comparisons (FR-M4-03).
 */
package com.sliit.ssmts.operator_dashboard.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ItemBookingHistoryBinding
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-performance adapter rendering historical reservation records for the Grid Operator.
 *
 * @property onItemClick Optional callback dispatched when an operator taps a reservation item.
 */
class BookingHistoryAdapter(
    private val onItemClick: ((Reservation) -> Unit)? = null
) : ListAdapter<Reservation, BookingHistoryAdapter.HistoryViewHolder>(ReservationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemBookingHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder managing ViewBinding references and presentation mapping.
     */
    class HistoryViewHolder(
        val binding: ItemBookingHistoryBinding,
        private val onItemClick: ((Reservation) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateTimeFormatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

        /**
         * Binds pure domain reservation fields into the view hierarchy.
         *
         * @param item Historical reservation model.
         */
        fun bind(item: Reservation) {
            val context = binding.root.context

            binding.tvHistoryStationName.text = item.stationName
            binding.tvHistoryRefId.text = context.getString(R.string.label_ref_id, item.id)
            binding.tvHistoryProsumerNic.text = context.getString(R.string.label_prosumer_nic, item.prosumerNic)
            binding.tvHistoryBay.text = context.getString(R.string.label_bay, item.allocatedBay)

            val formattedDateTime = dateTimeFormatter.format(Date(item.scheduledTimeMillis))
            binding.tvHistoryScheduledTime.text = context.getString(R.string.label_scheduled_time, formattedDateTime)

            binding.tvHistoryEstimatedKwh.text = context.getString(R.string.label_estimated_kwh, item.estimatedKwh)

            // FR-M4-03.5: Display both estimated kWh and actual metered kWh on completed transactions
            if (item.isCompleted && item.meteredKwh != null) {
                binding.layoutMeteredKwh.isVisible = true
                binding.tvHistoryMeteredKwh.text = context.getString(R.string.label_metered_kwh, item.meteredKwh)
            } else {
                binding.layoutMeteredKwh.isVisible = false
            }

            binding.badgeHistoryStatus.setStatus(item.status.value)

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    /**
     * DiffUtil callback computing distinct item identity and structural equality.
     */
    object ReservationDiffCallback : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem == newItem
        }
    }
}
