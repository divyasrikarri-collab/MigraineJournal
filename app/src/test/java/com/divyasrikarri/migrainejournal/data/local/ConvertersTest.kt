package com.divyasrikarri.migrainejournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round trips a list`() {
        val original = listOf("Nausea", "Light sensitivity", "Dizziness")
        assertEquals(original, converters.toStringList(converters.fromStringList(original)))
    }

    @Test
    fun `empty values collapse to an empty list`() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals("", converters.fromStringList(emptyList()))
    }

    @Test
    fun `blank entries are dropped rather than becoming empty strings`() {
        assertEquals("Nausea", converters.fromStringList(listOf("Nausea", "", "  ")))
        assertEquals(listOf("Nausea"), converters.toStringList("Nausea||"))
    }

    @Test
    fun `values containing commas survive the round trip`() {
        val original = listOf("Behind eyes, left", "Neck")
        assertEquals(original, converters.toStringList(converters.fromStringList(original)))
    }
}
