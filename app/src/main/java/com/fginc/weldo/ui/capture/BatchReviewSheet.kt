package com.fginc.weldo.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.BatchCreateRequest
import com.fginc.weldo.data.model.BatchItemRequest
import com.fginc.weldo.data.model.BatchProjectRequest
import com.fginc.weldo.data.model.CaptureItemProposal
import com.fginc.weldo.data.model.CaptureProposal
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Project
import com.fginc.weldo.ui.color
import com.fginc.weldo.ui.icon
import kotlinx.coroutines.launch

private data class BatchRow(val type: ItemType, val title: String, val source: CaptureItemProposal)

private sealed interface Destination {
    data object None : Destination
    data class New(val title: String) : Destination
    data class Existing(val projectId: String) : Destination
}

/**
 * Review sheet for a multi-item capture: pick a destination project (none / new / existing),
 * edit/remove each proposed item, then create them all atomically via /items/batch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchReviewSheet(
    proposal: CaptureProposal,
    projectId: String?,
    allProjects: List<Project>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repo = WeldoApp.graph.repository
    val scope = rememberCoroutineScope()

    val rows = remember {
        mutableStateListOf<BatchRow>().apply {
            proposal.items.forEach { add(BatchRow(ItemType.fromWire(it.type) ?: ItemType.TASK, it.title.orEmpty(), it)) }
        }
    }
    var destination by remember {
        mutableStateOf(
            when {
                proposal.existingProjectId != null -> Destination.Existing(proposal.existingProjectId)
                proposal.project != null -> Destination.New(proposal.project.title.orEmpty())
                else -> Destination.None
            },
        )
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            Text(
                "Review ${rows.size} item${if (rows.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))

            DestinationPicker(destination, proposal, allProjects) { destination = it }
            Spacer(Modifier.height(12.dp))

            rows.forEachIndexed { index, row ->
                BatchItemRow(
                    row = row,
                    onTitle = { rows[index] = row.copy(title = it) },
                    onType = { rows[index] = row.copy(type = it) },
                    onRemove = { rows.removeAt(index) },
                )
                HorizontalDivider()
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !saving && rows.isNotEmpty(),
                    onClick = {
                        scope.launch {
                            saving = true; error = null
                            val result = repo.createBatch(buildRequest(rows, destination, projectId, proposal))
                            saving = false
                            result.onSuccess { onSaved(); onDismiss() }
                                .onFailure { error = it.message ?: "Couldn't save" }
                        }
                    },
                ) { Text(if (saving) "Saving…" else "Save all") }
            }
        }
    }
}

private fun buildRequest(
    rows: List<BatchRow>,
    dest: Destination,
    contextProjectId: String?,
    proposal: CaptureProposal,
): BatchCreateRequest {
    val items = rows.filter { it.type != ItemType.PROJECT }.map { r ->
        BatchItemRequest(
            type = r.type.wire,
            title = r.title,
            detail = r.source.detail,
            madeTo = r.source.madeTo,
            waitingOn = r.source.waitingOn,
            dueDate = r.source.dueDate,
            remindAt = r.source.remindAt,
            followUpAt = r.source.followUpAt,
            recurrenceRule = r.source.recurrenceRule,
        )
    }
    return when (dest) {
        is Destination.New -> BatchCreateRequest(
            contextProjectId = contextProjectId,
            project = BatchProjectRequest(title = dest.title, detail = proposal.project?.detail, dueDate = proposal.project?.dueDate),
            items = items,
        )
        is Destination.Existing -> BatchCreateRequest(contextProjectId = dest.projectId, project = null, items = items)
        Destination.None -> BatchCreateRequest(contextProjectId = contextProjectId, project = null, items = items)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationPicker(
    current: Destination,
    proposal: CaptureProposal,
    allProjects: List<Project>,
    onSelect: (Destination) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (current) {
        Destination.None -> "No project"
        is Destination.New -> "New project: ${current.title.ifBlank { "(untitled)" }}"
        is Destination.Existing -> allProjects.firstOrNull { it.id == current.projectId }?.title ?: "Selected project"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label, onValueChange = {}, readOnly = true, label = { Text("Add to") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("No project") }, onClick = { onSelect(Destination.None); expanded = false })
            DropdownMenuItem(text = { Text("New project…") }, onClick = { onSelect(Destination.New(proposal.project?.title ?: "")); expanded = false })
            allProjects.forEach { p ->
                DropdownMenuItem(text = { Text(p.title.ifBlank { "(untitled)" }) }, onClick = { onSelect(Destination.Existing(p.id ?: "")); expanded = false })
            }
        }
    }
    if (current is Destination.New) {
        OutlinedTextField(
            value = current.title, onValueChange = { onSelect(Destination.New(it)) },
            label = { Text("New project name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchItemRow(row: BatchRow, onTitle: (String) -> Unit, onType: (ItemType) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(row.type.icon, contentDescription = null, tint = row.type.color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            OutlinedTextField(value = row.title, onValueChange = onTitle, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            TypeMenu(row.type, onType)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeMenu(selected: ItemType, onSelect: (ItemType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.display, onValueChange = {}, readOnly = true, label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemType.entries.filter { it != ItemType.PROJECT }.forEach { t ->
                DropdownMenuItem(text = { Text(t.display) }, onClick = { onSelect(t); expanded = false })
            }
        }
    }
}
