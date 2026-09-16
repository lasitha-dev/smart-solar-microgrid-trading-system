/**
 * Description: Repository implementation for IOperatorVerificationRepository executing remote
 * QR verification, defensive metered energy validation, and local SQLite audit persistence.
 */
package com.sliit.ssmts.operator_dashboard.data.repository

import com.sliit.ssmts.operator_dashboard.data.local.dao.OperatorAuditDao
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity
import com.sliit.ssmts.operator_dashboard.data.remote.OperatorDashboardApi
import com.sliit.ssmts.operator_dashboard.data.remote.dto.FinalizeTransferDto
import com.sliit.ssmts.operator_dashboard.data.remote.dto.QrVerificationRequestDto
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus
import com.sliit.ssmts.operator_dashboard.domain.repository.IOperatorVerificationRepository
import com.sliit.ssmts.operator_dashboard.util.NetworkResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Handles operator verification requests, defensive metered input validation, and local state sync.
 *
 * @property api Retrofit REST API interface.
 * @property reservationDao Room DAO for caching reservation records.
 * @property auditDao Room DAO for operator audit log entries.
 * @property dispatcher Coroutine dispatcher for background IO operations.
 */
class OperatorVerificationRepositoryImpl(
    private val api: OperatorDashboardApi,
    private val reservationDao: ReservationCacheDao,
    private val auditDao: OperatorAuditDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : IOperatorVerificationRepository {

    /**
     * Dispatches the scanned QR code token to the central C# Web API for signature verification.
     *
     * @param qrPayload The raw scanned token string from the device camera.
     * @return NetworkResult containing verified reservation details or rejection reason.
     */
    override suspend fun verifyScannedQr(qrPayload: String): NetworkResult<QrVerificationResult> = withContext(dispatcher) {
        if (qrPayload.isBlank()) {
            return@withContext NetworkResult.Error(
                code = "ERR_EMPTY_QR",
                message = "Scanned QR code payload cannot be empty."
            )
        }

        try {
            val response = api.verifyQr(QrVerificationRequestDto(qrPayload.trim()))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val result = QrVerificationResult(
                    isValid = body.valid,
                    reservationId = body.reservationId,
                    prosumerNic = body.prosumerNic,
                    stationName = body.stationName,
                    allocatedBayId = body.allocatedBayId,
                    status = ReservationStatus.fromString(body.status),
                    message = body.message,
                    errorCode = body.errorCode
                )
                NetworkResult.Success(result)
            } else {
                val errorCode = "HTTP_${response.code()}"
                val message = response.errorBody()?.string()?.ifBlank { null }
                    ?: response.message().ifBlank { "QR verification failed." }
                NetworkResult.Error(code = errorCode, message = message)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    /**
     * Commits the actual delivered power reading to finalize the energy transfer transaction.
     *
     * @param reservationId The verified reservation ID.
     * @param meteredKwh The physical power measured at the solar station hub (0.01 - 999.99 kWh).
     * @param notes Optional operator operational observations.
     * @return NetworkResult confirming transfer finalization and receipt metrics.
     */
    override suspend fun finalizeTransfer(
        reservationId: String,
        meteredKwh: Double,
        notes: String?
    ): NetworkResult<FinalizeTransferResult> = withContext(dispatcher) {
        if (reservationId.isBlank()) {
            return@withContext NetworkResult.Error(
                code = "ERR_INVALID_ID",
                message = "Reservation ID cannot be empty."
            )
        }

        if (meteredKwh.isNaN() || meteredKwh < MIN_METERED_KWH || meteredKwh > MAX_METERED_KWH) {
            return@withContext NetworkResult.Error(
                code = "ERR_INVALID_METERED_KWH",
                message = "Metered energy reading must be a positive decimal between $MIN_METERED_KWH and $MAX_METERED_KWH kWh."
            )
        }

        try {
            val request = FinalizeTransferDto(meteredEnergyKwh = meteredKwh, notes = notes)
            val response = api.finalizeTransfer(reservationId.trim(), request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                reservationDao.updateFinalizationStatus(
                    id = reservationId.trim(),
                    status = ReservationStatus.COMPLETED.name,
                    meteredKwh = meteredKwh,
                    lastSyncedAt = System.currentTimeMillis()
                )

                val audit = OperatorAuditEntity(
                    verificationId = UUID.randomUUID().toString(),
                    reservationId = reservationId.trim(),
                    operatorId = body.finalizedByOperator,
                    meteredKwh = meteredKwh,
                    timestamp = System.currentTimeMillis(),
                    syncStatus = OperatorAuditEntity.STATUS_SYNCED
                )
                auditDao.insertAudit(audit)

                val result = FinalizeTransferResult(
                    isSuccess = body.success,
                    reservationId = body.reservationId,
                    status = ReservationStatus.fromString(body.status),
                    meteredEnergyKwh = body.meteredEnergyKwh,
                    finalizedAtIso = body.finalizedAt,
                    finalizedByOperator = body.finalizedByOperator
                )
                NetworkResult.Success(result)
            } else {
                val errorCode = "HTTP_${response.code()}"
                val message = response.errorBody()?.string()?.ifBlank { null }
                    ?: response.message().ifBlank { "Energy transfer finalization failed." }
                NetworkResult.Error(code = errorCode, message = message)
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    companion object {
        const val MIN_METERED_KWH = 0.01
        const val MAX_METERED_KWH = 999.99
    }
}
