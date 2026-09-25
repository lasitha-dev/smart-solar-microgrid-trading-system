/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Retrofit interface for the C# Reservations API.
 */

package com.sliit.ssmts.reservation_workflow.data.remote

import com.sliit.ssmts.reservation_workflow.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ReservationApi {

    @GET("api/reservations/slots")
    suspend fun getAvailableSlots(
        @Query("stationId") stationId: String,
        @Query("date") date: String
    ): Response<ApiResponseDto<List<AvailableSlotDto>>>

    @POST("api/reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequestDto
    ): Response<ApiResponseDto<ReservationResponseDto>>

    @GET("api/reservations/{id}")
    suspend fun getReservationById(
        @Path("id") id: String
    ): Response<ApiResponseDto<ReservationResponseDto>>

    @GET("api/reservations")
    suspend fun getProsumerReservations(
        @Query("prosumerId") prosumerId: String? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponseDto<List<ReservationResponseDto>>>

    @PUT("api/reservations/{id}")
    suspend fun updateReservation(
        @Path("id") id: String,
        @Body request: UpdateReservationRequestDto
    ): Response<ApiResponseDto<ReservationResponseDto>>

    @DELETE("api/reservations/{id}")
    suspend fun cancelReservation(
        @Path("id") id: String,
        @Query("reason") reason: String? = null
    ): Response<ApiResponseDto<Any>>

    @PATCH("api/reservations/{id}/approve")
    suspend fun approveReservation(
        @Path("id") id: String,
        @Query("operatorId") operatorId: String
    ): Response<ApiResponseDto<Any>>

    @POST("api/reservations/seed")
    suspend fun seedSlots(): Response<ApiResponseDto<Any>>
}
