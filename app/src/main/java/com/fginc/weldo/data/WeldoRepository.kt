package com.fginc.weldo.data

import com.fginc.weldo.WeldoApp
import com.fginc.weldo.data.local.WeldoSession
import com.fginc.weldo.data.model.*
import com.fginc.weldo.data.remote.ApiProvider
import com.fginc.weldo.data.remote.WeldoTime

/**
 * The single data gateway used by every ViewModel. Wraps [WeldoApi] with:
 *  - auth flows that persist the session token,
 *  - a normalizer from the typed [AllItems] into a flat [AnyItem] list for the mixed UI,
 *  - a unified [ItemDraft] create/update/load path across all five types,
 *  - completion toggle / delete keyed by [ItemType].
 *
 * Every network method returns [Result]; callers decide how to surface failure.
 */
class WeldoRepository(
    private val apiProvider: ApiProvider,
    private val session: WeldoSession,
) {
    private fun api() = apiProvider.api()
    val isSignedIn: Boolean get() = session.isSignedIn

    // ---------------- Auth ----------------

    suspend fun passwordLogin(email: String, password: String): Result<Unit> = runCatching {
        val resp = api().passwordLogin(PasswordAuthRequest(email.trim(), password))
        if (!resp.isSuccessful) error(authError(resp.code()))
        session.setToken(resp.body()!!.token)
    }

    suspend fun passwordRegister(email: String, password: String): Result<Unit> = runCatching {
        val resp = api().passwordRegister(PasswordAuthRequest(email.trim(), password))
        if (!resp.isSuccessful) error(if (resp.code() == 409) "That email is already registered." else authError(resp.code()))
        session.setToken(resp.body()!!.token)
    }

    suspend fun appleLogin(identityToken: String, appleUserId: String?): Result<Unit> = runCatching {
        val resp = api().loginApple(AppleLoginRequest(identityToken))
        session.setToken(resp.token)
        session.setAppleUserId(appleUserId)
    }

    suspend fun signOut() {
        session.signOut()
        WeldoApp.graph.nudgeScheduler.cancelAll()
    }

    private fun authError(code: Int) = when (code) {
        400 -> "Password must be at least 8 characters."
        401 -> "Incorrect email or password."
        else -> "Sign-in failed ($code)."
    }

    // ---------------- Aggregate read ----------------

    suspend fun loadAll(): Result<AllItems> = runCatching { api().getItems() }

    suspend fun projectItems(id: String): Result<ProjectItems> = runCatching { api().getProjectItems(id) }

    suspend fun statistics(period: String): Result<Statistics> =
        runCatching { api().getStatistics(period, WeldoTime.timezoneId()) }

    // ---------------- Nudges (local-notification feed) ----------------

    /** GET /nudges/upcoming — upcoming reminders + routine occurrences (server 30-day window). */
    suspend fun upcomingNudges(limit: Int = 64): Result<List<Nudge>> =
        runCatching { api().getUpcomingNudges(null, limit) }

    /** PUT /routines/{id}/complete — nudge-and-advance (stamps lastCompletedAt, rolls next). */
    suspend fun completeRoutine(id: String): Result<Unit> =
        runCatching { api().completeRoutine(id); Unit }

    // ---------------- Completion / delete ----------------

    suspend fun setCompleted(type: ItemType, id: String, completed: Boolean): Result<Unit> = runCatching {
        val body = CompletionUpdate(completed)
        when (type) {
            ItemType.TASK -> api().setTaskCompleted(id, body)
            ItemType.PROJECT -> api().setProjectCompleted(id, body)
            ItemType.REMINDER -> api().setReminderCompleted(id, body)
            else -> error("${type.display} is not completable")
        }
        Unit
    }

    suspend fun delete(type: ItemType, id: String): Result<Unit> = runCatching {
        val resp = when (type) {
            ItemType.TASK -> api().deleteTask(id)
            ItemType.PROJECT -> api().deleteProject(id)
            ItemType.REMINDER -> api().deleteReminder(id)
            ItemType.ROUTINE -> api().deleteRoutine(id)
            ItemType.NOTE -> api().deleteNote(id)
        }
        // 404 == already gone; treat as success.
        if (!resp.isSuccessful && resp.code() != 404) error("Delete failed (${resp.code()})")
        Unit
    }

    // ---------------- Load / create / update via ItemDraft ----------------

    suspend fun load(type: ItemType, id: String): Result<ItemDraft> = runCatching {
        when (type) {
            ItemType.TASK -> api().getTask(id).toDraft()
            ItemType.PROJECT -> api().getProject(id).toDraft()
            ItemType.REMINDER -> api().getReminder(id).toDraft()
            ItemType.ROUTINE -> api().getRoutine(id).toDraft()
            ItemType.NOTE -> api().getNote(id).toDraft()
        }
    }

    suspend fun create(draft: ItemDraft): Result<Unit> = runCatching {
        when (draft.type) {
            ItemType.TASK -> api().createTask(draft.toTask())
            ItemType.PROJECT -> api().createProject(draft.toProject())
            ItemType.REMINDER -> api().createReminder(draft.toReminder())
            ItemType.ROUTINE -> api().createRoutine(draft.toRoutine())
            ItemType.NOTE -> api().createNote(draft.toNote())
        }
        Unit
    }

    suspend fun update(draft: ItemDraft): Result<Unit> = runCatching {
        val id = draft.id ?: error("Cannot update without id")
        when (draft.type) {
            ItemType.TASK -> api().updateTask(id, draft.toTask())
            ItemType.PROJECT -> api().updateProject(id, draft.toProject())
            ItemType.REMINDER -> api().updateReminder(id, draft.toReminder())
            ItemType.ROUTINE -> api().updateRoutine(id, draft.toRoutine())
            ItemType.NOTE -> api().updateNote(id, draft.toNote())
        }
        Unit
    }

    // ---------------- Capture / batch ----------------

    suspend fun capture(text: String): Result<CaptureProposal> =
        runCatching { api().capture(CaptureRequest(text), WeldoTime.timezoneId()) }

    suspend fun captureImage(base64: String, mime: String): Result<CaptureProposal> =
        runCatching { api().captureImage(CaptureImageRequest(base64, mime), WeldoTime.timezoneId()) }

    suspend fun captureFile(base64: String, mime: String?, fileName: String?): Result<CaptureProposal> =
        runCatching { api().captureFile(CaptureFileRequest(base64, mime, fileName), WeldoTime.timezoneId()) }

    suspend fun createBatch(request: BatchCreateRequest): Result<BatchCreateResponse> =
        runCatching { api().createBatch(request) }

    // ---------------- Profile / preferences ----------------

    suspend fun profile(): Result<UserProfile> = runCatching { api().getProfile() }

    suspend fun updateProfile(req: ProfileUpdateRequest): Result<UserProfile> = runCatching {
        val resp = api().updateProfile(req)
        when {
            resp.isSuccessful -> resp.body()!!
            resp.code() == 409 -> error("That handle is already taken.")
            resp.code() == 400 -> error("Handle must be 3–30 chars: letters, numbers, underscore.")
            else -> error("Couldn't save profile (${resp.code()}).")
        }
    }

    suspend fun handleAvailable(handle: String): Result<Boolean> = runCatching {
        val resp = api().handleCheck(handle)
        if (!resp.isSuccessful) error("check failed")
        resp.body()!!.available
    }

    suspend fun putPreference(key: String, value: String): Result<Unit> = runCatching {
        api().putPreference(key, PreferenceValue(value)); Unit
    }

    suspend fun getPreferences(): Result<Map<String, String>> = runCatching { api().getPreferences() }

    suspend fun getPreference(key: String): Result<PreferenceEntry> = runCatching { api().getPreference(key) }

    suspend fun deletePreference(key: String): Result<Unit> = runCatching {
        val resp = api().deletePreference(key)
        // 404 == already unset; treat as success.
        if (!resp.isSuccessful && resp.code() != 404) error("Delete failed (${resp.code()})")
        Unit
    }

    // ---------------- Attachments ----------------

    suspend fun stageAttachment(base64: String, mime: String): Result<AttachmentMeta> =
        runCatching { api().createAttachment(AttachmentCreateRequest(base64, mime)) }

    suspend fun addAttachment(type: ItemType, itemId: String, base64: String, mime: String): Result<AttachmentMeta> =
        runCatching { api().createAttachment(AttachmentCreateRequest(base64, mime, type.wire, itemId)) }

    suspend fun listAttachments(type: ItemType, itemId: String): Result<List<AttachmentMeta>> =
        runCatching { api().listAttachments(type.wire, itemId) }

    suspend fun attachmentData(id: String): Result<AttachmentData> = runCatching { api().getAttachment(id) }

    suspend fun deleteAttachment(id: String): Result<Unit> = runCatching {
        val resp = api().deleteAttachment(id)
        if (!resp.isSuccessful && resp.code() != 404) error("Delete failed (${resp.code()})")
        Unit
    }
}

