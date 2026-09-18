/**
 * Description: Reactive event coordinator broadcasting energy transfer completion events
 * across activities and fragments to trigger SQLite cache updates and dashboard counter recalculations (FR-M4-07.4).
 */
package com.sliit.ssmts.operator_dashboard.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event payload representing a completed energy transfer.
 *
 * @property reservationId The unique identifier of the finalized reservation.
 * @property meteredKwh The final metered energy transferred in kWh.
 * @property timestamp Epoch millisecond timestamp of finalization.
 */
data class TransferCompletedEvent(
    val reservationId: String,
    val meteredKwh: Double,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Reactive event notifier for communicating transfer completion events
 * to refresh local cache views and operational dashboard metrics.
 */
object TransferSyncNotifier {

    private val _events = MutableSharedFlow<TransferCompletedEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Shared observable stream of transfer completion events.
     */
    val events: SharedFlow<TransferCompletedEvent> = _events.asSharedFlow()

    /**
     * Broadcasts a transfer completion notification.
     *
     * @param reservationId The unique identifier of the finalized reservation.
     * @param meteredKwh The final metered energy transferred in kWh.
     */
    fun notifyTransferCompleted(reservationId: String, meteredKwh: Double) {
        _events.tryEmit(TransferCompletedEvent(reservationId, meteredKwh))
    }
}
