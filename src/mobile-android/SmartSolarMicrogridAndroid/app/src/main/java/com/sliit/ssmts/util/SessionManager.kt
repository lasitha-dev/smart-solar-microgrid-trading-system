/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Session manager encapsulating local SQLite persistence and active JWT credentials.
 */

package com.sliit.ssmts.util

import android.content.Context
import com.sliit.ssmts.data.local.SsmtsDatabase
import com.sliit.ssmts.data.local.entity.SessionEntity
import com.sliit.ssmts.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Manages active session caching in Room SQLite ('ssmts_local.db').
 */
class SessionManager(context: Context) {

    private val sessionDao = SsmtsDatabase.getInstance(context).sessionDao()

    /**
     * Synchronously retrieves the cached JWT bearer token for the OkHttp header interceptor.
     */
    fun getAuthToken(): String? {
        return runBlocking {
            sessionDao.getActiveSession()?.token
        }
    }

    /**
     * Checks whether an active user session exists in local storage.
     */
    suspend fun isLoggedIn(): Boolean {
        return sessionDao.getActiveSession() != null
    }

    /**
     * Retrieves the current domain [UserSession] from SQLite.
     */
    suspend fun getActiveSession(): UserSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    /**
     * Observes the active session as a reactive coroutine Flow.
     */
    fun observeActiveSession(): Flow<UserSession?> {
        return sessionDao.observeActiveSession().map { it?.toDomain() }
    }

    /**
     * Persists or updates the active session entity in local SQLite.
     */
    suspend fun saveSession(session: UserSession) {
        val entity = SessionEntity(
            token = session.token,
            userId = session.userId,
            nic = session.nic,
            username = session.username,
            fullName = session.fullName,
            phone = session.phone,
            address = session.address,
            latitude = session.latitude,
            longitude = session.longitude,
            role = session.role,
            status = session.status,
            expiresAt = session.expiresAt
        )
        sessionDao.saveSession(entity)
    }

    /**
     * Updates the local cached profile details in SQLite.
     */
    suspend fun updateProfileCache(fullName: String, phone: String, address: String) {
        sessionDao.updateProfileCache(fullName, phone, address)
    }

    /**
     * Updates the local cached account status in SQLite.
     */
    suspend fun updateStatusCache(status: String) {
        sessionDao.updateStatusCache(status)
    }

    /**
     * Clears all session data from local SQLite (e.g. upon user logout).
     */
    suspend fun clearSession() {
        sessionDao.clearSession()
    }
}
