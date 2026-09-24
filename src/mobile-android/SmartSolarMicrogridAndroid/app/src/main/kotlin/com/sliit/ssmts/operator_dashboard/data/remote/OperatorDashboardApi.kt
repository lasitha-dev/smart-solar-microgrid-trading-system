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
     * @return Retrofit Response containing DashboardMetricsDto.
     */
    @GET("api/reservations/dashboard-metrics")
    suspend fun getDashboardMetrics(): Response<DashboardMetricsDto>

    /**
     * Queries reservations with optional status, search keyword, and date filters.
     *
     * @param status Optional state filter ('Pending', 'Approved', 'Completed', 'Cancelled').
     * @param search Case-insensitive search string matching NIC or station name.
     * @param date Optional ISO date string (YYYY-MM-DD) for slot filtering.
     * @return Retrofit Response containing list of matching ReservationItemDto records.
     */
    @GET("api/reservations")
    suspend fun getReservations(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("date") date: String? = null
    ): Response<List<ReservationItemDto>>

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
