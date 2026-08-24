package com.divyasrikarri.migrainejournal.util

import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Water intake is always persisted in millilitres; these helpers handle display only. */
object UnitUtils {

    private const val ML_PER_OZ = 29.5735f

    /** A "glass" quick-tap adds this much water. */
    const val GLASS_ML = 250

    fun mlToDisplay(ml: Int, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.MILLILITRES -> ml
        VolumeUnit.OUNCES -> (ml / ML_PER_OZ).roundToInt()
    }

    fun displayToMl(value: Int, unit: VolumeUnit): Int = when (unit) {
        VolumeUnit.MILLILITRES -> value
        VolumeUnit.OUNCES -> (value * ML_PER_OZ).roundToInt()
    }

    fun formatVolume(ml: Int, unit: VolumeUnit): String =
        "${mlToDisplay(ml, unit)} ${unit.label}"

    fun formatHours(hours: Float): String = String.format(Locale.getDefault(), "%.1f h", hours)
}
