package com.fginc.weldo.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.model.ItemType
import kotlinx.coroutines.launch

/**
 * Self-managing "Photos" strip for an item (mirror of the iOS AttachmentsSection). Lists the
 * item's attachments, lazily decodes each thumbnail, adds via the photo picker, and removes on tap.
 * All ops hit the server immediately; failures degrade quietly (empty strip / "?" placeholder).
 */
@Composable
fun AttachmentsSection(type: ItemType, itemId: String) {
    val repo = WeldoApp.graph.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ids = remember(itemId) { mutableStateListOf<String>() }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            repo.listAttachments(type, itemId).onSuccess {
                ids.clear(); ids.addAll(it.mapNotNull { m -> m.id })
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            repo.addAttachment(type, itemId, b64, mime).onSuccess { reload() }
        }
    }

    LaunchedEffect(type, itemId) { reload() }

    Column {
        SectionHeader("Photos")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            ids.forEach { id ->
                Thumbnail(id = id, onClick = { pendingDelete = id })
            }
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = "Remove photo?",
            text = "This deletes the attachment.",
            confirmLabel = "Remove",
            onConfirm = { scope.launch { repo.deleteAttachment(id).onSuccess { reload() } } },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun Thumbnail(id: String, onClick: () -> Unit) {
    val repo = WeldoApp.graph.repository
    var bitmap by remember(id) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(id) { mutableStateOf(false) }

    LaunchedEffect(id) {
        repo.attachmentData(id)
            .onSuccess {
                try {
                    val bytes = Base64.decode(it.dataBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) bitmap = bmp.asImageBitmap() else failed = true
                } catch (_: Exception) {
                    failed = true
                }
            }
            .onFailure { failed = true }
    }

    Box(
        Modifier
            .padding(end = 8.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(bmp, contentDescription = "Attachment", modifier = Modifier.size(72.dp), contentScale = ContentScale.Crop)
            failed -> Text("?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> CircularProgressIndicator(Modifier.size(20.dp))
        }
    }
}
