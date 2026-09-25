/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Domain repository interface for managing reservations.
 */

package com.sliit.ssmts.reservation_workflow.domain.repository

import com.sliit.ssmts.reservation_workflow.domain.model.EnergySlot
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface IReservationRepository {
    suspend fun getAvailableSlots(stationId: String, date: Date): NetworkResult<List<EnergySlot>>
    suspend fun createReservation(prosumerId: String, stationId: String, slotId: String, scheduledTime: Date): NetworkResult<Reservation>
    suspend fun getReservation(id: String): NetworkResult<Reservation>
    suspend fun updateReservation(id: String, newSlotId: String, newTime: Date): NetworkResult<Reservation>
    suspend fun cancelReservation(id: String, reason: String?): NetworkResult<Unit>
    fun getMyReservations(prosumerId: String): Flow<List<Reservation>>
    suspend fun syncReservations(prosumerId: String): NetworkResult<List<Reservation>>
}
