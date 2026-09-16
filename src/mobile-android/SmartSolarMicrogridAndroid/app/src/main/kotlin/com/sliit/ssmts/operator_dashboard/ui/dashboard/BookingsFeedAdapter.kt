/**
 * Description: ListAdapter implementation utilizing DiffUtil to bind and render real-time operational
 * booking feed items on the dashboard (FR-M4-02).
 */
package com.sliit.ssmts.operator_dashboard.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.ItemDashboardBookingBinding
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter presenting real-time active slots and pending reservations for the grid operator.
 *
 * @property onItemClick Optional callback triggered when an operator selects a booking item.
 */
class BookingsFeedAdapter(
    private val onItemClick: ((Reservation) -> Unit)? = null
) : ListAdapter<Reservation, BookingsFeedAdapter.BookingViewHolder>(ReservationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemDashboardBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder caching ViewBinding elements and executing presentation binding.
     */
    class BookingViewHolder(
        private val binding: ItemDashboardBookingBinding,
        private val onItemClick: ((Reservation) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

        /**
         * Binds domain reservation model fields to the view-hierarchy.
         *
         * @param item Reservation entity to display.
         */
        fun bind(item: Reservation) {
            val context = binding.root.context

            binding.tvFeedStationName.text = item.stationName
            binding.tvFeedProsumerNic.text = context.getString(R.string.label_prosumer_nic, item.prosumerNic)
            binding.tvFeedBay.text = context.getString(R.string.label_bay, item.allocatedBay)

            val formattedTime = timeFormatter.format(Date(item.scheduledTimeMillis))
            binding.tvFeedScheduledTime.text = context.getString(R.string.label_scheduled_time, formattedTime)
            binding.tvFeedEnergy.text = context.getString(R.string.label_estimated_kwh, item.estimatedKwh)

            binding.badgeFeedStatus.setStatus(item.status.value)

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    /**
     * DiffUtil callback computing item differences efficiently for RecyclerView updates.
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
