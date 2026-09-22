/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Room SQLite database entity representing the active user session and local cache.
 */

package com.sliit.ssmts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sliit.ssmts.domain.model.UserSession

/**
 * SQLite table storing active authentication token and cached prosumer profile.
 * Maintained in 'ssmts_local.db' to satisfy local persistence requirements.
 */
@Entity(tableName = "user_session")
data class SessionEntity(
    @PrimaryKey
    val id: Int = 1,

    val token: String,
    val userId: String,
    val nic: String,
    val username: String,
    val fullName: String,
    val phone: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val role: String,
    val status: String,
    val email: String = "",
    val expiresAt: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Maps this SQLite entity to the clean domain [UserSession] model.
     */
    fun toDomain(): UserSession = UserSession(
        token = token,
        userId = userId,
        nic = nic,
        username = username,
        fullName = fullName,
        phone = phone,
        email = email,
        address = address,
        latitude = latitude,
        longitude = longitude,
        role = role,
        status = status,
        expiresAt = expiresAt
    )
}
