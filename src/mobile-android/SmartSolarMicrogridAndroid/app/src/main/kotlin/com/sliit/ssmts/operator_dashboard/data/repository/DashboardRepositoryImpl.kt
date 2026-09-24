/**
 * Description: Repository implementation for IDashboardRepository orchestrating central REST API queries,
 * offline SQLite cache fallback, and background synchronization streams for Member 4.
 */
package com.sliit.ssmts.operator_dashboard.data.repository

import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.domain.model.ActiveSpotlightReservation
import com.sliit.ssmts.operator_dashboard.domain.model.DashboardMetrics
import com.sliit.ssmts.operator_dashboard.domain.model.Reservation
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IDashboardRepository
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Coordinates central REST API operations and local SQLite database cache for the dashboard subsystem.
 *
 * @property api Retrofit REST client for Member 4 endpoints.
 * @property dao Room DAO for interacting with tbl_reservations_cache.
 * @property dispatcher Coroutine dispatcher for background IO operations.
 */
class DashboardRepositoryImpl(
    private val api: OperatorDashboardApi,
    private val dao: ReservationCacheDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IDashboardRepository {

    /**
     * Emits operational metrics, optionally enforcing a remote synchronization over the network.
     *
     * @param forceRefresh Set to true to bypass cache and immediately trigger a central API sync.
     * @return Flow emitting NetworkResult containing DashboardMetrics.
     */
    override fun getDashboardMetricsStream(forceRefresh: Boolean): Flow<NetworkResult<DashboardMetrics>> = flow {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMillis = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDayMillis = calendar.timeInMillis

        suspend fun computeOfflineMetrics(): DashboardMetrics {
            val pending = dao.getPendingCount()
            val approvedFuture = dao.getApprovedFutureCount(now)
            val completedToday = dao.getCompletedTodayCount(startOfDayMillis, endOfDayMillis)
            val spotlightEntity = dao.getActiveSpotlight(now)

            val activeSpotlight = spotlightEntity?.let { entity ->
                ActiveSpotlightReservation(
                    reservationId = entity.reservationId,
                    stationName = entity.stationName,
                    allocatedBayId = entity.allocatedBay,
                    scheduledDateTimeIso = formatMillisToIso(entity.scheduledTime),
                    scheduledTimeMillis = entity.scheduledTime,
                    status = ReservationStatus.fromString(entity.status),
                    estimatedKwh = entity.estimatedKwh
                )
            }

            return DashboardMetrics(
                pendingReservationsCount = pending,
                approvedFutureReservationsCount = approvedFuture,
                completedTodayCount = completedToday,
                activeSpotlight = activeSpotlight,
                isOfflineCached = true,
                lastSyncedAtMillis = now
            )
        }

        if (!forceRefresh) {
            emit(NetworkResult.Success(computeOfflineMetrics()))
        }

        try {
            val response = api.getDashboardMetrics()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val spotlight = dto.activeSpotlight?.let { s ->
                    ActiveSpotlightReservation(
                        reservationId = s.reservationId,
                        stationName = s.stationName,
                        allocatedBayId = s.allocatedBayId,
                        scheduledDateTimeIso = s.scheduledDateTime,
                        scheduledTimeMillis = parseIsoToMillis(s.scheduledDateTime),
                        status = ReservationStatus.fromString(s.status),
                        estimatedKwh = s.estimatedKwh
                    )
                }
                val liveMetrics = DashboardMetrics(
                    pendingReservationsCount = dto.pendingReservationsCount,
                    approvedFutureReservationsCount = dto.approvedFutureReservationsCount,
                    completedTodayCount = dto.completedTodayCount,
                    activeSpotlight = spotlight,
                    isOfflineCached = false,
                    lastSyncedAtMillis = System.currentTimeMillis()
                )
                emit(NetworkResult.Success(liveMetrics))
            } else {
                emit(NetworkResult.Success(computeOfflineMetrics()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Success(computeOfflineMetrics()))
        }
    }.flowOn(dispatcher)

    /**
     * Observes local reservation cache records matching optional status and keyword criteria.
     *
     * @param status Status filter chip string ('All', 'Pending', 'Approved', 'Completed', 'Cancelled').
     * @param search Debounced keyword query matching station name or prosumer NIC.
     * @return Flow emitting matching Reservation domain models.
     */
    override fun getCachedReservationsStream(status: String?, search: String?): Flow<List<Reservation>> {
        val normalizedStatus = if (status.isNullOrBlank() || status.equals("All", ignoreCase = true)) null else status.trim()
        val normalizedSearch = if (search.isNullOrBlank()) null else search.trim()

        return dao.searchReservationsFlow(normalizedStatus, normalizedSearch)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    /**
     * Observes active reservations scheduled for the current calendar date (FR-M4-02.1).
     *
     * @return Flow emitting active reservation domain models.
     */
    override fun getTodayActiveReservationsStream(): Flow<List<Reservation>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMillis = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDayMillis = calendar.timeInMillis

        return dao.getTodayActiveReservationsFlow(startOfDayMillis, endOfDayMillis)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    /**
     * Observes pending reservations awaiting operator or administrative validation (FR-M4-02.2).
     *
     * @return Flow emitting pending reservation domain models.
     */
    override fun getPendingQueueReservationsStream(): Flow<List<Reservation>> {
        return dao.getReservationsByStatusFlow("PENDING")
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatcher)
    }

    /**
     * Synchronizes local SQLite reservation cache with the central C# Web API.
     *
     * @return NetworkResult indicating synchronization success or failure.
     */
    override suspend fun syncRemoteReservations(): NetworkResult<Unit> = withContext(dispatcher) {
        try {
            val response = api.getReservations()
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                val entities = dtoList.map { dto ->
                    ReservationCacheEntity(
                        reservationId = dto.reservationId,
                        prosumerNic = dto.prosumerNic,
                        stationName = dto.stationName,
                        scheduledTime = parseIsoToMillis(dto.scheduledDateTime),
                        allocatedBay = dto.allocatedBayId,
                        status = dto.status,
                        qrPayload = dto.qrCode,
                        estimatedKwh = dto.estimatedKwh,
                        meteredKwh = dto.meteredEnergyKwh,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                }
                dao.upsertReservations(entities)
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(
                    code = "HTTP_${response.code()}",
                    message = response.message().ifBlank { "Failed to synchronize remote reservations." }
                )
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    private fun parseIsoToMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format.parse(iso)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun formatMillisToIso(millis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return format.format(Date(millis))
    }
}
