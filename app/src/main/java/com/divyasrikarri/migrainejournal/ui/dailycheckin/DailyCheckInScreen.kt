package com.divyasrikarri.migrainejournal.ui.dailycheckin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyasrikarri.migrainejournal.data.model.ExerciseTypes
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.LabeledSlider
import com.divyasrikarri.migrainejournal.ui.components.SectionCard
import com.divyasrikarri.migrainejournal.ui.components.SingleSelectChips
import com.divyasrikarri.migrainejournal.ui.components.StarRating
import com.divyasrikarri.migrainejournal.ui.components.SuggestionChips
import com.divyasrikarri.migrainejournal.ui.components.roundToTenth
import com.divyasrikarri.migrainejournal.util.DateUtils
import com.divyasrikarri.migrainejournal.util.UnitUtils

private val STRESS_LABELS = listOf("Very low", "Low", "Moderate", "High", "Very high")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckInScreen(
    dateKey: String?,
    onDone: () -> Unit,
    viewModel: DailyCheckInViewModel = viewModel(
        key = "check_in_${dateKey ?: "today"}",
        factory = AppViewModelProvider.dailyCheckInFactory(dateKey)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val foodQuery by viewModel.foodQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.foodSuggestions.collectAsStateWithLifecycle()
    val queryTrigger by viewModel.queryTriggerCategory.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily check-in") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Text("Save check-in")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    DateUtils.formatDate(state.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionCard(title = "Sleep") {
                    LabeledSlider(
                        label = "Hours slept",
                        value = state.sleepHours ?: 7f,
                        onValueChange = { viewModel.setSleepHours(it.roundToTenth()) },
                        valueRange = 0f..14f,
                        steps = 27,
                        valueLabel = state.sleepHours?.let { UnitUtils.formatHours(it) } ?: "not set"
                    )
                    Text("Quality", style = MaterialTheme.typography.bodyMedium)
                    StarRating(
                        rating = state.sleepQuality,
                        onRatingChange = viewModel::setSleepQuality
                    )
                }
            }

            item {
                SectionCard(
                    title = "Water",
                    trailing = {
                        Text(
                            UnitUtils.formatVolume(state.waterIntakeMl, state.volumeUnit),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.addWater(UnitUtils.GLASS_ML) },
                            modifier = Modifier.weight(1f)
                        ) { Text("+1 glass") }
                        OutlinedButton(
                            onClick = { viewModel.addWater(-UnitUtils.GLASS_ML) },
                            enabled = state.waterIntakeMl > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("−1 glass") }
                        OutlinedButton(
                            onClick = { viewModel.setWater(0) },
                            enabled = state.waterIntakeMl > 0
                        ) { Text("Clear") }
                    }
                    OutlinedTextField(
                        value = if (state.waterIntakeMl == 0) {
                            ""
                        } else {
                            UnitUtils.mlToDisplay(state.waterIntakeMl, state.volumeUnit).toString()
                        },
                        onValueChange = { input ->
                            val entered = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                            viewModel.setWater(UnitUtils.displayToMl(entered, state.volumeUnit))
                        },
                        label = { Text("Exact amount (${state.volumeUnit.label})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SectionCard(title = "Stress") {
                    LabeledSlider(
                        label = "Stress level",
                        value = (state.stressLevel ?: 3).toFloat(),
                        onValueChange = { viewModel.setStressLevel(it.toInt()) },
                        valueRange = 1f..5f,
                        steps = 3,
                        valueLabel = state.stressLevel
                            ?.let { STRESS_LABELS[it - 1] }
                            ?: "not set"
                    )
                }
            }

            item {
                SectionCard(title = "Exercise") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exercised today", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.exercised, onCheckedChange = viewModel::setExercised)
                    }
                    if (state.exercised) {
                        SingleSelectChips(
                            options = ExerciseTypes.ALL,
                            selected = state.exerciseType,
                            onSelect = viewModel::setExerciseType
                        )
                        OutlinedTextField(
                            value = state.exerciseDurationMin?.toString().orEmpty(),
                            onValueChange = {
                                viewModel.setExerciseDuration(
                                    it.filter { char -> char.isDigit() }.toIntOrNull()
                                )
                            },
                            label = { Text("Duration (minutes)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Food and drink") {
                    OutlinedTextField(
                        value = foodQuery,
                        onValueChange = viewModel::setFoodQuery,
                        label = { Text("Add a food or drink") },
                        singleLine = true,
                        trailingIcon = {
                            if (foodQuery.isNotBlank()) {
                                TextButton(
                                    onClick = { viewModel.addFood() }
                                ) { Text("Add") }
                            }
                        },
                        supportingText = {
                            queryTrigger?.let {
                                Text("Will be tagged as a common trigger: $it")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (suggestions.isNotEmpty()) {
                        SuggestionChips(
                            options = suggestions,
                            onClick = { viewModel.addFood(it) }
                        )
                    }
                    if (foods.isNotEmpty()) {
                        HorizontalDivider()
                        foods.forEach { food ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(food.foodName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        DateUtils.formatTime(food.mealTime),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                food.triggerCategory?.let { category ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(category) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor =
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }
                                IconButton(onClick = { viewModel.removeFood(food) }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove ${food.foodName}"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Other (optional)") {
                    LabeledSlider(
                        label = "Screen time",
                        value = state.screenTimeHours ?: 4f,
                        onValueChange = { viewModel.setScreenTime(it.roundToTenth()) },
                        valueRange = 0f..16f,
                        steps = 31,
                        valueLabel = state.screenTimeHours
                            ?.let { UnitUtils.formatHours(it) }
                            ?: "not set"
                    )
                    OutlinedTextField(
                        value = state.weatherPressure,
                        onValueChange = viewModel::setWeatherPressure,
                        label = { Text("Barometric pressure (hPa)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.trackMenstrualCycle) {
                        OutlinedTextField(
                            value = state.menstrualCycleDay,
                            onValueChange = viewModel::setMenstrualCycleDay,
                            label = { Text("Cycle day") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
