package com.fginc.weldo.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.model.Statistics
import com.fginc.weldo.data.remote.WeldoTime
import com.fginc.weldo.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding -> StatsBody(padding) }
}

/** Stats content for the Stats tab (no app bar); the shell supplies [contentPadding]. */
@Composable
fun StatsPane(contentPadding: PaddingValues) = StatsBody(contentPadding)

@Composable
private fun StatsBody(padding: PaddingValues) {
    val vm: StatsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("week" to "Week", "month" to "Month", "year" to "Year").forEach { (value, label) ->
                FilterChip(selected = state.period == value, onClick = { vm.load(value) }, label = { Text(label) })
            }
        }

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { LoadingBox() }
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            else -> StatsGrid(state.stats)
        }
    }
}

@Composable
private fun StatsGrid(s: Statistics?) {
    val stats = s ?: Statistics()
    val avg = stats.avgCompletionHours?.let { String.format("%.1f", it) } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Open items", stats.openItems.toString(), Modifier.weight(1f))
            StatCard("Completed total", stats.completedTotal.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Day streak", "🔥 ${stats.streakDays}", Modifier.weight(1f))
            StatCard("Avg completion (h)", avg, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Completed (period)", stats.completedInPeriod.toString(), Modifier.weight(1f))
            StatCard("Created (period)", stats.createdInPeriod.toString(), Modifier.weight(1f))
        }
        val from = WeldoTime.formatDay(stats.from)
        val to = WeldoTime.formatDay(stats.to)
        if (from != null && to != null) {
            Text(
                "${stats.period ?: ""} · $from – $to",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
