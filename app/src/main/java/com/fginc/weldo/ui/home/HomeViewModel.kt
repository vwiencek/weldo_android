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
import java.time.LocalDate
import java.time.ZoneId

/** The agenda buckets, in display order. `LATER` also holds undated items. */
enum class DueBucket(val label: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    WEEK("This week"),
    MONTH("This month"),
    LATER("Later / no date"),
}

/** One non-empty due bucket with its rows (already sorted). */
data class AgendaBucket(val kind: DueBucket, val items: List<AnyItem>)

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

    /**
     * The Home agenda (web parity with `core/due.ts` + features/agenda): open
     * task / project / reminder items grouped by due date into
     * Overdue → Today → Tomorrow → This week → This month → Later, empty buckets
     * dropped, undated items in Later. Cross-project (all levels).
     */
    fun agenda(): List<AgendaBucket> {
        val all = _state.value.allItems
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun startOf(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val startToday = startOf(today)
        val startTomorrow = startOf(today.plusDays(1))
        val startDayAfter = startOf(today.plusDays(2))
        // Monday-based: next Monday 00:00 (dayOfWeek.value is Mon=1 … Sun=7).
        val nextWeek = startOf(today.plusDays((8 - today.dayOfWeek.value).toLong()))
        val nextMonth = startOf(today.withDayOfMonth(1).plusMonths(1))

        fun bucketOf(due: Long?): DueBucket = when {
            due == null -> DueBucket.LATER
            due < startToday -> DueBucket.OVERDUE
            due < startTomorrow -> DueBucket.TODAY
            due < startDayAfter -> DueBucket.TOMORROW
            due < nextWeek -> DueBucket.WEEK
            due < nextMonth -> DueBucket.MONTH
            else -> DueBucket.LATER
        }

        data class Entry(val item: AnyItem, val due: Long?)
        val entries = buildList {
            all.tasks.filterNot { it.completed }.forEach { add(Entry(it.toAny(), it.dueDate.toDueMillis())) }
            all.projects.filterNot { it.completed }.forEach { add(Entry(it.toAny(), it.dueDate.toDueMillis())) }
            all.reminders.filterNot { it.completed }.forEach { add(Entry(it.toAny(), it.remindAt.toDueMillis())) }
        }
        val byBucket = entries.groupBy { bucketOf(it.due) }
        return DueBucket.entries.mapNotNull { kind ->
            val rows = byBucket[kind]
                ?.sortedWith(
                    // dated soonest-first; undated last, then newest updated first
                    compareBy<Entry> { it.due == null }
                        .thenBy { it.due ?: Long.MAX_VALUE }
                        .thenByDescending { WeldoTime.epochMillis(it.item.updatedAt) }
                )
                ?.map { it.item }
            if (rows.isNullOrEmpty()) null else AgendaBucket(kind, rows)
        }
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

/** A due-date/instant string → epoch millis, or null when absent/unparseable. */
private fun String?.toDueMillis(): Long? =
    this?.takeIf { it.isNotBlank() }?.let { WeldoTime.epochMillis(it) }?.takeIf { it > 0 }

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
