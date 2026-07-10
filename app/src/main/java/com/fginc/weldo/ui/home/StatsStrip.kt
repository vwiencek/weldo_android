package com.fginc.weldo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.model.Statistics
import com.fginc.weldo.ui.theme.WeldoTheme

/** Four compact KPI tiles fed by GET /statistics; alternating violet/coral Punch tints. */
@Composable
fun StatsStrip(stats: Statistics?, modifier: Modifier = Modifier) {
    val c = WeldoTheme.colors
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KpiTile("Open items", (stats?.openItems ?: 0).toString(), c.statVioletBg, c.violetTintFg)
        KpiTile("Done", (stats?.completedTotal ?: 0).toString(), c.statCoralBg, c.coralFg)
        KpiTile("Day streak", (stats?.streakDays ?: 0).toString(), c.statVioletBg, c.violetTintFg)
        KpiTile("This period", (stats?.completedInPeriod ?: 0).toString(), c.statCoralBg, c.coralFg)
    }
}

@Composable
private fun KpiTile(label: String, value: String, bg: Color, valueColor: Color) {
    Column(
        Modifier
            .widthIn(min = 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = WeldoTheme.colors.label)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
    }
}
