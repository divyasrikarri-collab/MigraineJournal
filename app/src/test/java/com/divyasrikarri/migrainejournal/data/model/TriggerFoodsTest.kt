package com.divyasrikarri.migrainejournal.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TriggerFoodsTest {

    @Test
    fun `matches a known trigger regardless of case or surrounding words`() {
        assertEquals("Caffeine", TriggerFoods.categorize("Large oat COFFEE"))
        assertEquals("Aged cheese", TriggerFoods.categorize("cheddar on toast"))
        assertEquals("Processed meat", TriggerFoods.categorize("Bacon sandwich"))
    }

    @Test
    fun `returns null for foods that are not on the seed list`() {
        assertNull(TriggerFoods.categorize("Porridge"))
        assertNull(TriggerFoods.categorize(""))
        assertNull(TriggerFoods.categorize("   "))
    }

    @Test
    fun `every category exposes at least one keyword`() {
        TriggerFoods.CATEGORIES.forEach { category ->
            assert(category.keywords.isNotEmpty()) { "${category.name} has no keywords" }
        }
    }
}
