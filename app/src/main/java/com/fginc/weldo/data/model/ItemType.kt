package com.fginc.weldo.data.model

/**
 * The nine GTD item types. [wire] is the capture/type string the backend uses
 * (note the underscore in `waiting_for`); [endpoint] is the REST base path
 * (note the hyphen in `waiting-for` and the singular `task`).
 */
enum class ItemType(
    val wire: String,
    val endpoint: String,
    val display: String,
    val completable: Boolean,
    val colorHex: Long,
) {
    TASK("task", "task", "Task", true, 0xFF4C5BD4),
    PROJECT("project", "projects", "Project", true, 0xFF7C4DFF),
    COMMITMENT("commitment", "commitments", "Commitment", true, 0xFFD81B60),
    REMINDER("reminder", "reminders", "Reminder", true, 0xFFF4511E),
    WAITING_FOR("waiting_for", "waiting-for", "Waiting For", true, 0xFF00897B),
    IDEA("idea", "ideas", "Idea", false, 0xFFFDD835),
    ROUTINE("routine", "routines", "Routine", false, 0xFF3949AB),
    SUGGESTION("suggestion", "suggestions", "Suggestion", false, 0xFF8E24AA),
    NOTE("note", "notes", "Note", false, 0xFF6D4C41);

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
