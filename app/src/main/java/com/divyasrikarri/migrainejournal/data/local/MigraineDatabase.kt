package com.divyasrikarri.migrainejournal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MigraineEntry::class, DailyLog::class, FoodEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MigraineDatabase : RoomDatabase() {

    abstract fun migraineDao(): MigraineDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun foodEntryDao(): FoodEntryDao

    companion object {
        private const val NAME = "migraine_journal.db"

        @Volatile
        private var instance: MigraineDatabase? = null

        fun getInstance(context: Context): MigraineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MigraineDatabase::class.java,
                    NAME
                ).build().also { instance = it }
            }
    }
}
