package com.divyasrikarri.migrainejournal.ui.theme

import androidx.compose.ui.graphics.Color

// A calm, low-glare palette — the app gets opened mid-attack, when bright screens hurt.
val Indigo80 = Color(0xFFBFC4FF)
val IndigoGrey80 = Color(0xFFC6C5D6)
val Teal80 = Color(0xFF9FDFD3)

val Indigo40 = Color(0xFF4B4FA8)
val IndigoGrey40 = Color(0xFF5C5B70)
val Teal40 = Color(0xFF2E6F65)

/** Pain-level scale, 1..10, used by the calendar heat map and pain slider. */
private val PainScale = listOf(
    Color(0xFFA5D6A7), // 1
    Color(0xFFC5E1A5),
    Color(0xFFE6EE9C),
    Color(0xFFFFF59D),
    Color(0xFFFFE082),
    Color(0xFFFFCC80),
    Color(0xFFFFAB91),
    Color(0xFFEF9A9A),
    Color(0xFFE57373),
    Color(0xFFD32F2F)  // 10
)

fun painColor(level: Int): Color = PainScale[level.coerceIn(1, 10) - 1]
