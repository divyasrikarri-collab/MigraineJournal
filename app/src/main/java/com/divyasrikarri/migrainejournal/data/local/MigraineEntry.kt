package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single migraine attack. [endDateTime] is null while the attack is still ongoing; the
 * user can reopen the entry later from History to close it out and rate their medication.
 */
@Entity(tableName = "migraine_entries")
data class MigraineEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDateTime: Long,
    val endDateTime: Long? = null,
    val painLevel: Int,
    val painLocations: List<String> = emptyList(),
    val painType: String = "",
    val hasAura: Boolean = false,
    val auraTypes: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val medicationName: String? = null,
    val medicationDose: String? = null,
    val medicationTime: Long? = null,
    val medicationEffectiveness: Int? = null,
    val notes: String? = null
) {
    val isOngoing: Boolean get() = endDateTime == null

    /** Attack length in minutes, or null while ongoing. */
    val durationMinutes: Long?
        get() = endDateTime?.let { (it - startDateTime).coerceAtLeast(0L) / 60_000L }
}
