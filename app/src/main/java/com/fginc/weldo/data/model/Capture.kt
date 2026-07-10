package com.fginc.weldo.data.model

import kotlinx.serialization.Serializable

// ---- Capture (POST /capture, POST /capture/image) ----

@Serializable
data class CaptureRequest(val text: String)

@Serializable
data class CaptureImageRequest(val imageBase64: String, val mimeType: String = "image/jpeg")

@Serializable
data class CaptureProjectProposal(
    val title: String? = null,
    val detail: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class CaptureItemProposal(
    val type: String? = null,
    val title: String? = null,
    val detail: String? = null,
    val madeTo: String? = null,
    val waitingOn: String? = null,
    val dueDate: String? = null,
    val remindAt: String? = null,
    val followUpAt: String? = null,
    val recurrenceRule: String? = null,
    val confidence: Double? = null,
)

@Serializable
data class CaptureProposal(
    val project: CaptureProjectProposal? = null,
    val existingProjectId: String? = null,
    val existingProjectTitle: String? = null,
    val items: List<CaptureItemProposal> = emptyList(),
)

// ---- Batch create (POST /items/batch) ----

@Serializable
data class BatchProjectRequest(
    val title: String? = null,
    val detail: String? = null,
    val dueDate: String? = null,
)

@Serializable
data class BatchItemRequest(
    val type: String,
    val title: String,
    val detail: String? = null,
    val madeTo: String? = null,
    val waitingOn: String? = null,
    val dueDate: String? = null,
    val remindAt: String? = null,
    val followUpAt: String? = null,
    val recurrenceRule: String? = null,
)

@Serializable
data class BatchCreateRequest(
    val contextProjectId: String? = null,
    val project: BatchProjectRequest? = null,
    val items: List<BatchItemRequest> = emptyList(),
)

@Serializable
data class BatchCreateResponse(
    val project: Project? = null,
    val itemCount: Long = 0,
)

// ---- Attachments (/attachments) ----

@Serializable
data class AttachmentCreateRequest(
    val dataBase64: String,
    val mimeType: String,
    val itemType: String? = null,
    val itemId: String? = null,
)

@Serializable
data class AttachmentMeta(
    val id: String,
    val mimeType: String? = null,
)

@Serializable
data class AttachmentData(
    val id: String,
    val mimeType: String? = null,
    val dataBase64: String,
)
