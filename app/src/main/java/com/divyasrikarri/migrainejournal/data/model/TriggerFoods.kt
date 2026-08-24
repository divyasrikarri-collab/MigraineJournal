package com.divyasrikarri.migrainejournal.data.model

import java.util.Locale

/**
 * Static seed list of foods commonly reported as migraine triggers. Matching is a plain
 * substring check against the typed food name — deliberately simple and inspectable, and
 * only ever used to *tag* an entry, never to tell the user a food caused an attack.
 */
object TriggerFoods {

    data class Category(val name: String, val keywords: List<String>)

    val CATEGORIES = listOf(
        Category(
            "Caffeine",
            listOf("coffee", "espresso", "latte", "cappuccino", "americano", "tea", "matcha", "cola", "energy drink")
        ),
        Category(
            "Alcohol",
            listOf("wine", "beer", "whisky", "whiskey", "vodka", "gin", "rum", "cocktail", "champagne", "prosecco")
        ),
        Category(
            "Chocolate",
            listOf("chocolate", "cocoa", "cacao", "brownie", "nutella")
        ),
        Category(
            "Aged cheese",
            listOf("cheddar", "parmesan", "blue cheese", "gouda", "brie", "camembert", "feta", "swiss cheese")
        ),
        Category(
            "Processed meat",
            listOf("bacon", "salami", "pepperoni", "hot dog", "sausage", "ham", "prosciutto", "deli meat", "jerky")
        ),
        Category(
            "MSG",
            listOf("msg", "instant noodle", "ramen", "bouillon", "stock cube", "takeout")
        ),
        Category(
            "Artificial sweeteners",
            listOf("aspartame", "sucralose", "diet soda", "diet coke", "sugar free", "sweetener", "splenda")
        ),
        Category(
            "Citrus",
            listOf("orange", "lemon", "lime", "grapefruit", "citrus", "tangerine", "mandarin")
        )
    )

    val CATEGORY_NAMES: List<String> = CATEGORIES.map { it.name }

    /** Suggestions offered before the user has any food history of their own. */
    val COMMON_FOOD_SUGGESTIONS = listOf(
        "Coffee", "Tea", "Red wine", "Dark chocolate", "Cheddar cheese",
        "Bacon", "Orange juice", "Diet soda", "Instant noodles", "Yogurt"
    )

    /** Returns the trigger category matching [foodName], or null if none does. */
    fun categorize(foodName: String): String? {
        val needle = foodName.lowercase(Locale.US).trim()
        if (needle.isEmpty()) return null
        return CATEGORIES.firstOrNull { category ->
            category.keywords.any { needle.contains(it) }
        }?.name
    }
}
