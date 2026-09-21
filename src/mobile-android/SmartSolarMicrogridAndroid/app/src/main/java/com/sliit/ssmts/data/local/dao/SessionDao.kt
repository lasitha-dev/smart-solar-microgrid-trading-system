/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Room Data Access Object (DAO) for querying and persisting local SQLite user sessions.
 */

package com.sliit.ssmts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sliit.ssmts.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for the 'user_session' table.
 */
@Dao
interface SessionDao {

    /**
     * Inserts or replaces the active session record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    /**
     * Retrieves the current active session record synchronously/suspend.
     */
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    /**
     * Observes the active session as a reactive coroutine Flow.
     */
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    /**
     * Updates permitted profile fields in the local SQLite cache.
     */
    @Query("UPDATE user_session SET fullName = :fullName, phone = :phone, address = :address, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateProfileCache(fullName: String, phone: String, address: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Updates account lifecycle status in the local SQLite cache.
     */
    @Query("UPDATE user_session SET status = :status, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateStatusCache(status: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Deletes all rows from the user_session table upon logout.
     */
    @Query("DELETE FROM user_session")
    suspend fun clearSession()
}
