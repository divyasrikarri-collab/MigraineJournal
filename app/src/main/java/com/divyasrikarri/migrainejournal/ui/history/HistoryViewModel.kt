package com.divyasrikarri.migrainejournal.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.data.repository.SettingsRepository
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class HistoryViewMode { CALENDAR, LIST }

data class MonthView(
    val month: LocalDate = DateUtils.today().withDayOfMonth(1),
    val painByDay: Map<LocalDate, Int> = emptyMap(),
    val checkedInDays: Set<LocalDate> = emptySet()
)

data class DayDetail(
    val date: LocalDate,
    val dailyLog: DailyLog? = null,
    val migraines: List<MigraineEntry> = emptyList(),
    val foods: List<FoodEntry> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: MigraineRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _month = MutableStateFlow(DateUtils.today().withDayOfMonth(1))
    private val _selectedDate = MutableStateFlow(DateUtils.today())
    private val _viewMode = MutableStateFlow(HistoryViewMode.CALENDAR)

    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    val viewMode: StateFlow<HistoryViewMode> = _viewMode.asStateFlow()

    val monthView: StateFlow<MonthView> = _month.flatMapLatest { month ->
        combine(
            repository.observeMonthPainByDay(month),
            repository.observeMonthCheckedInDays(month)
        ) { painByDay, checkedIn ->
            MonthView(month = month, painByDay = painByDay, checkedInDays = checkedIn)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthView())

    val dayDetail: StateFlow<DayDetail> = _selectedDate.flatMapLatest { date ->
        combine(
            repository.observeDailyLog(date),
            repository.observeMigrainesOnDay(date),
            repository.observeFoodForDay(date)
        ) { log, migraines, foods ->
            DayDetail(date = date, dailyLog = log, migraines = migraines, foods = foods)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DayDetail(DateUtils.today())
    )

    /** Display unit for the water read-out on the day detail. */
    val volumeUnit: StateFlow<VolumeUnit> = settingsRepository.settings
        .map { it.volumeUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VolumeUnit.MILLILITRES)

    val allMigraines: StateFlow<List<MigraineEntry>> = repository.observeAllMigraines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun showPreviousMonth() {
        _month.value = _month.value.minusMonths(1)
    }

    fun showNextMonth() {
        _month.value = _month.value.plusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _month.value = date.withDayOfMonth(1)
    }

    fun setViewMode(mode: HistoryViewMode) {
        _viewMode.value = mode
    }
}
