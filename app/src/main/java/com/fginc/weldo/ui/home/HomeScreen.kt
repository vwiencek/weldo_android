package com.fginc.weldo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.model.AnyItem
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.ui.color
import com.fginc.weldo.ui.common.ConfirmDialog
import com.fginc.weldo.ui.common.EmptyState
import com.fginc.weldo.ui.common.ItemRow
import com.fginc.weldo.ui.icon
import com.fginc.weldo.ui.theme.WeldoTheme

/**
 * The Home tab — web parity with the agenda landing page: open task / project /
 * reminder items bucketed by due date (Overdue → Today → Tomorrow → This week →
 * This month → Later, empty buckets dropped). The full browse list is the Items
 * tab; stats are the Stats tab; capture lives in the bottom accessory. Rendered
 * inside [com.fginc.weldo.ui.MainShell]'s scaffold, so it takes [contentPadding].
 */
@Composable
fun AgendaScreen(
    vm: HomeViewModel,
    contentPadding: PaddingValues,
    onOpenItem: (ItemType, String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val buckets = vm.agenda()
    val overdue = buckets.firstOrNull { it.kind == DueBucket.OVERDUE }?.items?.size ?: 0
    val today = buckets.firstOrNull { it.kind == DueBucket.TODAY }?.items?.size ?: 0

    Column(Modifier.padding(contentPadding).fillMaxSize()) {
        GreetingHero(overdue = overdue, today = today)

        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.load() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (buckets.isEmpty() && !state.loading) {
                EmptyState(
                    title = "Nothing on your plate.",
                    subtitle = state.error ?: "Capture something below to get started.",
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    buckets.forEach { bucket ->
                        item(key = "head:${bucket.kind.name}") {
                            BucketHeader(kind = bucket.kind, count = bucket.items.size)
                        }
                        items(bucket.items, key = { "${bucket.kind.name}:${it.type.wire}:${it.id}" }) { item ->
                            HomeRow(
                                item = item,
                                onClick = { if (item.type == ItemType.PROJECT) onOpenProject(item.id) else onOpenItem(item.type, item.id) },
                                onToggle = { vm.toggleComplete(item) },
                                onDelete = { vm.delete(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The "All items" tab — the browse list: live search, per-type filter chips with
 * counts, and the mixed list (top-level by default; searching/filtering reveals
 * all levels). Content-only; takes [contentPadding] from the shell.
 */
@Composable
fun ItemsScreen(
    vm: HomeViewModel,
    contentPadding: PaddingValues,
    onOpenItem: (ItemType, String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val items = vm.visibleItems()
    val counts = vm.counts()

    Column(Modifier.padding(contentPadding).fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.setQuery(it) },
            label = { Text("Search") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotBlank()) {
                { IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear") } }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ItemType.entries.filter { (counts[it] ?: 0) > 0 }.forEach { t ->
                FilterChip(
                    selected = state.activeFilter == t,
                    onClick = { vm.setFilter(if (state.activeFilter == t) null else t) },
                    label = { Text("${t.display} ${counts[t]}") },
                    leadingIcon = { Icon(t.icon, contentDescription = null, tint = t.color, modifier = Modifier.size(18.dp)) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.load() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                items.isEmpty() && !state.loading ->
                    EmptyState("Nothing here yet", state.error ?: "Capture something below to get started.")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.type.wire + ":" + it.id }) { item ->
                        HomeRow(
                            item = item,
                            onClick = { if (item.type == ItemType.PROJECT) onOpenProject(item.id) else onOpenItem(item.type, item.id) },
                            onToggle = { vm.toggleComplete(item) },
                            onDelete = { vm.delete(item) },
                        )
                    }
                }
            }
        }
    }
}

/** Bucket section header: tinted icon + label + count (web's bucket-head). */
@Composable
private fun BucketHeader(kind: DueBucket, count: Int) {
    val c = WeldoTheme.colors
    val icon: ImageVector = when (kind) {
        DueBucket.OVERDUE -> Icons.Filled.Warning
        DueBucket.TODAY -> Icons.Filled.Today
        DueBucket.TOMORROW -> Icons.Filled.WbSunny
        DueBucket.WEEK -> Icons.Filled.DateRange
        DueBucket.MONTH -> Icons.Filled.CalendarMonth
        DueBucket.LATER -> Icons.Filled.Schedule
    }
    val tint = when (kind) {
        DueBucket.OVERDUE -> c.coralFg
        DueBucket.TODAY -> c.violet
        else -> c.muted
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(kind.label, style = MaterialTheme.typography.labelLarge, color = c.label)
        Spacer(Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = c.muted)
    }
}

/**
 * Violet gradient greeting card, agenda copy (web parity): overdue / due-today
 * counts highlighted in coral, falling back to the all-clear line.
 */
@Composable
private fun GreetingHero(overdue: Int, today: Int) {
    val c = WeldoTheme.colors
    val hour = java.time.LocalTime.now().hour
    val part = when { hour < 12 -> "morning"; hour < 18 -> "afternoon"; else -> "evening" }
    val message = buildAnnotatedString {
        fun hl(n: Int) = withStyle(SpanStyle(color = c.heroHighlight)) { append("$n") }
        when {
            overdue > 0 -> {
                hl(overdue); append(" overdue and "); hl(today); append(" due today.")
            }
            today > 0 -> {
                append("You have "); hl(today); append(" to do today.")
            }
            else -> append("Nothing due today — you're on top of things.")
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.heroGradStart, c.heroGradEnd)))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            "Good $part",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.size(4.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

/** A list row plus its overflow menu (complete/reopen + delete with confirm). */
@Composable
private fun HomeRow(
    item: AnyItem,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ItemRow(item = item, onClick = onClick, modifier = Modifier.weight(1f).clickable { onClick() })
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (item.type.completable) {
                    DropdownMenuItem(
                        text = { Text(if (item.completed) "Reopen" else "Complete") },
                        onClick = { menu = false; onToggle() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menu = false; confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete ${item.type.display}?",
            text = if (item.type == ItemType.PROJECT) "This deletes the project and everything inside it." else "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}