// ---------------- AllItems / ProjectItems → AnyItem ----------------

fun AllItems.toAnyItems(): List<AnyItem> = buildList {
    tasks.forEach { add(it.toAny()) }
    projects.forEach { add(it.toAny()) }
    reminders.forEach { add(it.toAny()) }
    routines.forEach { add(it.toAny()) }
    notes.forEach { add(it.toAny()) }
}

fun ProjectItems.toAnyItems(): List<AnyItem> = buildList {
    tasks.forEach { add(it.toAny()) }
    reminders.forEach { add(it.toAny()) }
    routines.forEach { add(it.toAny()) }
    notes.forEach { add(it.toAny()) }
}

private fun due(date: String?) = WeldoTime.formatDay(date)?.let { "Due $it" }

fun Task.toAny() = AnyItem(ItemType.TASK, id ?: "", title, detail, projectId, completed, updatedAt, due(dueDate))
fun Project.toAny() = AnyItem(ItemType.PROJECT, id ?: "", title, detail, null, completed, updatedAt, due(dueDate))
fun Reminder.toAny() = AnyItem(
    ItemType.REMINDER, id ?: "", title, detail, projectId, completed, updatedAt,
    WeldoTime.formatDateTime(remindAt)?.let { "Remind $it" },
)
fun Routine.toAny() = AnyItem(
    ItemType.ROUTINE, id ?: "", title, detail, projectId, false, updatedAt,
    recurrenceRule?.takeIf { it.isNotBlank() },
)
fun Note.toAny() = AnyItem(ItemType.NOTE, id ?: "", title, detail, projectId, false, updatedAt, null)

