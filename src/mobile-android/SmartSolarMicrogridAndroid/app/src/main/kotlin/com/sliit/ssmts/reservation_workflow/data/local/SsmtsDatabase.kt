/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Room Database singleton for the Android app.
 */

package com.sliit.ssmts.reservation_workflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliit.ssmts.reservation_workflow.data.local.dao.ReservationDao
import com.sliit.ssmts.reservation_workflow.data.local.entity.ReservationEntity

@Database(entities = [ReservationEntity::class], version = 1, exportSchema = false)
abstract class SsmtsDatabase : RoomDatabase() {

    abstract fun reservationDao(): ReservationDao

    companion object {
        @Volatile
        private var INSTANCE: SsmtsDatabase? = null

        fun getDatabase(context: Context): SsmtsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SsmtsDatabase::class.java,
                    "ssmts_local.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
