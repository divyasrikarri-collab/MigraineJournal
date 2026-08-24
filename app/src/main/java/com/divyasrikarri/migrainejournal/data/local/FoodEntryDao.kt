package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodEntryDao {

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Delete
    suspend fun delete(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY mealTime ASC")
    fun observeByDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY mealTime ASC")
    suspend fun getByDate(date: String): List<FoodEntry>

    @Query("SELECT * FROM food_entries ORDER BY date ASC, mealTime ASC")
    suspend fun getAllOnce(): List<FoodEntry>

    /** Distinct previously logged foods, most recent first — powers the autocomplete. */
    @Query(
        "SELECT foodName FROM food_entries GROUP BY foodName " +
            "ORDER BY MAX(mealTime) DESC LIMIT :limit"
    )
    fun observeKnownFoods(limit: Int): Flow<List<String>>

    @Query("DELETE FROM food_entries")
    suspend fun deleteAll()
}
