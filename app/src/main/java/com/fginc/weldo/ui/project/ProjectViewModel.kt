package com.fginc.weldo.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.AnyItem
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Project
import com.fginc.weldo.data.toAnyItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProjectUiState(
    val loading: Boolean = true,
    val title: String = "",
    val subprojects: List<Project> = emptyList(),
    val items: List<AnyItem> = emptyList(),
    val allProjects: List<Project> = emptyList(),
    val projectDraft: ItemDraft? = null,
    val error: String? = null,
)

class ProjectViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository

    private val _state = MutableStateFlow(ProjectUiState())
    val state: StateFlow<ProjectUiState> = _state

    private var projectId: String? = null

    fun load(id: String) {
        projectId = id
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.projectItems(id)
                .onSuccess { pi ->
                    _state.value = _state.value.copy(
                        loading = false,
                        subprojects = pi.subprojects,
                        items = pi.toAnyItems(),
                    )
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Couldn't load project.") }
            repo.load(ItemType.PROJECT, id).onSuccess {
                _state.value = _state.value.copy(title = it.title, projectDraft = it)
            }
            repo.loadAll().onSuccess { _state.value = _state.value.copy(allProjects = it.projects) }
        }
    }

    fun refresh() = projectId?.let { load(it) }

    fun toggleComplete(item: AnyItem) {
        viewModelScope.launch {
            repo.setCompleted(item.type, item.id, !item.completed).onSuccess { refresh() }
        }
    }

    fun deleteItem(item: AnyItem) {
        viewModelScope.launch { repo.delete(item.type, item.id).onSuccess { refresh() } }
    }

    fun deleteProject(onDone: () -> Unit) {
        val id = projectId ?: return
        viewModelScope.launch { repo.delete(ItemType.PROJECT, id).onSuccess { onDone() } }
    }
}
