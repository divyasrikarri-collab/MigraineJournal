package com.divyasrikarri.migrainejournal.data.model

/** Volume display unit for water intake. Storage is always millilitres. */
enum class VolumeUnit(val label: String) {
    MILLILITRES("ml"),
    OUNCES("oz");

    companion object {
        fun fromName(name: String?): VolumeUnit =
            entries.firstOrNull { it.name == name } ?: MILLILITRES
    }
}
