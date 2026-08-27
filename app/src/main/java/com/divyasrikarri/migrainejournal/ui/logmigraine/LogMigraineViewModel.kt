package com.divyasrikarri.migrainejournal.ui.logmigraine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogMigraineUiState(
    val entryId: Long = 0L,
    val startDateTime: Long = System.currentTimeMillis(),
    val endDateTime: Long? = null,
    val ongoing: Boolean = true,
    val painLevel: Int = 5,
    val painLocations: Set<String> = emptySet(),
    val painType: String? = null,
    val hasAura: Boolean = false,
    val auraTypes: Set<String> = emptySet(),
    val symptoms: Set<String> = emptySet(),
    val medicationName: String = "",
    val medicationDose: String = "",
    val medicationTime: Long? = null,
    val medicationEffectiveness: Int? = null,
    val notes: String = "",
    val loading: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null
) {
    val isEditing: Boolean get() = entryId != 0L
    val hasMedication: Boolean get() = medicationName.isNotBlank()
}

class LogMigraineViewModel(
    private val repository: MigraineRepository,
    private val entryId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogMigraineUiState(entryId = entryId))
    val uiState: StateFlow<LogMigraineUiState> = _uiState.asStateFlow()

    val recentMedications: StateFlow<List<String>> = repository.observeRecentMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (entryId != 0L) loadExisting()
    }

    private fun loadExisting() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val entry = repository.getMigraine(entryId)
            if (entry == null) {
                _uiState.update { it.copy(loading = false, error = "That entry no longer exists.") }
                return@launch
            }
            _uiState.value = LogMigraineUiState(
                entryId = entry.id,
                startDateTime = entry.startDateTime,
                endDateTime = entry.endDateTime,
                ongoing = entry.endDateTime == null,
                painLevel = entry.painLevel,
                painLocations = entry.painLocations.toSet(),
                painType = entry.painType.takeIf { it.isNotBlank() },
                hasAura = entry.hasAura,
                auraTypes = entry.auraTypes.toSet(),
                symptoms = entry.symptoms.toSet(),
                medicationName = entry.medicationName.orEmpty(),
                medicationDose = entry.medicationDose.orEmpty(),
                medicationTime = entry.medicationTime,
                medicationEffectiveness = entry.medicationEffectiveness,
                notes = entry.notes.orEmpty()
            )
        }
    }

    fun setPainLevel(level: Int) = _uiState.update { it.copy(painLevel = level.coerceIn(1, 10)) }

    fun setStart(millis: Long) = _uiState.update { state ->
        // Moving the start past the end pushes the end along with it, rather than clearing it:
        // dropping the end would silently flip a finished migraine back to ongoing.
        state.copy(
            startDateTime = millis,
            endDateTime = state.endDateTime?.coerceAtLeast(millis)
        )
    }

    fun setEnd(millis: Long) = _uiState.update { state ->
        state.copy(endDateTime = millis.coerceAtLeast(state.startDateTime), ongoing = false)
    }

    fun setOngoing(ongoing: Boolean) = _uiState.update { state ->
        state.copy(
            ongoing = ongoing,
            endDateTime = if (ongoing) {
                null
            } else {
                // Default to now, but never before the start — the start may be back-dated.
                state.endDateTime ?: maxOf(System.currentTimeMillis(), state.startDateTime)
            }
        )
    }

    fun togglePainLocation(value: String) = _uiState.update {
        it.copy(painLocations = it.painLocations.toggle(value))
    }

    fun setPainType(value: String?) = _uiState.update { it.copy(painType = value) }

    fun setHasAura(value: Boolean) = _uiState.update {
        it.copy(hasAura = value, auraTypes = if (value) it.auraTypes else emptySet())
    }

    fun toggleAuraType(value: String) = _uiState.update {
        it.copy(auraTypes = it.auraTypes.toggle(value))
    }

    fun toggleSymptom(value: String) = _uiState.update {
        it.copy(symptoms = it.symptoms.toggle(value))
    }

    fun setMedicationName(value: String) = _uiState.update { state ->
        state.copy(
            medicationName = value,
            // Stamp the time on the first keystroke so the common case needs no extra tap.
            medicationTime = state.medicationTime
                ?: System.currentTimeMillis().takeIf { value.isNotBlank() }
        )
    }

    fun setMedicationDose(value: String) = _uiState.update { it.copy(medicationDose = value) }

    fun setMedicationTime(millis: Long) = _uiState.update { it.copy(medicationTime = millis) }

    fun setMedicationEffectiveness(value: Int?) =
        _uiState.update { it.copy(medicationEffectiveness = value) }

    fun setNotes(value: String) = _uiState.update { it.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveMigraine(state.toEntry())
            _uiState.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val state = _uiState.value
        if (!state.isEditing) return
        viewModelScope.launch {
            repository.getMigraine(state.entryId)?.let { repository.deleteMigraine(it) }
            _uiState.update { it.copy(deleted = true) }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    private fun LogMigraineUiState.toEntry() = MigraineEntry(
        id = entryId,
        startDateTime = startDateTime,
        endDateTime = if (ongoing) null else endDateTime,
        painLevel = painLevel,
        painLocations = painLocations.toList(),
        painType = painType.orEmpty(),
        hasAura = hasAura,
        auraTypes = if (hasAura) auraTypes.toList() else emptyList(),
        symptoms = symptoms.toList(),
        medicationName = medicationName.trim().takeIf { it.isNotBlank() },
        medicationDose = medicationDose.trim().takeIf { it.isNotBlank() },
        medicationTime = medicationTime.takeIf { medicationName.isNotBlank() },
        medicationEffectiveness = medicationEffectiveness.takeIf { medicationName.isNotBlank() },
        notes = notes.trim().takeIf { it.isNotBlank() }
    )
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
