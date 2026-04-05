package com.graygate.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.graygate.data.model.SelectedApp

@Database(
    entities = [SelectedApp::class],
    version = 1,
    exportSchema = false
)
abstract class GrayGateDatabase : RoomDatabase() {

    abstract fun selectedAppDao(): SelectedAppDao

    companion object {
        @Volatile
        private var INSTANCE: GrayGateDatabase? = null

        fun getInstance(context: Context): GrayGateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrayGateDatabase::class.java,
                    "graygate_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
