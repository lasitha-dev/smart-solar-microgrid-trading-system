/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for ReservationDao using in-memory Room database on Robolectric.
 */

package com.sliit.ssmts.reservation_workflow.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sliit.ssmts.reservation_workflow.data.local.dao.ReservationDao
import com.sliit.ssmts.reservation_workflow.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReservationDaoTest {

    private lateinit var database: SsmtsDatabase
    private lateinit var dao: ReservationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SsmtsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.reservationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `upsert inserts new reservation and getById retrieves it`() = runTest {
        val entity = ReservationEntity(
            reservationId = "RES-001",
            prosumerId = "200112345678",
            stationId = "60d5ec49f1b2c42d8c3b4a59",
            bookingSlotId = "slot-1",
            scheduledDateTime = "2026-09-25T10:00:00.000Z",
            status = "Pending",
            qrCode = null
        )

        dao.upsert(entity)

        val retrieved = dao.getById("RES-001")
        assertNotNull(retrieved)
        assertEquals("RES-001", retrieved?.reservationId)
        assertEquals("Pending", retrieved?.status)
    }

    @Test
    fun `upsert updates existing reservation when id matches`() = runTest {
        val initial = ReservationEntity(
            reservationId = "RES-002",
            prosumerId = "200112345678",
            stationId = "60d5ec49f1b2c42d8c3b4a59",
            bookingSlotId = "slot-1",
            scheduledDateTime = "2026-09-25T10:00:00.000Z",
            status = "Pending",
            qrCode = null
        )
        dao.upsert(initial)

        val updated = initial.copy(status = "Approved", qrCode = "QR-RES-002")
        dao.upsert(updated)

        val retrieved = dao.getById("RES-002")
        assertEquals("Approved", retrieved?.status)
        assertEquals("QR-RES-002", retrieved?.qrCode)
    }

    @Test
    fun `updateStatus changes status field`() = runTest {
        val entity = ReservationEntity(
            reservationId = "RES-003",
            prosumerId = "200112345678",
            stationId = "60d5ec49f1b2c42d8c3b4a59",
            bookingSlotId = "slot-2",
            scheduledDateTime = "2026-09-25T10:00:00.000Z",
            status = "Pending",
            qrCode = null
        )
        dao.upsert(entity)

        dao.updateStatus("RES-003", "Cancelled")

        val retrieved = dao.getById("RES-003")
        assertEquals("Cancelled", retrieved?.status)
    }

    @Test
    fun `getAllByProsumer returns correct entities ordered by scheduledDateTime`() = runTest {
        val entity1 = ReservationEntity(
            reservationId = "RES-010",
            prosumerId = "200112345678",
            stationId = "S1",
            bookingSlotId = "B1",
            scheduledDateTime = "2026-09-25T10:00:00.000Z",
            status = "Pending",
            qrCode = null
        )
        val entity2 = ReservationEntity(
            reservationId = "RES-011",
            prosumerId = "200112345678",
            stationId = "S1",
            bookingSlotId = "B2",
            scheduledDateTime = "2026-09-26T10:00:00.000Z",
            status = "Approved",
            qrCode = null
        )
        val otherUserEntity = ReservationEntity(
            reservationId = "RES-012",
            prosumerId = "999999999999",
            stationId = "S1",
            bookingSlotId = "B3",
            scheduledDateTime = "2026-09-27T10:00:00.000Z",
            status = "Pending",
            qrCode = null
        )

        dao.upsertAll(listOf(entity1, entity2, otherUserEntity))

        val prosumerList = dao.getAllByProsumer("200112345678").first()
        assertEquals(2, prosumerList.size)
        // Ordered DESC by scheduledDateTime -> 2026-09-26 comes first
        assertEquals("RES-011", prosumerList[0].reservationId)
        assertEquals("RES-010", prosumerList[1].reservationId)
    }
}
