package com.fginc.weldo.data

import com.fginc.weldo.data.model.ItemType

/**
 * A unified, editable shape spanning every field of every item type, so one form and one
 * detail screen can drive all five. Only the fields relevant to [type] are shown/sent;
 * mapping to the concrete wire model happens in [WeldoRepository].
 *
 * Date fields keep the wire convention: [dueDate] is a calendar day (yyyy-MM-dd);
 * [remindAt] is an ISO instant.
 */
data class ItemDraft(
    val type: ItemType,
    val id: String? = null,
    val title: String = "",
    val detail: String = "",
    val projectId: String? = null,
    val dueDate: String? = null,
    val remindAt: String? = null,
    val recurrenceRule: String? = null,
    val active: Boolean = true,       // routine
    val completed: Boolean = false,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    companion object {
        fun blank(type: ItemType, projectId: String? = null) = ItemDraft(type = type, projectId = projectId)
    }
}
