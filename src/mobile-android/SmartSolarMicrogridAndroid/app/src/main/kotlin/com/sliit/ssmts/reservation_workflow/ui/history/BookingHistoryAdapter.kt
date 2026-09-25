/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: RecyclerView Adapter for displaying reservation history items with item click listener.
 */

package com.sliit.ssmts.reservation_workflow.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import com.sliit.ssmts.reservation_workflow.util.DateTimeFormatter

class BookingHistoryAdapter(
    private val onItemClick: (Reservation) -> Unit = {}
) : ListAdapter<Reservation, BookingHistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvBookingId: TextView = view.findViewById(R.id.tvBookingId)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val tvBookingStation: TextView = view.findViewById(R.id.tvBookingStation)
        val tvBookingDateTime: TextView = view.findViewById(R.id.tvBookingDateTime)
        
        fun bind(reservation: Reservation, onItemClick: (Reservation) -> Unit) {
            tvBookingId.text = reservation.id
            tvBookingStation.text = reservation.stationId
            tvBookingDateTime.text = DateTimeFormatter.toDisplayString(reservation.scheduledDateTime)
            
            // Apply status styling
            val context = view.context
            val (bgColor, textColor, label) = when (reservation.status) {
                ReservationStatus.PENDING -> Triple(R.color.status_pending_container, R.color.status_pending_text, context.getString(R.string.status_pending))
                ReservationStatus.APPROVED -> Triple(R.color.status_approved_container, R.color.status_approved_text, context.getString(R.string.status_approved))
                ReservationStatus.CANCELLED -> Triple(R.color.status_cancelled_container, R.color.status_cancelled_text, context.getString(R.string.status_cancelled))
                ReservationStatus.COMPLETED -> Triple(R.color.status_completed_container, R.color.status_completed_text, context.getString(R.string.status_completed))
                else -> Triple(R.color.surface_card, R.color.text_primary, reservation.status.name)
            }
            
            tvStatusBadge.text = label
            tvStatusBadge.setTextColor(ContextCompat.getColor(context, textColor))
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, bgColor))

            view.setOnClickListener {
                onItemClick(reservation)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Reservation>() {
            override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
                return oldItem == newItem
            }
        }
    }
}
