/**
 * Description: Unit test suite for TransferSyncNotifier asserting cross-component reactive
 * event broadcast and collection semantics upon energy finalization (FR-M4-07.4).
 */
package com.sliit.ssmts.operator_dashboard.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Validates reactive event emission and delivery through TransferSyncNotifier.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TransferSyncNotifierTest {

    /**
     * Asserts that notifying a completed transfer emits a valid TransferCompletedEvent.
     */
    @Test
    fun notifyTransferCompleted_emitsValidEventToSubscribers() = runTest {
        val testReservationId = "664fa10b9c3e2e1a4f001201"
        val testMeteredKwh = 24.65
        var receivedEvent: TransferCompletedEvent? = null

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            receivedEvent = TransferSyncNotifier.events.first()
        }

        TransferSyncNotifier.notifyTransferCompleted(testReservationId, testMeteredKwh)

        assertNotNull(receivedEvent)
        assertEquals(testReservationId, receivedEvent?.reservationId)
        assertEquals(testMeteredKwh, receivedEvent?.meteredKwh ?: 0.0, 0.001)
        assertTrue((receivedEvent?.timestamp ?: 0L) > 0L)

        collectJob.cancel()
    }

    /**
     * Asserts that multiple sequential transfer completion notifications are broadcast without dropping.
     */
    @Test
    fun notifyTransferCompleted_emitsMultipleEventsSequentially() = runTest {
        val receivedEvents = mutableListOf<TransferCompletedEvent>()

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            TransferSyncNotifier.events.collect { event ->
                receivedEvents.add(event)
            }
        }

        TransferSyncNotifier.notifyTransferCompleted("RES-001", 10.50)
        TransferSyncNotifier.notifyTransferCompleted("RES-002", 45.20)

        assertEquals(2, receivedEvents.size)
        assertEquals("RES-001", receivedEvents[0].reservationId)
        assertEquals(10.50, receivedEvents[0].meteredKwh, 0.001)
        assertEquals("RES-002", receivedEvents[1].reservationId)
        assertEquals(45.20, receivedEvents[1].meteredKwh, 0.001)

        collectJob.cancel()
    }
}
