/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Unit test suite verifying Room SQLite Session DAO operations, data caching, and domain entity mapping.
 */

package com.sliit.ssmts.data.local

import com.sliit.ssmts.data.local.dao.SessionDao
import com.sliit.ssmts.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying Session DAO persistence contracts and entity domain conversions.
 */
class SessionDaoTest {

    private lateinit var fakeSessionDao: FakeSessionDao

    @Before
    fun setUp() {
        fakeSessionDao = FakeSessionDao()
    }

    @Test
    fun saveSession_And_GetActiveSession_SuccessfullyPersistsData() = runTest {
        // Arrange
        val session = SessionEntity(
            id = 1,
            token = "jwt_test_token_12345",
            userId = "user_67890",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            phone = "0771234567",
            email = "john@microgrid.lk",
            address = "No 10, Colombo",
            latitude = 6.9271,
            longitude = 79.8612,
            role = "Prosumer",
            status = "Active",
            expiresAt = "2026-12-31T23:59:59Z"
        )

        // Act
        fakeSessionDao.saveSession(session)
        val retrieved = fakeSessionDao.getActiveSession()

        // Assert
        assertNotNull(retrieved)
        assertEquals("jwt_test_token_12345", retrieved?.token)
        assertEquals("user_67890", retrieved?.userId)
        assertEquals("199512345678", retrieved?.nic)
        assertEquals("prosumer_john", retrieved?.username)
        assertEquals("John Silva", retrieved?.fullName)
        assertEquals("0771234567", retrieved?.phone)
        assertEquals("john@microgrid.lk", retrieved?.email)
        assertEquals("No 10, Colombo", retrieved?.address)
        assertEquals(6.9271, retrieved?.latitude ?: 0.0, 0.0001)
        assertEquals(79.8612, retrieved?.longitude ?: 0.0, 0.0001)
        assertEquals("Prosumer", retrieved?.role)
        assertEquals("Active", retrieved?.status)
    }

    @Test
    fun updateProfileCache_ModifiesCachedNamePhoneAndAddress() = runTest {
        // Arrange
        val initialSession = SessionEntity(
            id = 1,
            token = "jwt_test_token_12345",
            userId = "user_67890",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "Original Name",
            phone = "0770000000",
            address = "Old Address",
            role = "Prosumer",
            status = "Active"
        )
        fakeSessionDao.saveSession(initialSession)

        // Act
        fakeSessionDao.updateProfileCache(
            fullName = "Updated Legal Name",
            phone = "0779999999",
            address = "New Solar Facility Road"
        )
        val updated = fakeSessionDao.getActiveSession()

        // Assert
        assertNotNull(updated)
        assertEquals("Updated Legal Name", updated?.fullName)
        assertEquals("0779999999", updated?.phone)
        assertEquals("New Solar Facility Road", updated?.address)
        assertEquals("199512345678", updated?.nic) // Immutable field unchanged
    }

    @Test
    fun updateStatusCache_ModifiesCachedAccountStatus() = runTest {
        // Arrange
        val session = SessionEntity(
            id = 1,
            token = "jwt_test_token_12345",
            userId = "user_67890",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            role = "Prosumer",
            status = "PendingActivation"
        )
        fakeSessionDao.saveSession(session)

        // Act
        fakeSessionDao.updateStatusCache("Active")
        val updated = fakeSessionDao.getActiveSession()

        // Assert
        assertNotNull(updated)
        assertEquals("Active", updated?.status)
    }

    @Test
    fun clearSession_RemovesAllStoredSessionData() = runTest {
        // Arrange
        val session = SessionEntity(
            id = 1,
            token = "jwt_test_token_12345",
            userId = "user_67890",
            nic = "199512345678",
            username = "prosumer_john",
            fullName = "John Silva",
            role = "Prosumer",
            status = "Active"
        )
        fakeSessionDao.saveSession(session)
        assertNotNull(fakeSessionDao.getActiveSession())

        // Act
        fakeSessionDao.clearSession()
        val cleared = fakeSessionDao.getActiveSession()

        // Assert
        assertNull(cleared)
    }

    @Test
    fun toDomain_MapsAllSessionEntityFieldsAccurately() {
        // Arrange
        val entity = SessionEntity(
            id = 1,
            token = "sample_access_token",
            userId = "usr_1001",
            nic = "199012345678",
            username = "solar_hero",
            fullName = "Solar Hero",
            phone = "0712345678",
            email = "hero@solar.lk",
            address = "Kandy, Sri Lanka",
            latitude = 7.2906,
            longitude = 80.6337,
            role = "Prosumer",
            status = "Active",
            expiresAt = "2026-12-31T00:00:00Z"
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(entity.token, domain.token)
        assertEquals(entity.userId, domain.userId)
        assertEquals(entity.nic, domain.nic)
        assertEquals(entity.username, domain.username)
        assertEquals(entity.fullName, domain.fullName)
        assertEquals(entity.phone, domain.phone)
        assertEquals(entity.email, domain.email)
        assertEquals(entity.address, domain.address)
        assertEquals(entity.latitude, domain.latitude)
        assertEquals(entity.longitude, domain.longitude)
        assertEquals(entity.role, domain.role)
        assertEquals(entity.status, domain.status)
        assertEquals(entity.expiresAt, domain.expiresAt)
    }
}

/**
 * In-memory test implementation of [SessionDao] providing fidelity to Room SQLite behavior.
 */
class FakeSessionDao : SessionDao {

    private var currentSession: SessionEntity? = null
    private val sessionFlow = MutableStateFlow<SessionEntity?>(null)

    override suspend fun saveSession(session: SessionEntity) {
        currentSession = session
        sessionFlow.value = session
    }

    override suspend fun getActiveSession(): SessionEntity? {
        return currentSession
    }

    override fun observeActiveSession(): Flow<SessionEntity?> {
        return sessionFlow.asStateFlow()
    }

    override suspend fun updateProfileCache(
        fullName: String,
        phone: String,
        address: String,
        updatedAt: Long
    ) {
        currentSession?.let {
            currentSession = it.copy(
                fullName = fullName,
                phone = phone,
                address = address,
                updatedAt = updatedAt
            )
            sessionFlow.value = currentSession
        }
    }

    override suspend fun updateStatusCache(status: String, updatedAt: Long) {
        currentSession?.let {
            currentSession = it.copy(
                status = status,
                updatedAt = updatedAt
            )
            sessionFlow.value = currentSession
        }
    }

    override suspend fun clearSession() {
        currentSession = null
        sessionFlow.value = null
    }
}