// ---------------- wire → ItemDraft ----------------

fun Task.toDraft() = ItemDraft(ItemType.TASK, id, title, detail, projectId, dueDate = dueDate, completed = completed, completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt)
fun Project.toDraft() = ItemDraft(ItemType.PROJECT, id, title, detail, null, dueDate = dueDate, completed = completed, completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt)
fun Reminder.toDraft() = ItemDraft(ItemType.REMINDER, id, title, detail, projectId, remindAt = remindAt, completed = completed, completedAt = completedAt, createdAt = createdAt, updatedAt = updatedAt)
fun Routine.toDraft() = ItemDraft(ItemType.ROUTINE, id, title, detail, projectId, recurrenceRule = recurrenceRule, active = active, createdAt = createdAt, updatedAt = updatedAt)
fun Note.toDraft() = ItemDraft(ItemType.NOTE, id, title, detail, projectId, createdAt = createdAt, updatedAt = updatedAt)

// ---------------- ItemDraft → wire (create/update payloads) ----------------
// PUT is full-replace; the server preserves id/userId/createdAt and re-owns updatedAt,
// so we omit userId/updatedAt and let explicitNulls=false drop the nulls.

private fun String.orNull() = takeIf { it.isNotBlank() }

fun ItemDraft.toTask() = Task(id = id, title = title, detail = detail, projectId = projectId, dueDate = dueDate?.orNull(), completed = completed, completedAt = completedAt, createdAt = createdAt)
fun ItemDraft.toProject() = Project(id = id, title = title, detail = detail, dueDate = dueDate?.orNull(), completed = completed, completedAt = completedAt, createdAt = createdAt)
fun ItemDraft.toReminder() = Reminder(id = id, title = title, detail = detail, remindAt = remindAt?.orNull(), projectId = projectId, completed = completed, completedAt = completedAt, createdAt = createdAt)
fun ItemDraft.toRoutine() = Routine(id = id, title = title, detail = detail, recurrenceRule = recurrenceRule?.orNull(), projectId = projectId, active = active, createdAt = createdAt)
fun ItemDraft.toNote() = Note(id = id, title = title, detail = detail, projectId = projectId, createdAt = createdAt)
