package com.fginc.weldo.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.CaptureItemProposal
import com.fginc.weldo.data.model.CaptureProposal
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One-shot outcome of a capture round, consumed by [CaptureBar]. */
sealed interface CaptureEvent {
    data class Single(val draft: ItemDraft) : CaptureEvent
    data class Batch(val proposal: CaptureProposal) : CaptureEvent
    data class Error(val message: String) : CaptureEvent
}

class CaptureViewModel : ViewModel() {
    private val repo = WeldoApp.graph.repository

    private val _allProjects = MutableStateFlow<List<Project>>(emptyList())
    val allProjects: StateFlow<List<Project>> = _allProjects

    private val _event = MutableStateFlow<CaptureEvent?>(null)
    val event: StateFlow<CaptureEvent?> = _event

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun refreshProjects() {
        viewModelScope.launch {
            repo.loadAll().onSuccess { _allProjects.value = it.projects }
        }
    }

    fun classifyText(text: String, contextProjectId: String?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            repo.capture(trimmed)
                .onSuccess { route(it, contextProjectId) }
                .onFailure { _event.value = CaptureEvent.Error("Couldn't reach capture. Try again.") }
            _busy.value = false
        }
    }

    fun classifyImage(base64: String, mime: String, contextProjectId: String?) {
        viewModelScope.launch {
            _busy.value = true
            repo.captureImage(base64, mime)
                .onSuccess { route(it, contextProjectId) }
                .onFailure { _event.value = CaptureEvent.Error("Couldn't read that photo.") }
            _busy.value = false
        }
    }

    private fun route(proposal: CaptureProposal, contextProjectId: String?) {
        val single = proposal.items.size == 1 && proposal.project == null && proposal.existingProjectId == null
        _event.value = if (single) {
            CaptureEvent.Single(proposal.items.first().toDraft(contextProjectId))
        } else {
            CaptureEvent.Batch(proposal)
        }
    }

    fun consume() {
        _event.value = null
    }
}

/** Maps an AI item proposal to an editable [ItemDraft], defaulting the type to Task. */
fun CaptureItemProposal.toDraft(projectId: String?): ItemDraft {
    val t = ItemType.fromWire(type) ?: ItemType.TASK
    return ItemDraft(
        type = t,
        title = title.orEmpty(),
        detail = detail.orEmpty(),
        projectId = projectId,
        dueDate = dueDate,
        madeTo = madeTo,
        waitingOn = waitingOn,
        remindAt = remindAt,
        followUpAt = followUpAt,
        recurrenceRule = recurrenceRule,
    )
}
