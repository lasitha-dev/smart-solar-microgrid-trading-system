/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Implementation of repository connecting API and Room DB.
 */

package com.sliit.ssmts.reservation_workflow.data.repository

import com.sliit.ssmts.reservation_workflow.data.local.dao.ReservationDao
import com.sliit.ssmts.reservation_workflow.data.local.entity.ReservationEntity
import com.sliit.ssmts.reservation_workflow.data.remote.ReservationApi
import com.sliit.ssmts.reservation_workflow.data.remote.dto.CreateReservationRequestDto
import com.sliit.ssmts.reservation_workflow.data.remote.dto.ReservationResponseDto
import com.sliit.ssmts.reservation_workflow.data.remote.dto.UpdateReservationRequestDto
import com.sliit.ssmts.reservation_workflow.domain.model.EnergySlot
import com.sliit.ssmts.reservation_workflow.domain.model.Reservation
import com.sliit.ssmts.reservation_workflow.domain.model.ReservationStatus
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.util.DateTimeFormatter
import com.sliit.ssmts.reservation_workflow.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class ReservationRepositoryImpl(
    private val api: ReservationApi,
    private val dao: ReservationDao
) : IReservationRepository {

    override suspend fun getAvailableSlots(stationId: String, date: Date): NetworkResult<List<EnergySlot>> {
        return try {
            val dateStr = DateTimeFormatter.toIso8601(date).split("T")[0]
            val response = api.getAvailableSlots(stationId, dateStr)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val slots = response.body()?.data?.map { dto ->
                    EnergySlot(
                        id = dto.id,
                        stationId = dto.stationId,
                        date = dto.slotDate,
                        timeRange = "${dto.startTime} - ${dto.endTime}",
                        batterySlotId = dto.batterySlotId,
                        isAvailable = dto.status == "Open"
                    )
                } ?: emptyList()
                NetworkResult.Success(slots)
            } else {
                NetworkResult.Error("API_ERROR", response.body()?.message ?: "Unknown error fetching slots")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun createReservation(prosumerId: String, stationId: String, slotId: String, scheduledTime: Date): NetworkResult<Reservation> {
        return try {
            val request = CreateReservationRequestDto(
                prosumerId = prosumerId,
                stationId = stationId,
                bookingSlotId = slotId,
                scheduledDateTime = DateTimeFormatter.toIso8601(scheduledTime)
            )
            
            val response = api.createReservation(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()!!.data!!
                val reservation = mapToDomain(dto)
                
                // Cache locally
                dao.upsert(mapToEntity(dto))
                
                NetworkResult.Success(reservation)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), response.body()?.message, "Validation failed or slot unavailable")
                NetworkResult.Error(response.code().toString(), errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun getReservation(id: String): NetworkResult<Reservation> {
        return try {
            val response = api.getReservationById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()!!.data!!
                dao.upsert(mapToEntity(dto))
                NetworkResult.Success(mapToDomain(dto))
            } else {
                // Fallback to cache if API fails (e.g. offline)
                val cached = dao.getById(id)
                if (cached != null) {
                    NetworkResult.Success(mapEntityToDomain(cached))
                } else {
                    NetworkResult.Error("NOT_FOUND", "Reservation not found locally or remotely")
                }
            }
        } catch (e: Exception) {
            // Fallback to cache on network exception
            val cached = dao.getById(id)
            if (cached != null) {
                NetworkResult.Success(mapEntityToDomain(cached))
            } else {
                NetworkResult.Exception(e)
            }
        }
    }

    override suspend fun updateReservation(id: String, newSlotId: String, newTime: Date): NetworkResult<Reservation> {
        return try {
            val request = UpdateReservationRequestDto(
                bookingSlotId = newSlotId,
                scheduledDateTime = DateTimeFormatter.toIso8601(newTime)
            )
            val response = api.updateReservation(id, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()!!.data!!
                dao.upsert(mapToEntity(dto))
                NetworkResult.Success(mapToDomain(dto))
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), response.body()?.message, "Update blocked by business rule")
                NetworkResult.Error(response.code().toString(), errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    override suspend fun cancelReservation(id: String, reason: String?): NetworkResult<Unit> {
        return try {
            val response = api.cancelReservation(id, reason)
            
            if (response.isSuccessful) {
                dao.updateStatus(id, "Cancelled")
                NetworkResult.Success(Unit)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), response.body()?.message, "Cancellation blocked by 12-hour rule")
                NetworkResult.Error(response.code().toString(), errorMsg)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    private fun extractErrorMessage(errorBody: String?, bodyMessage: String?, defaultMsg: String): String {
        if (!errorBody.isNullOrBlank()) {
            try {
                val jsonObject = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull) {
                    return jsonObject.get("message").asString
                }
            } catch (_: Exception) {}
        }
        return bodyMessage ?: defaultMsg
    }

    override fun getMyReservations(prosumerId: String): Flow<List<Reservation>> {
        val flow = if (prosumerId.isBlank()) dao.getAll() else dao.getAllByProsumer(prosumerId)
        return flow.map { entities ->
            entities.map { mapEntityToDomain(it) }
        }
    }

    override suspend fun syncReservations(prosumerId: String): NetworkResult<List<Reservation>> {
        return try {
            val queryId = if (prosumerId.isBlank()) null else prosumerId
            val response = api.getProsumerReservations(queryId)
            if (response.isSuccessful && response.body()?.success == true) {
                val dtos = response.body()?.data ?: emptyList()
                val entities = dtos.map { mapToEntity(it) }
                dao.upsertAll(entities)
                NetworkResult.Success(dtos.map { mapToDomain(it) })
            } else {
                NetworkResult.Error(response.code().toString(), response.body()?.message ?: "Failed to sync reservations")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    // --- Mappers ---

    private fun mapToDomain(dto: ReservationResponseDto): Reservation {
        return Reservation(
            id = dto.id,
            prosumerId = dto.prosumerId,
            stationId = dto.stationId,
            bookingSlotId = dto.bookingSlotId,
            scheduledDateTime = DateTimeFormatter.fromIso8601(dto.scheduledDateTime) ?: Date(),
            status = ReservationStatus.fromString(dto.status),
            qrCode = dto.qrCode
        )
    }

    private fun mapToEntity(dto: ReservationResponseDto): ReservationEntity {
        return ReservationEntity(
            reservationId = dto.id,
            prosumerId = dto.prosumerId,
            stationId = dto.stationId,
            bookingSlotId = dto.bookingSlotId,
            scheduledDateTime = dto.scheduledDateTime,
            status = dto.status,
            qrCode = dto.qrCode
        )
    }

    private fun mapEntityToDomain(entity: ReservationEntity): Reservation {
        return Reservation(
            id = entity.reservationId,
            prosumerId = entity.prosumerId,
            stationId = entity.stationId,
            bookingSlotId = entity.bookingSlotId,
            scheduledDateTime = DateTimeFormatter.fromIso8601(entity.scheduledDateTime) ?: Date(),
            status = ReservationStatus.fromString(entity.status),
            qrCode = entity.qrCode
        )
    }
}
