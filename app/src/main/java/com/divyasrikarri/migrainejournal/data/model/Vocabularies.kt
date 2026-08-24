package com.divyasrikarri.migrainejournal.data.model

/**
 * Fixed option vocabularies used by the chip pickers. Values are stored verbatim in the
 * database, so entries here must not be renamed without a migration.
 */
object PainLocations {
    const val LEFT_TEMPLE = "Left temple"
    const val RIGHT_TEMPLE = "Right temple"

    val ALL = listOf(
        LEFT_TEMPLE,
        RIGHT_TEMPLE,
        "Forehead",
        "Behind eyes",
        "Back of head",
        "Neck",
        "Whole head"
    )
}

object PainTypes {
    val ALL = listOf("Throbbing", "Pressure", "Sharp", "Dull")
}

object AuraTypes {
    val ALL = listOf("Visual", "Sensory", "Speech")
}

object Symptoms {
    val ALL = listOf(
        "Nausea",
        "Vomiting",
        "Light sensitivity",
        "Sound sensitivity",
        "Smell sensitivity",
        "Dizziness",
        "Neck stiffness",
        "Fatigue"
    )
}

object ExerciseTypes {
    val ALL = listOf("Walk", "Run", "Cycling", "Strength", "Yoga", "Swimming", "Other")
}
