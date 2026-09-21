/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Room database implementation managing the local SQLite storage 'ssmts_local.db'.
 */

package com.sliit.ssmts.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sliit.ssmts.data.local.dao.SessionDao
import com.sliit.ssmts.data.local.entity.SessionEntity

/**
 * Main Room SQLite database for local persistence across offline intervals and session restarts.
 */
@Database(
    entities = [SessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SsmtsDatabase : RoomDatabase() {

    /**
     * Provides access to the SessionDao operations.
     */
    abstract fun sessionDao(): SessionDao

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
