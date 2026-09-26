/**
 * Description: Retrofit REST API contract defining the central endpoints owned and consumed
 * by Member 4 (Operator Verification & Operational Dashboard).
 */
package com.sliit.ssmts.operator_dashboard.data.remote

import com.sliit.ssmts.operator_dashboard.data.remote.dto.DashboardMetricsDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.FinalizeTransferDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.FinalizeTransferResponseDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.QrVerificationRequestDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.QrVerificationResponseDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.ReservationItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface exposing Member 4 dashboard and operator verification endpoints.
 */
interface OperatorDashboardApi {

    /**
     * Retrieves aggregated live counts and active booking spotlight details.
     *
     * @param operatorId Optional Grid Operator ID to filter metrics specifically for their assigned station.
     * @return Retrofit Response containing DashboardMetricsDto.
     */
    @GET("api/reservations/dashboard-metrics")
    suspend fun getDashboardMetrics(
        @Query("operatorId") operatorId: String? = null
    ): Response<DashboardMetricsDto>

    /**
     * Queries reservations with optional status, search keyword, date, and operatorId filters.
     *
     * @param status Optional state filter ('Pending', 'Approved', 'Completed', 'Cancelled').
     * @param search Case-insensitive search string matching NIC or station name.
     * @param date Optional ISO date string (YYYY-MM-DD) for slot filtering.
     * @param operatorId Optional Grid Operator identifier to restrict listings to assigned station.
     * @return Retrofit Response containing list of matching ReservationItemDto records.
     */
    @GET("api/reservations")
    suspend fun getReservations(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("date") date: String? = null,
        @Query("operatorId") operatorId: String? = null
    ): Response<List<ReservationItemDto>>

    /**
     * Approves a pending reservation, allocating slot/bay and generating cryptographic check-in QR code.
     *
     * @param reservationId Remote reservation identifier to approve.
     * @param operatorId Optional Grid Operator identifier executing the approval.
     * @return Retrofit Response containing the approved ReservationItemDto.
     */
    @PATCH("api/reservations/{id}/approve")
    suspend fun approveReservation(
        @Path("id") reservationId: String,
        @Query("operatorId") operatorId: String? = null
    ): Response<ReservationItemDto>

    /**
     * Rejects a pending reservation, cancelling it and releasing the slot and bay.
     *
     * @param reservationId Remote reservation identifier to reject.
     * @param reason Optional explanation for the rejection.
     * @param operatorId Optional Grid Operator identifier executing the rejection.
     * @return Retrofit Response containing the rejected ReservationItemDto.
     */
    @PATCH("api/reservations/{id}/reject")
    suspend fun rejectReservation(
        @Path("id") reservationId: String,
        @Query("reason") reason: String? = null,
        @Query("operatorId") operatorId: String? = null
    ): Response<ReservationItemDto>

    /**
     * Submits a scanned QR payload to the central API for cryptographic signature validation.
     *
     * @param request Payload containing raw scanned QR token.
     * @return Retrofit Response containing QrVerificationResponseDto.
     */
    @POST("api/reservations/verify-qr")
    suspend fun verifyQr(
        @Body request: QrVerificationRequestDto
    ): Response<QrVerificationResponseDto>

    /**
     * Finalizes energy transfer, committing actual metered kWh and transitioning status to Completed.
     *
     * @param reservationId Remote reservation identifier to finalize.
     * @param request Payload containing actual metered kWh and optional notes.
     * @return Retrofit Response containing FinalizeTransferResponseDto.
     */
    @PATCH("api/reservations/{id}/finalize")
    suspend fun finalizeTransfer(
        @Path("id") reservationId: String,
        @Body request: FinalizeTransferDto
    ): Response<FinalizeTransferResponseDto>
}
