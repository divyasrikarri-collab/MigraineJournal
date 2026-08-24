package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A single food or drink logged against a day. Linked to [DailyLog.date] by value. */
@Entity(tableName = "food_entries", indices = [Index("date")])
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val foodName: String,
    val mealTime: Long,
    val isCommonTrigger: Boolean = false,
    val triggerCategory: String? = null
)
