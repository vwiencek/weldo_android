package com.fginc.weldo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fginc.weldo.data.remote.WeldoTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenStats: () -> Unit, onSignedOut: () -> Unit) {
    val vm: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Profile ----
            SettingsCard("Profile") {
                OutlinedTextField(
                    value = state.firstName, onValueChange = vm::setFirstName,
                    label = { Text("First name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.lastName, onValueChange = vm::setLastName,
                    label = { Text("Last name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.birthDate, onValueChange = vm::setBirthDate,
                    label = { Text("Birth date (yyyy-MM-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.handle, onValueChange = vm::setHandle,
                    label = { Text("Handle") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("@") },
                    supportingText = {
                        when (state.handleAvailable) {
                            true -> Text("available", color = Color(0xFF2E7D32))
                            false -> Text("taken or invalid", color = MaterialTheme.colorScheme.error)
                            null -> {}
                        }
                    },
                )
                if (state.email.isNotBlank()) {
                    Text("Email: ${state.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                WeldoTime.formatDay(state.memberSince)?.let {
                    Text("Member since $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = vm::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Saving…" else if (state.saved) "Saved ✓" else "Save profile")
                }
            }

            // ---- Statistics ----
            SettingsCard("Statistics") {
                PeriodChips(selected = state.statsPeriod, onSelect = vm::setStatsPeriod)
                OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) { Text("View statistics") }
            }

            // ---- Server ----
            SettingsCard("Server") {
                var url by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                )
                Button(onClick = { vm.setBaseUrl(url) }, modifier = Modifier.fillMaxWidth()) { Text("Save server") }
            }

            OutlinedButton(
                onClick = { vm.signOut(onSignedOut) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodChips(selected: String, onSelect: (String) -> Unit) {
    val periods = listOf("week" to "Week", "month" to "Month", "year" to "Year")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        periods.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}
