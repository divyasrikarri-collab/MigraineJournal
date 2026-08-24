package com.divyasrikarri.migrainejournal.data.model

/** One bar in the "migraines over time" chart. */
data class FrequencyBucket(val label: String, val count: Int)

/** One point on the pain-level trend line. */
data class PainPoint(val label: String, val painLevel: Float)

/**
 * A single plain-language correlation callout. [detail] always states the sample size so the
 * user can judge how much weight to give it — these are descriptive counts, not predictions.
 */
data class Correlation(val headline: String, val detail: String)

data class InsightsData(
    val frequency: List<FrequencyBucket> = emptyList(),
    val painTrend: List<PainPoint> = emptyList(),
    val correlations: List<Correlation> = emptyList(),
    val totalMigraines: Int = 0,
    val averagePainLevel: Float? = null,
    val averageDurationMinutes: Long? = null
)

enum class InsightsRange(val label: String, val days: Int) {
    LAST_30("30 days", 30),
    LAST_90("90 days", 90),
    LAST_365("12 months", 365)
}

enum class FrequencyGrouping { WEEKLY, MONTHLY }
