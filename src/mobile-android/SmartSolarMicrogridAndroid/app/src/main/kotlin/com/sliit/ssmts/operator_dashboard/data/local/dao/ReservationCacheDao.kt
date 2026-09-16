/**
 * Description: Room Data Access Object (DAO) for tbl_reservations_cache providing local queries,
 * offline search, filtering, and metric aggregations for Member 4.
 */
package com.sliit.ssmts.operator_dashboard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object defining database interactions with tbl_reservations_cache.
 */
@Dao
interface ReservationCacheDao {

    /**
     * Observes all cached reservations ordered chronologically by scheduled time.
     *
     * @return Flow emitting list of cached reservation entities.
     */
    @Query("SELECT * FROM tbl_reservations_cache ORDER BY scheduled_time ASC")
    fun getAllReservationsFlow(): Flow<List<ReservationCacheEntity>>

    /**
     * Observes cached reservations filtered by specific status.
     *
     * @param status Status filter ('Pending', 'Approved', 'Completed', 'Cancelled').
     * @return Flow emitting matching cached reservation entities.
     */
    @Query("SELECT * FROM tbl_reservations_cache WHERE UPPER(status) = UPPER(:status) ORDER BY scheduled_time ASC")
    fun getReservationsByStatusFlow(status: String): Flow<List<ReservationCacheEntity>>

    /**
     * Searches and filters cached reservations by keyword (NIC, station, or ID) and optional status.
     *
     * @param status Optional status filter or null to match all statuses.
     * @param query Search query matching prosumer NIC, station name, or reservation ID.
     * @return Flow emitting filtered cached reservation entities.
     */
    @Query(
        """
        SELECT * FROM tbl_reservations_cache 
        WHERE (:status IS NULL OR :status = '' OR UPPER(status) = UPPER(:status))
          AND (:query IS NULL OR :query = '' 
               OR prosumer_nic LIKE '%' || :query || '%' 
               OR station_name LIKE '%' || :query || '%' 
               OR reservation_id LIKE '%' || :query || '%')
        ORDER BY scheduled_time ASC
        """
    )
    fun searchReservationsFlow(status: String?, query: String?): Flow<List<ReservationCacheEntity>>

    /**
     * Retrieves a single cached reservation by its primary identifier.
     *
     * @param id Remote reservation identifier.
     * @return Matching entity or null if not found.
     */
    @Query("SELECT * FROM tbl_reservations_cache WHERE reservation_id = :id LIMIT 1")
    suspend fun getReservationById(id: String): ReservationCacheEntity?

    /**
     * Counts the total number of reservations currently awaiting review.
     *
     * @return Integer count of pending reservations.
     */
    @Query("SELECT COUNT(*) FROM tbl_reservations_cache WHERE UPPER(status) = 'PENDING'")
    suspend fun getPendingCount(): Int

    /**
     * Counts approved reservations scheduled in the future after the specified epoch millis.
     *
     * @param nowMillis Epoch timestamp in milliseconds.
     * @return Integer count of approved future reservations.
     */
    @Query("SELECT COUNT(*) FROM tbl_reservations_cache WHERE UPPER(status) = 'APPROVED' AND scheduled_time > :nowMillis")
    suspend fun getApprovedFutureCount(nowMillis: Long): Int

    /**
     * Counts completed reservations for the calendar day window.
     *
     * @param startOfDayMillis Start of calendar day in epoch millis.
     * @param endOfDayMillis End of calendar day in epoch millis.
     * @return Integer count of completed reservations today.
     */
    @Query(
        """
        SELECT COUNT(*) FROM tbl_reservations_cache 
        WHERE UPPER(status) = 'COMPLETED' 
          AND scheduled_time >= :startOfDayMillis 
          AND scheduled_time < :endOfDayMillis
        """
    )
    suspend fun getCompletedTodayCount(startOfDayMillis: Long, endOfDayMillis: Long): Int

    /**
     * Retrieves the earliest upcoming approved reservation to display on the dashboard spotlight.
     *
     * @param nowMillis Epoch timestamp in milliseconds.
     * @return Nearest approved entity or null if none available.
     */
    @Query("SELECT * FROM tbl_reservations_cache WHERE UPPER(status) = 'APPROVED' AND scheduled_time >= :nowMillis ORDER BY scheduled_time ASC LIMIT 1")
    suspend fun getActiveSpotlight(nowMillis: Long): ReservationCacheEntity?

    /**
     * Inserts or replaces a single reservation entity in the local cache.
     *
     * @param entity Entity to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReservation(entity: ReservationCacheEntity)

    /**
     * Inserts or replaces a collection of reservation entities in the local cache.
     *
     * @param entities List of entities to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReservations(entities: List<ReservationCacheEntity>)

    /**
     * Updates the status and metered kWh when an energy transfer is finalized.
     *
     * @param id Remote reservation identifier.
     * @param status Updated status string ('Completed').
     * @param meteredKwh Delivered energy value in kilowatt-hours.
     * @param lastSyncedAt Timestamp of local update.
     * @return Number of rows updated.
     */
    @Query(
        """
        UPDATE tbl_reservations_cache 
        SET status = :status, metered_kwh = :meteredKwh, last_synced_at = :lastSyncedAt 
        WHERE reservation_id = :id
        """
    )
    suspend fun updateFinalizationStatus(id: String, status: String, meteredKwh: Double, lastSyncedAt: Long): Int

    /**
     * Deletes a single cached reservation by ID.
     *
     * @param id Remote reservation identifier.
     * @return Number of rows removed.
     */
    @Query("DELETE FROM tbl_reservations_cache WHERE reservation_id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Clears all cached reservation records from tbl_reservations_cache.
     */
    @Query("DELETE FROM tbl_reservations_cache")
    suspend fun clearAll()
}
