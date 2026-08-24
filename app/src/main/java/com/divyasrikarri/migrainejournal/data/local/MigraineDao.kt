package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MigraineDao {

    @Insert
    suspend fun insert(entry: MigraineEntry): Long

    @Update
    suspend fun update(entry: MigraineEntry)

    @Delete
    suspend fun delete(entry: MigraineEntry)

    @Query("SELECT * FROM migraine_entries WHERE id = :id")
    suspend fun getById(id: Long): MigraineEntry?

    @Query("SELECT * FROM migraine_entries ORDER BY startDateTime DESC")
    fun observeAll(): Flow<List<MigraineEntry>>

    @Query("SELECT * FROM migraine_entries ORDER BY startDateTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MigraineEntry>>

    @Query(
        "SELECT * FROM migraine_entries WHERE startDateTime >= :from AND startDateTime < :to " +
            "ORDER BY startDateTime ASC"
    )
    fun observeInRange(from: Long, to: Long): Flow<List<MigraineEntry>>

    @Query(
        "SELECT * FROM migraine_entries WHERE startDateTime >= :from AND startDateTime < :to " +
            "ORDER BY startDateTime ASC"
    )
    suspend fun getInRange(from: Long, to: Long): List<MigraineEntry>

    @Query("SELECT * FROM migraine_entries ORDER BY startDateTime ASC")
    suspend fun getAllOnce(): List<MigraineEntry>

    /** Most recently used medication names, for the quick-add row on the log screen. */
    @Query(
        "SELECT medicationName FROM migraine_entries WHERE medicationName IS NOT NULL " +
            "AND TRIM(medicationName) != '' GROUP BY medicationName " +
            "ORDER BY MAX(startDateTime) DESC LIMIT :limit"
    )
    fun observeRecentMedications(limit: Int): Flow<List<String>>

    @Query("SELECT MAX(startDateTime) FROM migraine_entries")
    fun observeLatestStart(): Flow<Long?>

    @Query("DELETE FROM migraine_entries")
    suspend fun deleteAll()
}
