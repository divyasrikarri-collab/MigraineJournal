package com.divyasrikarri.migrainejournal.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyasrikarri.migrainejournal.data.model.Correlation
import com.divyasrikarri.migrainejournal.data.model.FrequencyGrouping
import com.divyasrikarri.migrainejournal.data.model.InsightsRange
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.EmptyState
import com.divyasrikarri.migrainejournal.ui.components.FrequencyBarChart
import com.divyasrikarri.migrainejournal.ui.components.PainTrendChart
import com.divyasrikarri.migrainejournal.ui.components.SectionCard
import com.divyasrikarri.migrainejournal.ui.components.StatTile
import com.divyasrikarri.migrainejournal.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val grouping by viewModel.grouping.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Insights") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    InsightsRange.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = range == option,
                            onClick = { viewModel.setRange(option) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = InsightsRange.entries.size
                            )
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Migraines",
                        value = insights.totalMigraines.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Avg pain",
                        value = insights.averagePainLevel
                            ?.let { String.format(Locale.getDefault(), "%.1f", it) }
                            ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Avg duration",
                        value = insights.averageDurationMinutes
                            ?.let { DateUtils.formatDuration(it) }
                            ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SectionCard(
                    title = "Frequency",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FrequencyGrouping.entries.forEach { option ->
                                FilterChip(
                                    selected = grouping == option,
                                    onClick = { viewModel.setGrouping(option) },
                                    label = {
                                        Text(
                                            if (option == FrequencyGrouping.WEEKLY) {
                                                "Weekly"
                                            } else {
                                                "Monthly"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) {
                    FrequencyBarChart(buckets = insights.frequency)
                }
            }

            item {
                SectionCard(title = "Pain level trend") {
                    PainTrendChart(points = insights.painTrend)
                }
            }

            item {
                Text(
                    "Patterns",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (insights.correlations.isEmpty()) {
                item {
                    EmptyState(
                        "Patterns appear once there are a few migraines and daily check-ins " +
                            "to compare."
                    )
                }
            } else {
                items(insights.correlations) { correlation -> CorrelationCard(correlation) }
                item {
                    Text(
                        "These are plain counts over what you logged — co-occurrence, not " +
                            "cause. Bring them to a clinician rather than treating them as a " +
                            "diagnosis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CorrelationCard(correlation: Correlation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(correlation.headline, style = MaterialTheme.typography.bodyLarge)
            Text(
                correlation.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
