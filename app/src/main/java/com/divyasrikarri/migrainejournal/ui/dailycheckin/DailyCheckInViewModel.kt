package com.divyasrikarri.migrainejournal.ui.dailycheckin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.model.TriggerFoods
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.data.repository.SettingsRepository
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class DailyCheckInUiState(
    val date: LocalDate = DateUtils.today(),
    val sleepHours: Float? = null,
    val sleepQuality: Int? = null,
    val waterIntakeMl: Int = 0,
    val stressLevel: Int? = null,
    val exercised: Boolean = false,
    val exerciseType: String? = null,
    val exerciseDurationMin: Int? = null,
    val weatherPressure: String = "",
    val menstrualCycleDay: String = "",
    val screenTimeHours: Float? = null,
    val trackMenstrualCycle: Boolean = false,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITRES,
    val saved: Boolean = false
)

class DailyCheckInViewModel(
    private val repository: MigraineRepository,
    private val settingsRepository: SettingsRepository,
    dateKey: String?
) : ViewModel() {

    private val date: LocalDate = dateKey?.let { DateUtils.parseKeyOrNull(it) } ?: DateUtils.today()

    private val _uiState = MutableStateFlow(DailyCheckInUiState(date = date))
    val uiState: StateFlow<DailyCheckInUiState> = _uiState.asStateFlow()

    val foods: StateFlow<List<FoodEntry>> = repository.observeFoodForDay(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val knownFoods: StateFlow<List<String>> = repository.observeKnownFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _foodQuery = MutableStateFlow("")
    val foodQuery: StateFlow<String> = _foodQuery.asStateFlow()

    /** Autocomplete over the user's own history, backfilled with common foods when new. */
    val foodSuggestions: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(
        _foodQuery,
        knownFoods
    ) { query, known ->
        val pool = (known + TriggerFoods.COMMON_FOOD_SUGGESTIONS).distinctBy {
            it.lowercase(Locale.getDefault())
        }
        val needle = query.trim().lowercase(Locale.getDefault())
        if (needle.isEmpty()) {
            pool.take(6)
        } else {
            pool.filter { it.lowercase(Locale.getDefault()).contains(needle) }.take(6)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Live trigger tag preview for whatever is currently typed. */
    val queryTriggerCategory: StateFlow<String?> = _foodQuery
        .map { TriggerFoods.categorize(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val existing = repository.getDailyLog(date)
            _uiState.update { state ->
                state.copy(
                    sleepHours = existing?.sleepHours,
                    sleepQuality = existing?.sleepQuality,
                    waterIntakeMl = existing?.waterIntakeMl ?: 0,
                    stressLevel = existing?.stressLevel,
                    exercised = existing?.exercised ?: false,
                    exerciseType = existing?.exerciseType,
                    exerciseDurationMin = existing?.exerciseDurationMin,
                    weatherPressure = existing?.weatherPressure?.toString().orEmpty(),
                    menstrualCycleDay = existing?.menstrualCycleDay?.toString().orEmpty(),
                    screenTimeHours = existing?.screenTimeHours,
                    trackMenstrualCycle = settings.trackMenstrualCycle,
                    volumeUnit = settings.volumeUnit
                )
            }
        }
    }

    fun setSleepHours(hours: Float?) = _uiState.update { it.copy(sleepHours = hours) }

    fun setSleepQuality(quality: Int?) = _uiState.update { it.copy(sleepQuality = quality) }

    fun addWater(ml: Int) = _uiState.update {
        it.copy(waterIntakeMl = (it.waterIntakeMl + ml).coerceAtLeast(0))
    }

    fun setWater(ml: Int) = _uiState.update { it.copy(waterIntakeMl = ml.coerceAtLeast(0)) }

    fun setStressLevel(level: Int?) = _uiState.update { it.copy(stressLevel = level) }

    fun setExercised(value: Boolean) = _uiState.update {
        it.copy(
            exercised = value,
            exerciseType = if (value) it.exerciseType else null,
            exerciseDurationMin = if (value) it.exerciseDurationMin else null
        )
    }

    fun setExerciseType(type: String?) = _uiState.update { it.copy(exerciseType = type) }

    fun setExerciseDuration(minutes: Int?) =
        _uiState.update { it.copy(exerciseDurationMin = minutes) }

    fun setScreenTime(hours: Float?) = _uiState.update { it.copy(screenTimeHours = hours) }

    fun setWeatherPressure(value: String) = _uiState.update { it.copy(weatherPressure = value) }

    fun setMenstrualCycleDay(value: String) = _uiState.update { it.copy(menstrualCycleDay = value) }

    fun setFoodQuery(value: String) {
        _foodQuery.value = value
    }

    fun addFood(name: String = _foodQuery.value) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addFood(date, trimmed, System.currentTimeMillis())
            _foodQuery.value = ""
        }
    }

    fun removeFood(entry: FoodEntry) {
        viewModelScope.launch { repository.deleteFood(entry) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveDailyLog(
                DailyLog(
                    date = DateUtils.toKey(date),
                    sleepHours = state.sleepHours,
                    sleepQuality = state.sleepQuality,
                    waterIntakeMl = state.waterIntakeMl.takeIf { it > 0 },
                    stressLevel = state.stressLevel,
                    exercised = state.exercised,
                    exerciseType = state.exerciseType,
                    exerciseDurationMin = state.exerciseDurationMin,
                    weatherPressure = state.weatherPressure.trim().toFloatOrNull(),
                    menstrualCycleDay = state.menstrualCycleDay.trim().toIntOrNull()
                        ?.takeIf { state.trackMenstrualCycle },
                    screenTimeHours = state.screenTimeHours
                )
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
