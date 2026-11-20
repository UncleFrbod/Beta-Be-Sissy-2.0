package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoricalAppUsageEntity::class,
        RestrictionSessionLogEntity::class,
        ScheduledAppBlockTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BeSissyDatabase : RoomDatabase() {
    abstract fun usageHistoryDao(): UsageHistoryDao
    abstract fun scheduledAppBlockDao(): ScheduledAppBlockDao

    companion object {
        @Volatile
        private var instance: BeSissyDatabase? = null

        fun getInstance(context: Context): BeSissyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeSissyDatabase::class.java,
                    "besissy_usage_history.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
