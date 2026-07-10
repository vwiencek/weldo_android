package com.fginc.weldo.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fginc.weldo.data.ItemDraft
import com.fginc.weldo.data.model.CaptureProposal
import com.fginc.weldo.data.model.ItemType
import kotlinx.coroutines.launch

/**
 * The four capture entry points (new / text / voice / photo) + their review sheets.
 * Embeddable in any screen; when [projectId] is set, created items land inside that project.
 * Calls [onChanged] after any successful create/batch so the host can reload.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureBar(projectId: String? = null, onChanged: () -> Unit, modifier: Modifier = Modifier) {
    val vm: CaptureViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allProjects by vm.allProjects.collectAsState()
    val event by vm.event.collectAsState()
    val busy by vm.busy.collectAsState()

    LaunchedEffect(Unit) { vm.refreshProjects() }

    var formDraft by remember { mutableStateOf<ItemDraft?>(null) }
    var batchProposal by remember { mutableStateOf<CaptureProposal?>(null) }
    var showTextSheet by remember { mutableStateOf(false) }
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
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recorder.start(onResult = { vm.classifyText(it, projectId) }, onError = { errorText = it })
        } else {
            errorText = "Microphone permission needed"
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val res = ImageUtil.uriToJpegBase64(context, uri)
            if (res != null) vm.classifyImage(res.first, res.second, projectId) else errorText = "Couldn't read that image"
        }
    }

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CaptureButton(Icons.Filled.Add, "New") { formDraft = ItemDraft.blank(ItemType.TASK, projectId) }
            CaptureButton(Icons.Filled.Edit, "Text") { showTextSheet = true }
            CaptureButton(Icons.Filled.Mic, "Voice") {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    recorder.start(onResult = { vm.classifyText(it, projectId) }, onError = { errorText = it })
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            CaptureButton(Icons.Filled.PhotoCamera, "Photo") {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }

    if (showTextSheet) {
        TextCaptureSheet(
            onDismiss = { showTextSheet = false },
            onSubmit = { showTextSheet = false; vm.classifyText(it, projectId) },
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

@Composable
private fun CaptureButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextCaptureSheet(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Capture", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.heightIn(min = 8.dp).size(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                label = { Text("Type or paste…") },
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { if (text.isNotBlank()) onSubmit(text) }, enabled = text.isNotBlank()) { Text("Capture") }
            }
        }
    }
}
