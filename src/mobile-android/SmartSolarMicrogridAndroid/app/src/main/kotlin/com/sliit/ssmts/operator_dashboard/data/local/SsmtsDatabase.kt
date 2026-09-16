/**
 * Description: Room Database configuration for ssmts_local.db managing local SQLite tables
 * tbl_reservations_cache and tbl_operator_audit_cache for Member 4.
 */
package com.sliit.ssmts.operator_dashboard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliit.ssmts.operator_dashboard.data.local.dao.OperatorAuditDao
import com.sliit.ssmts.operator_dashboard.data.local.dao.ReservationCacheDao
import com.sliit.ssmts.operator_dashboard.data.local.entity.OperatorAuditEntity
import com.sliit.ssmts.operator_dashboard.data.local.entity.ReservationCacheEntity

/**
 * Main Room database provider managing local SQLite persistence for Member 4.
 */
@Database(
    entities = [
        ReservationCacheEntity::class,
        OperatorAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SsmtsDatabase : RoomDatabase() {

    /**
     * Provides access to reservation cache database operations.
     *
     * @return ReservationCacheDao instance.
     */
    abstract fun reservationCacheDao(): ReservationCacheDao

    /**
     * Provides access to operator audit database operations.
     *
     * @return OperatorAuditDao instance.
     */
    abstract fun operatorAuditDao(): OperatorAuditDao

    companion object {
        /**
         * Database name on disk adhering to project specifications.
         */
        const val DATABASE_NAME = "ssmts_local.db"

        @Volatile
        private var instance: SsmtsDatabase? = null

        /**
         * Thread-safe singleton getter for SsmtsDatabase instance.
         *
         * @param context Android application or activity context.
         * @return Initialized SsmtsDatabase instance.
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
