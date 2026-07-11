package com.fginc.weldo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.data.remote.WeldoTime
import com.fginc.weldo.ui.capture.ItemFormSheet
import com.fginc.weldo.ui.chipBg
import com.fginc.weldo.ui.chipFg
import com.fginc.weldo.ui.common.AttachmentsSection
import com.fginc.weldo.ui.common.ConfirmDialog
import com.fginc.weldo.ui.common.EmptyState
import com.fginc.weldo.ui.common.LoadingBox
import com.fginc.weldo.ui.icon
import com.fginc.weldo.ui.theme.WeldoTheme
import java.time.LocalDate

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
                onToggleActive = vm::toggleActive,
                onMarkRoutineDone = vm::markRoutineDone,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailBody(
    draft: ItemDraft,
    projectTitle: String?,
    onToggleComplete: () -> Unit,
    onToggleActive: () -> Unit,
    onMarkRoutineDone: () -> Unit,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WeldoTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // --- Light header: tinted icon chip + badge + title + meta chips ---
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(draft.type.chipBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(draft.type.icon, contentDescription = null, tint = draft.type.chipFg, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(draft)
                    if (draft.type == ItemType.ROUTINE) {
                        Spacer(Modifier.weight(1f))
                        Text("Active", style = MaterialTheme.typography.labelLarge, color = c.label)
                        Spacer(Modifier.size(8.dp))
                        Switch(checked = draft.active, onCheckedChange = { onToggleActive() })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    draft.title.ifBlank { "(untitled)" },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChips(draft, projectTitle)
                }
            }
        }

        // --- Description ---
        if (draft.detail.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            SectionCard {
                CardHeader("Description")
                Spacer(Modifier.height(8.dp))
                // A Note's detail is markdown server-side; shown as plain text here.
                Text(draft.detail, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // --- Reminder: prominent coral "Reminds at" ---
        if (draft.type == ItemType.REMINDER && WeldoTime.formatDateTime(draft.remindAt) != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.statCoralBg).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(c.coral),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("REMINDS AT", style = MaterialTheme.typography.labelMedium, color = c.coralFg)
                    Text(
                        WeldoTime.formatDateTime(draft.remindAt) ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // --- Routine: schedule + Mark done ---
        if (draft.type == ItemType.ROUTINE) {
            Spacer(Modifier.height(14.dp))
            SectionCard {
                CardHeader("Schedule")
                Spacer(Modifier.height(6.dp))
                Text(
                    draft.recurrenceRule?.ifBlank { null } ?: "No recurrence set",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onMarkRoutineDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = c.violet),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Mark done")
            }
        }

        // --- Details section ---
        Spacer(Modifier.height(18.dp))
        SectionCard {
            CardHeader("Details")
            Spacer(Modifier.height(4.dp))
            if (draft.type.completable) {
                DetailRow("Status", if (draft.completed) "Completed" else "Open", if (draft.completed) c.muted else c.posFg)
            }
            when (draft.type) {
                ItemType.TASK, ItemType.PROJECT ->
                    DetailRow("Due date", WeldoTime.formatDay(draft.dueDate), if (isDueUrgent(draft.dueDate)) c.coralFg else null)
                ItemType.REMINDER ->
                    DetailRow("Reminds at", WeldoTime.formatDateTime(draft.remindAt), c.coralFg)
                ItemType.ROUTINE -> {
                    DetailRow("State", if (draft.active) "Active" else "Paused", if (draft.active) c.posFg else c.muted)
                    DetailRow("Recurrence", draft.recurrenceRule)
                }
                ItemType.NOTE -> Unit
            }
            if (projectTitle != null) DetailRow("Project", projectTitle)
            DetailRow("Created", WeldoTime.formatDateTime(draft.createdAt))
            DetailRow("Updated", WeldoTime.formatDateTime(draft.updatedAt))
        }

        if (draft.projectId != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { onOpenProject(draft.projectId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open project" + (projectTitle?.let { ": $it" } ?: ""))
            }
        }

        if (draft.type.completable) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onToggleComplete, modifier = Modifier.fillMaxWidth()) {
                Text(if (draft.completed) "Reopen" else "Complete")
            }
        }

        Spacer(Modifier.height(24.dp))
        draft.id?.let { AttachmentsSection(draft.type, it) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TypeBadge(draft: ItemDraft) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(draft.type.chipBg).padding(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Text(draft.type.display.uppercase(), style = MaterialTheme.typography.labelMedium, color = draft.type.chipFg)
    }
}

@Composable
private fun MetaChip(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(11.dp)).background(bg).padding(horizontal = 13.dp, vertical = 7.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun MetaChips(draft: ItemDraft, projectTitle: String?) {
    val c = WeldoTheme.colors
    when (draft.type) {
        ItemType.TASK, ItemType.PROJECT -> dueLabel(draft.dueDate)?.let {
            MetaChip(it, c.coralTintBg, c.coralFg)
        }
        ItemType.REMINDER -> WeldoTime.formatDateTime(draft.remindAt)?.let { MetaChip("Reminds $it", c.coralTintBg, c.coralFg) }
        ItemType.ROUTINE -> {
            draft.recurrenceRule?.ifBlank { null }?.let { MetaChip(it, c.neutralBg, c.neutralFg) }
            if (draft.active) MetaChip("Active", c.posBg, c.posFg)
        }
        else -> Unit
    }
    projectTitle?.let { MetaChip(it, c.neutralBg, c.neutralFg) }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun CardHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun DetailRow(label: String, value: String?, valueColor: Color? = null) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = WeldoTheme.colors.muted)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Due today" / "Overdue" / "Due Jul 12" for a yyyy-MM-dd calendar day. */
private fun dueLabel(day: String?): String? {
    val d = parseDay(day) ?: return null
    return when {
        d.isEqual(LocalDate.now()) -> "Due today"
        d.isBefore(LocalDate.now()) -> "Overdue"
        else -> "Due ${WeldoTime.formatDay(day)}"
    }
}

private fun isDueUrgent(day: String?): Boolean {
    val d = parseDay(day) ?: return false
    return !d.isAfter(LocalDate.now())
}

private fun parseDay(day: String?): LocalDate? {
    if (day.isNullOrBlank()) return null
    return try {
        LocalDate.parse(day.substring(0, minOf(10, day.length)))
    } catch (_: Exception) {
        null
    }
}
