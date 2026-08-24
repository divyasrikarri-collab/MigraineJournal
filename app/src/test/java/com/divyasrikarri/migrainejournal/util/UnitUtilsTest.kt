package com.divyasrikarri.migrainejournal.util

import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitUtilsTest {

    @Test
    fun `millilitres pass through unchanged`() {
        assertEquals(750, UnitUtils.mlToDisplay(750, VolumeUnit.MILLILITRES))
        assertEquals(750, UnitUtils.displayToMl(750, VolumeUnit.MILLILITRES))
    }

    @Test
    fun `ounces convert and round trip within rounding tolerance`() {
        assertEquals(25, UnitUtils.mlToDisplay(750, VolumeUnit.OUNCES))
        val backToMl = UnitUtils.displayToMl(25, VolumeUnit.OUNCES)
        assert(kotlin.math.abs(backToMl - 750) <= 15) { "round trip drifted to $backToMl" }
    }

    @Test
    fun `formats with the unit label`() {
        assertEquals("500 ml", UnitUtils.formatVolume(500, VolumeUnit.MILLILITRES))
        assertEquals("17 oz", UnitUtils.formatVolume(500, VolumeUnit.OUNCES))
    }
}
