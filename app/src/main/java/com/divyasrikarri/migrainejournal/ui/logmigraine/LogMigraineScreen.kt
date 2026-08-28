package com.divyasrikarri.migrainejournal.ui.logmigraine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.divyasrikarri.migrainejournal.data.model.AuraTypes
import com.divyasrikarri.migrainejournal.data.model.PainLocations
import com.divyasrikarri.migrainejournal.data.model.PainTypes
import com.divyasrikarri.migrainejournal.data.model.Symptoms
import com.divyasrikarri.migrainejournal.ui.AppViewModelProvider
import com.divyasrikarri.migrainejournal.ui.components.DateTimeField
import com.divyasrikarri.migrainejournal.ui.components.LabeledSlider
import com.divyasrikarri.migrainejournal.ui.components.MultiSelectChips
import com.divyasrikarri.migrainejournal.ui.components.PainLevelBadge
import com.divyasrikarri.migrainejournal.ui.components.SectionCard
import com.divyasrikarri.migrainejournal.ui.components.SingleSelectChips
import com.divyasrikarri.migrainejournal.ui.components.SuggestionChips
import com.divyasrikarri.migrainejournal.ui.components.StarRating
import com.divyasrikarri.migrainejournal.util.DateUtils

/**
 * Fast-entry form. Every field has a working default, so a user mid-attack can open the
 * screen and hit Save immediately; the sticky Save bar keeps that one tap always reachable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMigraineScreen(
    entryId: Long,
    onDone: () -> Unit,
    viewModel: LogMigraineViewModel = viewModel(
        key = "log_migraine_$entryId",
        factory = AppViewModelProvider.logMigraineFactory(entryId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recentMedications by viewModel.recentMedications.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit migraine" else "Log migraine") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete entry")
                        }
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
                    Text(if (state.isEditing) "Save changes" else "Save migraine")
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
                SectionCard(
                    title = "Pain",
                    trailing = { PainLevelBadge(state.painLevel) }
                ) {
                    LabeledSlider(
                        label = "Pain level",
                        value = state.painLevel.toFloat(),
                        onValueChange = { viewModel.setPainLevel(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        valueLabel = "${state.painLevel} / 10"
                    )
                    DateTimeField(
                        label = "Started",
                        epochMillis = state.startDateTime,
                        onChange = viewModel::setStart
                    )
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Still ongoing", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Add the end time later from History",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = state.ongoing, onCheckedChange = viewModel::setOngoing)
                    }
                    if (!state.ongoing) {
                        DateTimeField(
                            label = "Ended",
                            epochMillis = state.endDateTime ?: state.startDateTime,
                            onChange = viewModel::setEnd
                        )
                        state.endDateTime?.let { end ->
                            val minutes = (end - state.startDateTime).coerceAtLeast(0) / 60_000L
                            Text(
                                "Duration: ${DateUtils.formatDuration(minutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Where and how") {
                    Text("Location", style = MaterialTheme.typography.bodyMedium)
                    MultiSelectChips(
                        options = PainLocations.ALL,
                        selected = state.painLocations,
                        onToggle = viewModel::togglePainLocation
                    )
                    Text("Type", style = MaterialTheme.typography.bodyMedium)
                    SingleSelectChips(
                        options = PainTypes.ALL,
                        selected = state.painType,
                        onSelect = viewModel::setPainType
                    )
                }
            }

            item {
                SectionCard(title = "Symptoms") {
                    MultiSelectChips(
                        options = Symptoms.ALL,
                        selected = state.symptoms,
                        onToggle = viewModel::toggleSymptom
                    )
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aura", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.hasAura, onCheckedChange = viewModel::setHasAura)
                    }
                    if (state.hasAura) {
                        MultiSelectChips(
                            options = AuraTypes.ALL,
                            selected = state.auraTypes,
                            onToggle = viewModel::toggleAuraType
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Medication (optional)") {
                    if (recentMedications.isNotEmpty()) {
                        Text(
                            "Recently used",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SuggestionChips(
                            options = recentMedications,
                            onClick = viewModel::setMedicationName
                        )
                    }
                    OutlinedTextField(
                        value = state.medicationName,
                        onValueChange = viewModel::setMedicationName,
                        label = { Text("Medication") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.hasMedication) {
                        OutlinedTextField(
                            value = state.medicationDose,
                            onValueChange = viewModel::setMedicationDose,
                            label = { Text("Dose (e.g. 50 mg)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DateTimeField(
                            label = "Taken at",
                            epochMillis = state.medicationTime ?: state.startDateTime,
                            onChange = viewModel::setMedicationTime
                        )
                        Text("How well did it work?", style = MaterialTheme.typography.bodyMedium)
                        StarRating(
                            rating = state.medicationEffectiveness,
                            onRatingChange = viewModel::setMedicationEffectiveness
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Notes (optional)") {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        placeholder = { Text("Anything else worth remembering") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = {
                viewModel.consumeError()
                onDone()
            },
            title = { Text("Can't open this entry") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumeError()
                    onDone()
                }) { Text("OK") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this migraine?") },
            text = { Text("This entry will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
