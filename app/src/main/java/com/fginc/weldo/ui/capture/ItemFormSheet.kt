package com.fginc.weldo.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.model.Project
import com.fginc.weldo.data.remote.WeldoTime
import kotlinx.coroutines.launch

/**
 * Create/edit form for any of the nine types. Blank [initial] = "new"; a loaded draft = "edit"
 * (type locked). Saves via the repository's unified create/update path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormSheet(
    initial: ItemDraft,
    allProjects: List<Project>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repo = WeldoApp.graph.repository
    val scope = rememberCoroutineScope()

    var draft by remember { mutableStateOf(initial) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            Text(
                if (initial.id == null) "New ${draft.type.display}" else "Edit ${draft.type.display}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))

            if (initial.id == null) {
                TypeDropdown(draft.type) { draft = draft.copy(type = it) }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = draft.title, onValueChange = { draft = draft.copy(title = it) },
                label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.detail, onValueChange = { draft = draft.copy(detail = it) },
                label = { Text("Details") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            )
            Spacer(Modifier.height(8.dp))

            ProjectDropdown(
                if (draft.type == ItemType.PROJECT) "Parent project" else "Project",
                draft.projectId, allProjects,
            ) { draft = draft.copy(projectId = it) }
            Spacer(Modifier.height(8.dp))

            TypeSpecificFields(draft) { draft = it }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = !saving && draft.title.isNotBlank(),
                    onClick = {
                        scope.launch {
                            saving = true; error = null
                            val result = if (draft.id == null) repo.create(draft) else repo.update(draft)
                            saving = false
                            result.onSuccess { onSaved(); onDismiss() }
                                .onFailure { error = it.message ?: "Couldn't save" }
                        }
                    },
                ) { Text(if (saving) "Saving…" else "Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeDropdown(selected: ItemType, onSelect: (ItemType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.display, onValueChange = {}, readOnly = true, label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemType.entries.forEach { t ->
                DropdownMenuItem(text = { Text(t.display) }, onClick = { onSelect(t); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDropdown(label: String, selectedId: String?, projects: List<Project>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = projects.firstOrNull { it.id == selectedId }?.title ?: "None"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedTitle, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            projects.forEach { p ->
                DropdownMenuItem(text = { Text(p.title.ifBlank { "(untitled)" }) }, onClick = { onSelect(p.id); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("PENDING", "ACCEPTED", "REJECTED")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { onSelect(s); expanded = false }) }
        }
    }
}

@Composable
private fun TypeSpecificFields(draft: ItemDraft, onChange: (ItemDraft) -> Unit) {
    when (draft.type) {
        ItemType.TASK, ItemType.PROJECT ->
            DateField("Due date", draft.dueDate, isInstant = false) { onChange(draft.copy(dueDate = it)) }
        ItemType.COMMITMENT -> {
            OutlinedTextField(
                value = draft.madeTo.orEmpty(), onValueChange = { onChange(draft.copy(madeTo = it)) },
                label = { Text("Made to") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DateField("Due date", draft.dueDate, isInstant = false) { onChange(draft.copy(dueDate = it)) }
        }
        ItemType.REMINDER ->
            DateField("Remind at", draft.remindAt, isInstant = true) { onChange(draft.copy(remindAt = it)) }
        ItemType.WAITING_FOR -> {
            OutlinedTextField(
                value = draft.waitingOn.orEmpty(), onValueChange = { onChange(draft.copy(waitingOn = it)) },
                label = { Text("Waiting on") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            DateField("Follow up at", draft.followUpAt, isInstant = true) { onChange(draft.copy(followUpAt = it)) }
        }
        ItemType.ROUTINE -> {
            OutlinedTextField(
                value = draft.recurrenceRule.orEmpty(), onValueChange = { onChange(draft.copy(recurrenceRule = it)) },
                label = { Text("Recurrence (cron: or RRULE:)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Active", modifier = Modifier.weight(1f))
                Switch(checked = draft.active, onCheckedChange = { onChange(draft.copy(active = it)) })
            }
        }
        ItemType.SUGGESTION -> StatusDropdown(draft.status) { onChange(draft.copy(status = it)) }
        ItemType.IDEA, ItemType.NOTE -> {}
    }
}

/**
 * Toggleable date field. For an instant field the picked calendar day becomes a start-of-day
 * ISO instant (time-of-day is not captured — a deliberate simplification); otherwise an ISO day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String?, isInstant: Boolean, onChange: (String?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val display = if (isInstant) WeldoTime.formatDateTime(value) else WeldoTime.formatDay(value)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
            Text(if (display != null) "$label: $display" else "Set $label")
        }
        if (value != null) {
            TextButton(onClick = { onChange(null) }) { Text("Clear") }
        }
    }
    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onChange(if (isInstant) WeldoTime.isoInstant(millis) else WeldoTime.isoDate(millis))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
