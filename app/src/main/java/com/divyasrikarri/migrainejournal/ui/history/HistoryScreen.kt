package com.divyasrikarri.migrainejournal.ui.history

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.EmptyState
import com.divyasrikarri.migrainejournal.ui.components.SectionCard
import com.divyasrikarri.migrainejournal.ui.home.MigraineCard
import com.divyasrikarri.migrainejournal.util.DateUtils
import com.divyasrikarri.migrainejournal.util.UnitUtils
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenMigraine: (Long) -> Unit,
    onOpenCheckIn: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val monthView by viewModel.monthView.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayDetail by viewModel.dayDetail.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val allMigraines by viewModel.allMigraines.collectAsStateWithLifecycle()
    val volumeUnit by viewModel.volumeUnit.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) },
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
                    HistoryViewMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = viewMode == mode,
                            onClick = { viewModel.setViewMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HistoryViewMode.entries.size
                            )
                        ) {
                            Text(if (mode == HistoryViewMode.CALENDAR) "Calendar" else "List")
                        }
                    }
                }
            }

            if (viewMode == HistoryViewMode.CALENDAR) {
                item {
                    MonthCalendar(
                        monthView = monthView,
                        selectedDate = selectedDate,
                        onSelectDate = viewModel::selectDate,
                        onPreviousMonth = viewModel::showPreviousMonth,
                        onNextMonth = viewModel::showNextMonth
                    )
                }
                item { HorizontalDivider() }
                item {
                    DayDetailSection(
                        detail = dayDetail,
                        volumeUnit = volumeUnit,
                        onOpenMigraine = onOpenMigraine,
                        onOpenCheckIn = onOpenCheckIn
                    )
                }
            } else {
                if (allMigraines.isEmpty()) {
                    item { EmptyState("No migraines logged yet.") }
                } else {
                    items(allMigraines, key = { it.id }) { entry ->
                        MigraineCard(entry = entry, onClick = { onOpenMigraine(entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailSection(
    detail: DayDetail,
    volumeUnit: VolumeUnit,
    onOpenMigraine: (Long) -> Unit,
    onOpenCheckIn: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            DateUtils.formatDate(detail.date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (detail.migraines.isEmpty()) {
            Text(
                "No migraine logged on this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            detail.migraines.forEach { entry ->
                MigraineCard(entry = entry, onClick = { onOpenMigraine(entry.id) })
            }
        }

        SectionCard(
            title = "Daily check-in",
            trailing = {
                OutlinedButton(onClick = { onOpenCheckIn(DateUtils.toKey(detail.date)) }) {
                    Text(if (detail.dailyLog?.hasContent == true) "Edit" else "Fill in")
                }
            }
        ) {
            val log = detail.dailyLog
            if (log == null || !log.hasContent) {
                Text(
                    "Nothing recorded for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                log.sleepHours?.let { DetailRow("Sleep", UnitUtils.formatHours(it)) }
                log.sleepQuality?.let { DetailRow("Sleep quality", "$it / 5") }
                log.waterIntakeMl?.let {
                    DetailRow("Water", UnitUtils.formatVolume(it, volumeUnit))
                }
                log.stressLevel?.let { DetailRow("Stress", "$it / 5") }
                if (log.exercised) {
                    DetailRow(
                        "Exercise",
                        listOfNotNull(
                            log.exerciseType,
                            log.exerciseDurationMin?.let { "$it min" }
                        ).joinToString(", ").ifEmpty { "Yes" }
                    )
                }
                log.screenTimeHours?.let { DetailRow("Screen time", UnitUtils.formatHours(it)) }
                log.weatherPressure?.let { DetailRow("Pressure", "$it hPa") }
                log.menstrualCycleDay?.let { DetailRow("Cycle day", it.toString()) }
            }

            if (detail.foods.isNotEmpty()) {
                HorizontalDivider()
                Text("Food and drink", style = MaterialTheme.typography.bodyMedium)
                detail.foods.forEach { food ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${DateUtils.formatTime(food.mealTime)}  ${food.foodName}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        food.triggerCategory?.let { category ->
                            AssistChip(onClick = {}, label = { Text(category) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
