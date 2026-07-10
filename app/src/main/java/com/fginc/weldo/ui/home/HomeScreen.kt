package com.fginc.weldo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.model.AnyItem
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.ui.capture.CaptureBar
import com.fginc.weldo.ui.color
import com.fginc.weldo.ui.common.ConfirmDialog
import com.fginc.weldo.ui.common.EmptyState
import com.fginc.weldo.ui.common.ItemRow
import com.fginc.weldo.ui.icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenItem: (ItemType, String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    val items = vm.visibleItems()
    val counts = vm.counts()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weldo") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = { CaptureBar(projectId = null, onChanged = { vm.load() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.query.isBlank()) GreetingHero(state.stats)

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

            if (state.query.isBlank()) StatsStrip(state.stats)

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
}

/** Violet gradient greeting card: "You have N open items and a M-day streak." */
@Composable
private fun GreetingHero(stats: com.fginc.weldo.data.model.Statistics?) {
    val c = com.fginc.weldo.ui.theme.WeldoTheme.colors
    val hour = java.time.LocalTime.now().hour
    val part = when { hour < 12 -> "morning"; hour < 18 -> "afternoon"; else -> "evening" }
    val open = stats?.openItems ?: 0
    val streak = stats?.streakDays ?: 0
    val message = androidx.compose.ui.text.buildAnnotatedString {
        append("You have ")
        withStyle(androidx.compose.ui.text.SpanStyle(color = c.heroHighlight)) {
            append("$open open ${if (open == 1L) "item" else "items"}")
        }
        append(" and a ")
        withStyle(androidx.compose.ui.text.SpanStyle(color = c.heroHighlight)) { append("$streak-day streak") }
        append(".")
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
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text(message, style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White)
    }
}

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
