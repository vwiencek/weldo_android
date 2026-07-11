package com.fginc.weldo.data.model

/**
 * The five item types (backend DOMAIN.md). [wire] is the capture/type string the
 * backend uses; [endpoint] is the REST base path (note the singular `task`).
 * Only task / project / reminder are completable; project is the container and
 * does not nest.
 */
enum class ItemType(
    val wire: String,
    val endpoint: String,
    val display: String,
    val completable: Boolean,
    val colorHex: Long,
) {
    // Punch palette: every type reads as either violet #7C3AED or coral #FF6A3D
    // (see ItemType.tone in ui/ItemVisuals). colorHex is the row/icon accent.
    TASK("task", "task", "Task", true, 0xFF7C3AED),
    PROJECT("project", "projects", "Project", true, 0xFF7C3AED),
    REMINDER("reminder", "reminders", "Reminder", true, 0xFFFF6A3D),
    ROUTINE("routine", "routines", "Routine", false, 0xFF7C3AED),
    NOTE("note", "notes", "Note", false, 0xFFFF6A3D);

    companion object {
        fun fromWire(value: String?): ItemType? =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) }
    }
}

/**
 * A type-erased view of any item, so the mixed home/search/project lists can render
 * every type through one row without a giant `when`. Built by the repository.
 */
data class AnyItem(
    val type: ItemType,
    val id: String,
    val title: String,
    val detail: String,
    val projectId: String?,
    val completed: Boolean,
    val updatedAt: String?,
    /** One-line, type-specific context (e.g. "to Alice", "due Jul 12"). */
    val subtitle: String?,
)
