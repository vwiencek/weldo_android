package com.fginc.weldo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.remote.WeldoTime
import com.fginc.weldo.ui.capture.ItemFormSheet
import com.fginc.weldo.ui.color
import com.fginc.weldo.ui.common.AttachmentsSection
import com.fginc.weldo.ui.common.ConfirmDialog
import com.fginc.weldo.ui.common.EmptyState
import com.fginc.weldo.ui.common.LoadingBox
import com.fginc.weldo.ui.icon
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    type: ItemType,
    id: String,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val vm: ItemDetailViewModel = viewModel()
    val state by vm.state.collectAsState()
    val projects by vm.projects.collectAsState()

    LaunchedEffect(type, id) { vm.load(type, id) }

    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val draft = (state as? DetailUiState.Loaded)?.draft

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(draft?.type?.display ?: type.display) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (draft != null) {
                        IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; showDelete = true })
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            is DetailUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is DetailUiState.Error -> EmptyState("Couldn't load", s.message, Modifier.padding(padding))
            is DetailUiState.Loaded -> DetailBody(
                draft = s.draft,
                projectTitle = projects.firstOrNull { it.id == s.draft.projectId }?.title,
                onToggleComplete = vm::toggleComplete,
                onOpenProject = onOpenProject,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showEdit && draft != null) {
        ItemFormSheet(
            initial = draft,
            allProjects = projects,
            onDismiss = { showEdit = false },
            onSaved = { showEdit = false; vm.refresh() },
        )
    }

    if (showDelete && draft != null) {
        val isProject = draft.type == ItemType.PROJECT
        ConfirmDialog(
            title = "Delete ${draft.type.display.lowercase()}?",
            text = if (isProject) "This deletes the project and everything inside it." else "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { vm.delete(onDone = onBack) },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun DetailBody(
    draft: ItemDraft,
    projectTitle: String?,
    onToggleComplete: () -> Unit,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Type badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(draft.type.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(draft.type.icon, contentDescription = null, tint = draft.type.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(draft.type.display, style = MaterialTheme.typography.labelLarge, color = draft.type.color)
        }

        Spacer(Modifier.height(12.dp))
        Text(draft.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

        if (draft.detail.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            // NOTE: a Note's detail is markdown on the server; rendered as plain text here (no MD renderer ported).
            Text(draft.detail, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Type-specific read-only fields
        when (draft.type) {
            ItemType.TASK -> LabeledRow("Due", WeldoTime.formatDay(draft.dueDate))
            ItemType.PROJECT -> LabeledRow("Due", WeldoTime.formatDay(draft.dueDate))
            ItemType.COMMITMENT -> {
                LabeledRow("Made to", draft.madeTo)
                LabeledRow("Due", WeldoTime.formatDay(draft.dueDate))
            }
            ItemType.REMINDER -> LabeledRow("Remind at", WeldoTime.formatDateTime(draft.remindAt))
            ItemType.WAITING_FOR -> {
                LabeledRow("Waiting on", draft.waitingOn)
                LabeledRow("Follow up", WeldoTime.formatDateTime(draft.followUpAt))
            }
            ItemType.ROUTINE -> {
                LabeledRow("Recurrence", draft.recurrenceRule)
                LabeledRow("Active", if (draft.active) "Yes" else "No")
            }
            ItemType.SUGGESTION -> LabeledRow("Status", draft.status)
            ItemType.IDEA, ItemType.NOTE -> Unit
        }

        if (draft.projectId != null) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { onOpenProject(draft.projectId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open project" + (projectTitle?.let { ": $it" } ?: ""))
            }
        }

        if (draft.type.completable) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onToggleComplete, modifier = Modifier.fillMaxWidth()) {
                Text(if (draft.completed) "Reopen" else "Mark complete")
            }
        }

        Spacer(Modifier.height(24.dp))
        draft.id?.let { AttachmentsSection(draft.type, it) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun LabeledRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
