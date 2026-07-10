package com.fginc.weldo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fginc.weldo.data.model.ItemType

/** Icon + brand color per item type, kept out of the model so Compose deps stay in the UI layer. */
val ItemType.icon: ImageVector
    get() = when (this) {
        ItemType.TASK -> Icons.Outlined.CheckCircle
        ItemType.PROJECT -> Icons.Filled.Folder
        ItemType.COMMITMENT -> Icons.Outlined.People
        ItemType.REMINDER -> Icons.Filled.Notifications
        ItemType.WAITING_FOR -> Icons.Outlined.HourglassEmpty
        ItemType.IDEA -> Icons.Filled.Lightbulb
        ItemType.ROUTINE -> Icons.Filled.Repeat
        ItemType.SUGGESTION -> Icons.Outlined.AutoAwesome
        ItemType.NOTE -> Icons.Outlined.Notes
    }

val ItemType.color: Color get() = Color(colorHex)
