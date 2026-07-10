package com.fginc.weldo.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.model.Statistics

/** Four compact KPI tiles fed by GET /statistics; null-safe to zeros. */
@Composable
fun StatsStrip(stats: Statistics?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiTile("Open", (stats?.openItems ?: 0).toString())
        KpiTile("Done", (stats?.completedTotal ?: 0).toString())
        KpiTile("Streak", "🔥 ${stats?.streakDays ?: 0}")
        KpiTile("This period", (stats?.completedInPeriod ?: 0).toString())
    }
}

@Composable
private fun KpiTile(label: String, value: String) {
    Card {
        Column(
            Modifier.widthIn(min = 76.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
