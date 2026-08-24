package com.divyasrikarri.migrainejournal.data.local

import androidx.room.TypeConverter

/**
 * Room converters for the `List<String>` columns on [MigraineEntry].
 *
 * Values are stored as a `|` delimited string rather than JSON: the lists here are short,
 * fixed vocabularies (pain locations, aura types, symptoms) and avoiding a serialization
 * dependency keeps the schema readable in a raw CSV export. `|` is used instead of a comma
 * so a value containing a comma still round trips.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        value.filter { it.isNotBlank() }.joinToString(DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.split(DELIMITER).filter { it.isNotBlank() }

    private companion object {
        const val DELIMITER = "|"
    }
}
