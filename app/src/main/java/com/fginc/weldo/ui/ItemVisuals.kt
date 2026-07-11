package com.fginc.weldo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fginc.weldo.data.model.ItemType
import com.fginc.weldo.ui.theme.WeldoTheme

/** Icon + brand color per item type, kept out of the model so Compose deps stay in the UI layer. */
val ItemType.icon: ImageVector
    get() = when (this) {
        ItemType.TASK -> Icons.Outlined.CheckCircle
        ItemType.PROJECT -> Icons.Filled.Folder
        ItemType.REMINDER -> Icons.Filled.Notifications
        ItemType.ROUTINE -> Icons.Filled.Repeat
        ItemType.NOTE -> Icons.Outlined.Notes
    }

val ItemType.color: Color get() = Color(colorHex)

/** Punch tone: each type reads as violet or coral (drives the tinted chip). */
enum class ItemTone { VIOLET, CORAL }

val ItemType.tone: ItemTone
    get() = when (this) {
        ItemType.REMINDER, ItemType.NOTE -> ItemTone.CORAL
        else -> ItemTone.VIOLET
    }

/** Soft tinted background for the type's icon chip / badge (theme-aware). */
val ItemType.chipBg: Color
    @Composable @ReadOnlyComposable
    get() = if (tone == ItemTone.CORAL) WeldoTheme.colors.coralTintBg else WeldoTheme.colors.violetTintBg

/** Foreground (icon/text) color that pairs with [chipBg]. */
val ItemType.chipFg: Color
    @Composable @ReadOnlyComposable
    get() = if (tone == ItemTone.CORAL) WeldoTheme.colors.coralTintFg else WeldoTheme.colors.violetTintFg
