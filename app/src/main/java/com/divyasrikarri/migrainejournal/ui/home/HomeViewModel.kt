package com.divyasrikarri.migrainejournal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.HomeStats
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val stats: HomeStats = HomeStats(),
    val recentMigraines: List<MigraineEntry> = emptyList(),
    val checkInDone: Boolean = false,
    val ongoingMigraine: MigraineEntry? = null
)

class HomeViewModel(repository: MigraineRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeHomeStats(),
        repository.observeRecentMigraines(),
        repository.observeDailyLog(DateUtils.today())
    ) { stats, recent, todayLog ->
        HomeUiState(
            stats = stats,
            recentMigraines = recent,
            checkInDone = todayLog?.hasContent == true,
            ongoingMigraine = recent.firstOrNull { it.isOngoing }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
