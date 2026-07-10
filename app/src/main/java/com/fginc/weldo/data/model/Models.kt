package com.fginc.weldo.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Wire models mirroring the backend (`doit_back`) contract. See ../doit_back/JSON.md.
 *
 * Deliberate quirks matched here (do not "clean up" one side only):
 *  - Every date/time value is kept as a raw String. Backend emits Java `Instant` with
 *    microsecond precision, which no stock parser round-trips; [com.fginc.weldo.data.remote.WeldoTime]
 *    parses/formats. `dueDate` is a calendar day; the rest are instants.
 *  - Completion flag serializes as `completed`; we also decode `isCompleted` via @JsonNames.
 *  - Routine's flag is `active` (decode also tolerates `isActive`).
 *  - Server owns id/userId/createdAt/updatedAt; they are nullable and dropped on encode when null.
 */

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Task(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val projectId: String? = null,
    val dueDate: String? = null,
    @JsonNames("isCompleted") val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Project(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val parentId: String? = null,
    val dueDate: String? = null,
    @JsonNames("isCompleted") val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Commitment(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val madeTo: String? = null,
    val dueDate: String? = null,
    val projectId: String? = null,
    @JsonNames("isCompleted") val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Reminder(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val remindAt: String? = null,
    val projectId: String? = null,
    @JsonNames("isCompleted") val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class WaitingFor(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val waitingOn: String? = null,
    val followUpAt: String? = null,
    val projectId: String? = null,
    @JsonNames("isCompleted") val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Idea(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val projectId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Note(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val projectId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Routine(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val recurrenceRule: String? = null,
    val lastCompletedAt: String? = null,
    val projectId: String? = null,
    @JsonNames("isActive") val active: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class Suggestion(
    val id: String? = null,
    val userId: String? = null,
    val title: String = "",
    val detail: String = "",
    val status: String = "PENDING",
    val projectId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/** GET /items — the aggregate home feed (all nine types). `notes` may be absent on older servers. */
@Serializable
data class AllItems(
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val ideas: List<Idea> = emptyList(),
    val waitingFor: List<WaitingFor> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val notes: List<Note> = emptyList(),
)

/** GET /projects/{id}/items — one project's subprojects + contents. */
@Serializable
data class ProjectItems(
    val subprojects: List<Project> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val ideas: List<Idea> = emptyList(),
    val waitingFor: List<WaitingFor> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val notes: List<Note> = emptyList(),
)

/** Field-preserving completion toggle: PUT /<type>/{id}/completed. */
@Serializable
data class CompletionUpdate(val completed: Boolean)

// ---- Auth ----

@Serializable
data class AppleLoginRequest(val identityToken: String)

@Serializable
data class PasswordAuthRequest(val email: String, val password: String)

@Serializable
data class AuthTokenResponse(val token: String)

// ---- Profile ----

@Serializable
data class UserProfile(
    val id: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val handle: String? = null,
    val preferences: Map<String, String> = emptyMap(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ProfileUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val handle: String? = null,
)

@Serializable
data class HandleCheckResponse(val handle: String, val available: Boolean)

@Serializable
data class PreferenceValue(val value: String)

// ---- Statistics ----

@Serializable
data class Statistics(
    val period: String? = null,
    val from: String? = null,
    val to: String? = null,
    val openItems: Long = 0,
    val completedTotal: Long = 0,
    val streakDays: Long = 0,
    val completedInPeriod: Long = 0,
    val createdInPeriod: Long = 0,
    val avgCompletionHours: Double? = null,
)
