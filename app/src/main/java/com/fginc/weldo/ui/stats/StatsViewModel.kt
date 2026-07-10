package com.fginc.weldo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.Statistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val period: String = "month",
    val stats: Statistics? = null,
)

class StatsViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository
    private val session = WeldoApp.graph.session

    private val _state = MutableStateFlow(StatsUiState(period = session.statsPeriod.value))
    val state: StateFlow<StatsUiState> = _state

    fun load(period: String = _state.value.period) {
        _state.update { it.copy(loading = true, error = null, period = period) }
        viewModelScope.launch {
            repo.statistics(period)
                .onSuccess { s -> _state.update { it.copy(loading = false, stats = s) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message ?: "Couldn't load statistics.") } }
        }
    }
}
