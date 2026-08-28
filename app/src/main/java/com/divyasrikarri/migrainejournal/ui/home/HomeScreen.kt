package com.divyasrikarri.migrainejournal.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.EmptyState
import com.divyasrikarri.migrainejournal.ui.components.PainLevelBadge
import com.divyasrikarri.migrainejournal.ui.components.StatTile
import com.divyasrikarri.migrainejournal.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogMigraine: () -> Unit,
    onOpenMigraine: (Long) -> Unit,
    onOpenCheckIn: () -> Unit,
    onSeeAllHistory: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Migraine Journal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        // The host Scaffold already reserves room for the navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = onLogMigraine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Text("Log migraine now", style = MaterialTheme.typography.titleMedium)
                }
            }

            state.ongoingMigraine?.let { ongoing ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMigraine(ongoing.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Migraine in progress",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Started ${DateUtils.formatDateTime(ongoing.startDateTime)} — " +
                                    "tap to add an end time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            item { CheckInCard(done = state.checkInDone, onClick = onOpenCheckIn) }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "This month",
                        value = state.stats.migrainesThisMonth.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Avg pain",
                        value = state.stats.averagePainLevel
                            ?.let { String.format(Locale.getDefault(), "%.1f", it) }
                            ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Days migraine-free",
                        value = state.stats.currentStreakDays?.toString() ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent migraines", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onSeeAllHistory) { Text("See all") }
                }
            }

            if (state.recentMigraines.isEmpty()) {
                item { EmptyState("Nothing logged yet. The button above takes about 20 seconds.") }
            } else {
                items(state.recentMigraines, key = { it.id }) { entry ->
                    MigraineCard(entry = entry, onClick = { onOpenMigraine(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun CheckInCard(done: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (done) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (done) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.RadioButtonUnchecked
                },
                contentDescription = null
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (done) "Today's check-in is done" else "Today's check-in",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (done) {
                        "Tap to review or update it"
                    } else {
                        "Sleep, water, stress, food — about a minute"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun MigraineCard(entry: MigraineEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PainLevelBadge(entry.painLevel)
            Column(Modifier.weight(1f)) {
                Text(
                    DateUtils.formatDateTime(entry.startDateTime),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = buildString {
                        append(
                            entry.durationMinutes
                                ?.let { DateUtils.formatDuration(it) }
                                ?: "Ongoing"
                        )
                        if (entry.symptoms.isNotEmpty()) {
                            append(" · ")
                            append(entry.symptoms.take(2).joinToString(", "))
                            if (entry.symptoms.size > 2) append(" +${entry.symptoms.size - 2}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entry.medicationName?.takeIf { it.isNotBlank() }?.let { medication ->
                    Text(
                        medication,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
