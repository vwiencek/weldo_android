package com.fginc.weldo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val draft: ItemDraft) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class ItemDetailViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private var current: Pair<ItemType, String>? = null

    fun load(type: ItemType, id: String) {
        current = type to id
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            repo.load(type, id)
                .onSuccess { _state.value = DetailUiState.Loaded(it) }
                .onFailure { _state.value = DetailUiState.Error(it.message ?: "Couldn't load this item.") }
            repo.loadAll().onSuccess { _projects.value = it.projects }
        }
    }

    fun refresh() = current?.let { load(it.first, it.second) }

    fun toggleComplete() {
        val draft = (_state.value as? DetailUiState.Loaded)?.draft ?: return
        val id = draft.id ?: return
        viewModelScope.launch {
            repo.setCompleted(draft.type, id, !draft.completed).onSuccess { refresh() }
        }
    }

    /** Routine: flip the Active flag via the full-replace update. */
    fun toggleActive() {
        val draft = (_state.value as? DetailUiState.Loaded)?.draft ?: return
        if (draft.type != ItemType.ROUTINE) return
        viewModelScope.launch {
            repo.update(draft.copy(active = !draft.active)).onSuccess { refresh() }
        }
    }

    /** Routine: mark done for today (best-effort; mirrors the web action). */
    fun markRoutineDone() {
        val draft = (_state.value as? DetailUiState.Loaded)?.draft ?: return
        val id = draft.id ?: return
        if (draft.type != ItemType.ROUTINE) return
        viewModelScope.launch {
            repo.setCompleted(draft.type, id, true).onSuccess { refresh() }
        }
    }

    fun delete(onDone: () -> Unit) {
        val draft = (_state.value as? DetailUiState.Loaded)?.draft ?: return
        val id = draft.id ?: return
        viewModelScope.launch {
            repo.delete(draft.type, id).onSuccess { onDone() }
        }
    }
}
