package com.divyasrikarri.migrainejournal.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.model.FrequencyGrouping
import com.divyasrikarri.migrainejournal.data.model.InsightsData
import com.divyasrikarri.migrainejournal.data.model.InsightsRange
import com.divyasrikarri.migrainejournal.data.repository.InsightsCalculator
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(private val repository: MigraineRepository) : ViewModel() {

    private val _range = MutableStateFlow(InsightsRange.LAST_90)
    val range: StateFlow<InsightsRange> = _range.asStateFlow()

    private val _grouping = MutableStateFlow<FrequencyGrouping?>(null)

    /** Grouping follows the range unless the user has picked one explicitly. */
    val grouping: StateFlow<FrequencyGrouping> = combine(_range, _grouping) { range, override ->
        override ?: defaultGroupingFor(range)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FrequencyGrouping.WEEKLY
    )

    val insights: StateFlow<InsightsData> =
        combine(_range, _grouping) { range, override -> range to (override ?: defaultGroupingFor(range)) }
            .flatMapLatest { (range, grouping) ->
                val to = DateUtils.today()
                val from = to.minusDays((range.days - 1).toLong())
                combine(
                    repository.observeMigrainesBetween(from, to),
                    repository.observeDailyLogsBetween(from, to),
                    repository.observeFoodBetween(from, to)
                ) { migraines, logs, foods ->
                    // The calculator narrows food down to migraine days itself, so this is a
                    // single range query rather than one lookup per day with an attack.
                    InsightsCalculator.compute(
                        migraines = migraines,
                        dailyLogs = logs,
                        foods = foods,
                        from = from,
                        toInclusive = to,
                        grouping = grouping
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsData())

    fun setRange(range: InsightsRange) {
        _range.value = range
        // Reset any manual grouping so the new range gets its sensible default.
        _grouping.value = null
    }

    fun setGrouping(grouping: FrequencyGrouping) {
        _grouping.value = grouping
    }

    private fun defaultGroupingFor(range: InsightsRange): FrequencyGrouping {
        val to = DateUtils.today()
        return InsightsCalculator.defaultGrouping(to.minusDays((range.days - 1).toLong()), to)
    }
}
