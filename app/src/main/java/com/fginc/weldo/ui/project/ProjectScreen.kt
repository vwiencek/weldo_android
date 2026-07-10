package com.fginc.weldo.ui.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.model.AnyItem
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.toAny
import com.fginc.weldo.ui.capture.CaptureBar
import com.fginc.weldo.ui.capture.ItemFormSheet
import com.fginc.weldo.ui.common.ConfirmDialog
import com.fginc.weldo.ui.common.EmptyState
import com.fginc.weldo.ui.common.ItemRow
import com.fginc.weldo.ui.common.LoadingBox
import com.fginc.weldo.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    projectId: String,
    onBack: () -> Unit,
    onOpenItem: (ItemType, String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val vm: ProjectViewModel = viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(projectId) { vm.load(projectId) }

    var menuOpen by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Project" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Edit project") }, onClick = { menuOpen = false; showEdit = true }, enabled = state.projectDraft != null)
                        DropdownMenuItem(text = { Text("Delete project") }, onClick = { menuOpen = false; showDelete = true })
                    }
                },
            )
        },
        bottomBar = { CaptureBar(projectId = projectId, onChanged = { vm.refresh() }) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> EmptyState("Couldn't load", state.error, Modifier.padding(padding))
            state.subprojects.isEmpty() && state.items.isEmpty() ->
                EmptyState("Nothing here yet", "Use the bar below to add items to this project.", Modifier.padding(padding))
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                if (state.subprojects.isNotEmpty()) {
                    item { SectionHeader("Subprojects") }
                    items(state.subprojects, key = { "p_${it.id}" }) { sub ->
                        ItemRow(sub.toAny(), onClick = { sub.id?.let(onOpenProject) })
                    }
                }
                if (state.items.isNotEmpty()) {
                    item { SectionHeader("Items") }
                    items(state.items, key = { "${it.type.wire}_${it.id}" }) { item ->
                        ProjectItemRow(
                            item = item,
                            onClick = { onOpenItem(item.type, item.id) },
                            onToggle = { vm.toggleComplete(item) },
                            onDelete = { vm.deleteItem(item) },
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        state.projectDraft?.let { draft ->
            ItemFormSheet(
                initial = draft,
                allProjects = state.allProjects,
                onDismiss = { showEdit = false },
                onSaved = { showEdit = false; vm.refresh() },
            )
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete project?",
            text = "This deletes the project and everything inside it.",
            confirmLabel = "Delete",
            onConfirm = { vm.deleteProject(onDone = onBack) },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ProjectItemRow(
    item: AnyItem,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        ItemRow(item, onClick = onClick, modifier = Modifier.fillMaxWidth().padding(end = 40.dp))
        Box(Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (item.type.completable) {
                    DropdownMenuItem(
                        text = { Text(if (item.completed) "Reopen" else "Complete") },
                        onClick = { menuOpen = false; onToggle() },
                    )
                }
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
            }
        }
    }
    if (confirmDelete) {
        val isProject = item.type == ItemType.PROJECT
        ConfirmDialog(
            title = "Delete ${item.type.display.lowercase()}?",
            text = if (isProject) "This deletes the project and everything inside it." else "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}
