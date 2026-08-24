package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    @Upsert
    suspend fun upsert(log: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun getByDate(date: String): DailyLog?

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun observeInRange(from: String, to: String): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs ORDER BY date ASC")
    suspend fun getAllOnce(): List<DailyLog>

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAll()
}
