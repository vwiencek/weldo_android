package com.fginc.weldo.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.CaptureProposal
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.ui.theme.WeldoTheme

/**
 * The two capture entry points — **Add item** (manual: fill the form directly) and
 * **Use AI** (a chooser between Voice → transcribe → /capture and Photo → /capture/image).
 * Embeddable in any screen; when [projectId] is set, created items land inside that project.
 * Calls [onChanged] after any successful create/batch so the host can reload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureBar(
    projectId: String? = null,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val vm: CaptureViewModel = viewModel()
    val context = LocalContext.current

    val allProjects by vm.allProjects.collectAsState()
    val event by vm.event.collectAsState()
    val busy by vm.busy.collectAsState()

    LaunchedEffect(Unit) { vm.refreshProjects() }

    var formDraft by remember { mutableStateOf<ItemDraft?>(null) }
    var batchProposal by remember { mutableStateOf<CaptureProposal?>(null) }
    var showAiChooser by remember { mutableStateOf(false) }
    var captureMenu by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(event) {
        when (val e = event) {
            is CaptureEvent.Single -> { formDraft = e.draft; vm.consume() }
            is CaptureEvent.Batch -> { batchProposal = e.proposal; vm.consume() }
            is CaptureEvent.Error -> { errorText = e.message; vm.consume() }
            null -> {}
        }
    }

    val recorder = remember { VoiceRecorder(context) }
    fun startVoice() {
        showAiChooser = false
        recorder.start(onResult = { vm.classifyText(it, projectId) }, onError = { errorText = it })
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoice() else errorText = "Microphone permission needed"
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val res = ImageUtil.uriToJpegBase64(context, uri)
            if (res != null) vm.classifyImage(res.first, res.second, projectId) else errorText = "Couldn't read that image"
        }
    }

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        if (compact) {
            // A single discrete "＋ Capture" pill whose menu offers the same two
            // actions — keeps the bottom accessory uncluttered (web-rail parity).
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box {
                    Button(
                        onClick = { captureMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WeldoTheme.colors.violet,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Capture")
                    }
                    DropdownMenu(expanded = captureMenu, onDismissRequest = { captureMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add item") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = { captureMenu = false; formDraft = ItemDraft.blank(ItemType.TASK, projectId) },
                        )
                        DropdownMenuItem(
                            text = { Text("Use AI") },
                            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                            onClick = { captureMenu = false; showAiChooser = true },
                        )
                    }
                }
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterEnd).size(22.dp), strokeWidth = 2.dp)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = { formDraft = ItemDraft.blank(ItemType.TASK, projectId) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add item")
                }
                Button(
                    onClick = { showAiChooser = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WeldoTheme.colors.coral,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Use AI")
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
    }

    if (showAiChooser) {
        AiChooserSheet(
            onDismiss = { showAiChooser = false },
            onVoice = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startVoice()
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onPhoto = {
                showAiChooser = false
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }

    formDraft?.let { draft ->
        ItemFormSheet(
            initial = draft,
            allProjects = allProjects,
            onDismiss = { formDraft = null },
            onSaved = { formDraft = null; onChanged() },
        )
    }

    batchProposal?.let { proposal ->
        BatchReviewSheet(
            proposal = proposal,
            projectId = projectId,
            allProjects = allProjects,
            onDismiss = { batchProposal = null },
            onSaved = { batchProposal = null; onChanged() },
        )
    }

    errorText?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { errorText = null },
            confirmButton = { TextButton(onClick = { errorText = null }) { Text("OK") } },
            text = { Text(msg) },
        )
    }
}

/** "Use AI" — pick voice or photo capture; both feed the same /capture review. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChooserSheet(onDismiss: () -> Unit, onVoice: () -> Unit, onPhoto: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Use AI", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Capture by voice or photo — AI turns it into items.",
                style = MaterialTheme.typography.bodyMedium,
                color = WeldoTheme.colors.muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiChoice(
                    icon = Icons.Filled.Mic,
                    title = "Voice",
                    subtitle = "Speak; we'll transcribe it",
                    tint = WeldoTheme.colors.violetTintFg,
                    tintBg = WeldoTheme.colors.violetTintBg,
                    modifier = Modifier.weight(1f),
                    onClick = onVoice,
                )
                AiChoice(
                    icon = Icons.Filled.PhotoCamera,
                    title = "Photo",
                    subtitle = "Use your camera or upload",
                    tint = WeldoTheme.colors.coralTintFg,
                    tintBg = WeldoTheme.colors.coralTintBg,
                    modifier = Modifier.weight(1f),
                    onClick = onPhoto,
                )
            }
        }
    }
}

@Composable
private fun AiChoice(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    tintBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(tintBg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = WeldoTheme.colors.muted,
        )
    }
}
