package com.fginc.weldo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.AllItems
import com.fginc.weldo.data.model.AnyItem
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Statistics
import com.fginc.weldo.data.remote.WeldoTime
import com.fginc.weldo.data.toAny
import com.fginc.weldo.data.toAnyItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val allItems: AllItems = AllItems(),
    val stats: Statistics? = null,
    val query: String = "",
    val activeFilter: ItemType? = null,
)

class HomeViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository
    private val session = WeldoApp.graph.session

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.loadAll()
                .onSuccess { all -> _state.update { it.copy(allItems = all, loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
            repo.statistics(session.statsPeriod.value)
                .onSuccess { s -> _state.update { it.copy(stats = s) } }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setFilter(type: ItemType?) = _state.update { it.copy(activeFilter = type) }

    /** Counts per type over the full flattened list (all levels) — drives the filter chips. */
    fun counts(): Map<ItemType, Int> = _state.value.allItems.toAnyItems().groupingBy { it.type }.eachCount()

    /**
     * Default view is top-level only; searching/filtering reveals all levels.
     * Top-level = non-project items with no projectId + projects with no parentId
     * (Project.toAny() nulls projectId, so top-level projects are taken from the raw list).
     */
    fun visibleItems(): List<AnyItem> {
        val s = _state.value
        val searching = s.query.isNotBlank() || s.activeFilter != null
        val list = if (searching) {
            s.allItems.toAnyItems()
        } else {
            val nonProject = s.allItems.toAnyItems().filter { it.type != ItemType.PROJECT && it.projectId == null }
            val topProjects = s.allItems.projects.filter { it.parentId == null }.map { it.toAny() }
            nonProject + topProjects
        }
        return list
            .filter { item ->
                (s.activeFilter == null || item.type == s.activeFilter) &&
                    (s.query.isBlank() || item.title.contains(s.query, true) || item.detail.contains(s.query, true))
            }
            .sortedByDescending { WeldoTime.epochMillis(it.updatedAt) }
    }

    fun toggleComplete(item: AnyItem) {
        if (!item.type.completable) return
        val target = !item.completed
        _state.update { it.copy(allItems = it.allItems.withCompleted(item.type, item.id, target)) }
        viewModelScope.launch { repo.setCompleted(item.type, item.id, target).onFailure { load() } }
    }

    fun delete(item: AnyItem) {
        _state.update { it.copy(allItems = it.allItems.without(item.type, item.id)) }
        viewModelScope.launch { repo.delete(item.type, item.id).onFailure { load() } }
    }
}

private fun AllItems.withCompleted(type: ItemType, id: String, completed: Boolean): AllItems = when (type) {
    ItemType.TASK -> copy(tasks = tasks.map { if (it.id == id) it.copy(completed = completed) else it })
    ItemType.PROJECT -> copy(projects = projects.map { if (it.id == id) it.copy(completed = completed) else it })
    ItemType.COMMITMENT -> copy(commitments = commitments.map { if (it.id == id) it.copy(completed = completed) else it })
    ItemType.REMINDER -> copy(reminders = reminders.map { if (it.id == id) it.copy(completed = completed) else it })
    ItemType.WAITING_FOR -> copy(waitingFor = waitingFor.map { if (it.id == id) it.copy(completed = completed) else it })
    else -> this
}

private fun AllItems.without(type: ItemType, id: String): AllItems = when (type) {
    ItemType.TASK -> copy(tasks = tasks.filterNot { it.id == id })
    ItemType.PROJECT -> copy(projects = projects.filterNot { it.id == id })
    ItemType.COMMITMENT -> copy(commitments = commitments.filterNot { it.id == id })
    ItemType.REMINDER -> copy(reminders = reminders.filterNot { it.id == id })
    ItemType.WAITING_FOR -> copy(waitingFor = waitingFor.filterNot { it.id == id })
    ItemType.IDEA -> copy(ideas = ideas.filterNot { it.id == id })
    ItemType.ROUTINE -> copy(routines = routines.filterNot { it.id == id })
    ItemType.SUGGESTION -> copy(suggestions = suggestions.filterNot { it.id == id })
    ItemType.NOTE -> copy(notes = notes.filterNot { it.id == id })
}
