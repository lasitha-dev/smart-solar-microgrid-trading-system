/**
 * Description: Inverted repository interface defining operational metrics queries and local cache synchronization streams.
 */
package com.sliit.ssmts.operator_dashboard.domain.repository

import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Description: Abstraction consumed by ViewModels to access operational metrics and cached booking feeds.
 */
interface IDashboardRepository {

    /**
     * Emits operational metrics, optionally enforcing a remote synchronization over the network.
     *
     * @param forceRefresh Set to true to bypass cache and immediately trigger a central API sync.
     * @param operatorId Optional Grid Operator user ID to filter station-specific operational metrics.
     * @return Flow emitting NetworkResult containing DashboardMetrics.
     */
    fun getDashboardMetricsStream(forceRefresh: Boolean = false, operatorId: String? = null): Flow<NetworkResult<DashboardMetrics>>

    /**
     * Observes local reservation cache records matching optional status and keyword criteria.
     *
     * @param status Status filter chip string ('All', 'Pending', 'Approved', 'Completed', 'Cancelled').
     * @param search Debounced keyword query matching station name or prosumer NIC.
     * @return Flow emitting matching Reservation domain models.
     */
    fun getCachedReservationsStream(status: String? = null, search: String? = null): Flow<List<Reservation>>

    /**
     * Observes active reservations scheduled for the current calendar date (FR-M4-02.1).
     *
     * @return Flow emitting active reservation domain models.
     */
    fun getTodayActiveReservationsStream(): Flow<List<Reservation>>

    /**
     * Observes pending reservations awaiting operator or administrative validation (FR-M4-02.2).
     *
     * @return Flow emitting pending reservation domain models.
     */
    fun getPendingQueueReservationsStream(): Flow<List<Reservation>>

    /**
     * Synchronizes local SQLite reservation cache with the central C# Web API.
     *
     * @param operatorId Optional Grid Operator identifier to restrict sync to assigned station.
     * @return NetworkResult indicating synchronization success or failure.
     */
    suspend fun syncRemoteReservations(operatorId: String? = null): NetworkResult<Unit>

    /**
     * Approves a pending reservation via the central API and updates the local cache.
     *
     * @param reservationId Identifier of the reservation being approved.
     * @param operatorId Optional Grid Operator identifier performing the approval.
     * @return NetworkResult containing the approved Reservation domain model.
     */
    suspend fun approveReservation(reservationId: String, operatorId: String? = null): NetworkResult<Reservation>
    suspend fun rejectReservation(reservationId: String, reason: String? = null, operatorId: String? = null): NetworkResult<Reservation>
}
