package com.fginc.weldo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.ui.capture.CaptureBar
import com.fginc.weldo.ui.home.AgendaScreen
import com.fginc.weldo.ui.home.HomeViewModel
import com.fginc.weldo.ui.home.ItemsScreen
import com.fginc.weldo.ui.stats.StatsPane

/** The three top-level destinations — the web left rail, translated to a bottom bar. */
private enum class MainTab(val title: String, val label: String, val icon: ImageVector) {
    HOME("Weldo", "Home", Icons.Filled.Home),
    ITEMS("All items", "Items", Icons.AutoMirrored.Filled.List),
    STATS("Statistics", "Stats", Icons.Filled.BarChart),
}

/**
 * The signed-in shell. Mirrors the web left rail (Home / All items / Statistics
 * + always-present capture): a bottom [NavigationBar] switches the three tabs,
 * with the [CaptureBar] as a compact "＋ Capture" accessory floating above it,
 * and Settings as a top-bar gear (one per tab). Home + Items share one
 * [HomeViewModel]; Stats has its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onOpenItem: (ItemType, String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var tab by remember { mutableStateOf(MainTab.HOME) }
    val homeVm: HomeViewModel = viewModel()
    LaunchedEffect(Unit) { homeVm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.title) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                CaptureBar(projectId = null, onChanged = { homeVm.load() }, compact = true)
                NavigationBar {
                    MainTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (tab) {
            MainTab.HOME -> AgendaScreen(homeVm, padding, onOpenItem, onOpenProject)
            MainTab.ITEMS -> ItemsScreen(homeVm, padding, onOpenItem, onOpenProject)
            MainTab.STATS -> StatsPane(padding)
        }
    }
}
