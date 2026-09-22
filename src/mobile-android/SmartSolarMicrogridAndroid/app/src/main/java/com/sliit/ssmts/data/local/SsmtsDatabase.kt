/*
 * Student Name: SILVA M N U (IT22169112) & A.L.M Athulathmudali (IT21129544)
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1) & Operator Verification (Member 4)
 * Description: Room database implementation managing the local SQLite storage 'ssmts_local.db'.
 */

package com.sliit.ssmts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliit.ssmts.data.local.dao.SessionDao
import com.sliit.ssmts.data.local.entity.SessionEntity
import com.sliit.ssmts.operator_dashboard.data.local.dao.OperatorAuditDao
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity

/**
 * Main Room SQLite database for local persistence across offline intervals and session restarts.
 */
@Database(
    entities = [
        SessionEntity::class,
        ReservationCacheEntity::class,
        OperatorAuditEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class SsmtsDatabase : RoomDatabase() {

    /**
     * Provides access to the SessionDao operations.
     */
    abstract fun sessionDao(): SessionDao

    /**
     * Provides access to reservation cache database operations.
     */
    abstract fun reservationCacheDao(): ReservationCacheDao

    /**
     * Provides access to operator audit database operations.
     */
    abstract fun operatorAuditDao(): OperatorAuditDao

    companion object {
        private const val DATABASE_NAME = "ssmts_local.db"

        @Volatile
        private var instance: SsmtsDatabase? = null

        /**
         * Returns the singleton instance of the [SsmtsDatabase].
         */
        fun getInstance(context: Context): SsmtsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SsmtsDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
