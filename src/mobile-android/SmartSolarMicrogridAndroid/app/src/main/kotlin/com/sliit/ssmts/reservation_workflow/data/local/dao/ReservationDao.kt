/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Room DAO for accessing cached reservations.
 */

package com.sliit.ssmts.reservation_workflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sliit.ssmts.reservation_workflow.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Query("SELECT * FROM tbl_reservations_cache WHERE prosumerId = :prosumerId ORDER BY scheduledDateTime DESC")
    fun getAllByProsumer(prosumerId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM tbl_reservations_cache ORDER BY scheduledDateTime DESC")
    fun getAll(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM tbl_reservations_cache WHERE reservationId = :id")
    suspend fun getById(id: String): ReservationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reservation: ReservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reservations: List<ReservationEntity>)

    @Query("UPDATE tbl_reservations_cache SET status = :status WHERE reservationId = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM tbl_reservations_cache")
    suspend fun deleteAll()
}
