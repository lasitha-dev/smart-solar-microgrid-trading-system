/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Generic wrapper for API responses matching C# backend.
 */

package com.sliit.ssmts.reservation_workflow.data.remote.dto

data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val code: String?,
    val errors: List<String>?
)
