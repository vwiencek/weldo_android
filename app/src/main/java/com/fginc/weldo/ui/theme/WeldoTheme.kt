package com.fginc.weldo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * "Punch" design tokens (violet #7C3AED + coral #FF6A3D) — the web app's design
 * system ported to Compose. Everything theme-aware lives here so components read
 * [WeldoTheme.colors] and recolor for free between light and dark.
 */
data class WeldoColors(
    val violet: Color,
    val violetDeep: Color,
    val coral: Color,
    val coralFg: Color,
    val violetTintBg: Color,
    val violetTintFg: Color,
    val coralTintBg: Color,
    val coralTintFg: Color,
    val neutralBg: Color,
    val neutralFg: Color,
    val posBg: Color,
    val posFg: Color,
    val statVioletBg: Color,
    val statCoralBg: Color,
    val label: Color,
    val muted: Color,
    val heroGradStart: Color,
    val heroGradEnd: Color,
    val heroHighlight: Color,
)

private val LightWeldo = WeldoColors(
    violet = Color(0xFF7C3AED),
    violetDeep = Color(0xFF6023C9),
    coral = Color(0xFFFF6A3D),
    coralFg = Color(0xFFD24B1E),
    violetTintBg = Color(0xFFF3EEFB),
    violetTintFg = Color(0xFF6023C9),
    coralTintBg = Color(0xFFFFEDE4),
    coralTintFg = Color(0xFFD24B1E),
    neutralBg = Color(0xFFF4F2F7),
    neutralFg = Color(0xFF5C5866),
    posBg = Color(0xFFEEFCF4),
    posFg = Color(0xFF1F8A5B),
    statVioletBg = Color(0xFFFBF7FF),
    statCoralBg = Color(0xFFFFF3EE),
    label = Color(0xFF5C5866),
    muted = Color(0xFF9A93A6),
    heroGradStart = Color(0xFF7C3AED),
    heroGradEnd = Color(0xFF6023C9),
    heroHighlight = Color(0xFFFFC7AE),
)

private val DarkWeldo = WeldoColors(
    violet = Color(0xFFA78BFA),
    violetDeep = Color(0xFF7C3AED),
    coral = Color(0xFFFF6A3D),
    coralFg = Color(0xFFFF9D7A),
    violetTintBg = Color(0x2E8B5CF6),
    violetTintFg = Color(0xFFC9B6F7),
    coralTintBg = Color(0x33FF6A3D),
    coralTintFg = Color(0xFFFF9D7A),
    neutralBg = Color(0xFF272232),
    neutralFg = Color(0xFFB7B0C4),
    posBg = Color(0xFF16241C),
    posFg = Color(0xFF4FD08A),
    statVioletBg = Color(0x1F8B5CF6),
    statCoralBg = Color(0x21FF6A3D),
    label = Color(0xFFB7B0C4),
    muted = Color(0xFFA49DB3),
    heroGradStart = Color(0xFF7C3AED),
    heroGradEnd = Color(0xFF4A1E9E),
    heroHighlight = Color(0xFFFFC7AE),
)

private val LocalWeldoColors = staticCompositionLocalOf { LightWeldo }

object WeldoTheme {
    val colors: WeldoColors
        @Composable @ReadOnlyComposable get() = LocalWeldoColors.current
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3EEFB),
    onPrimaryContainer = Color(0xFF6023C9),
    secondary = Color(0xFFFF6A3D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1F8A5B),
    background = Color(0xFFF7F5FB),
    onBackground = Color(0xFF17131F),
    surface = Color(0xFFFCFBFE),
    onSurface = Color(0xFF17131F),
    surfaceVariant = Color(0xFFF4F2F7),
    onSurfaceVariant = Color(0xFF5C5866),
    outline = Color(0xFFEAE5F1),
    outlineVariant = Color(0xFFF0ECF6),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1B1526),
    primaryContainer = Color(0xFF2E2440),
    onPrimaryContainer = Color(0xFFC9B6F7),
    secondary = Color(0xFFFF6A3D),
    onSecondary = Color(0xFF1B1526),
    tertiary = Color(0xFF4FD08A),
    background = Color(0xFF14111C),
    onBackground = Color(0xFFF2EFF7),
    surface = Color(0xFF1C1826),
    onSurface = Color(0xFFF2EFF7),
    surfaceVariant = Color(0xFF272232),
    onSurfaceVariant = Color(0xFFB7B0C4),
    outline = Color(0xFF352E42),
    outlineVariant = Color(0xFF2C2637),
    error = Color(0xFFF87171),
)

@Composable
fun WeldoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Punch is a bespoke brand palette — dynamic (Material You) color is off so
    // the violet/coral system shows identically on every device.
    val colors = if (darkTheme) DarkColors else LightColors
    val weldo = if (darkTheme) DarkWeldo else LightWeldo

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        window.statusBarColor = colors.background.toArgb()
    }

    CompositionLocalProvider(LocalWeldoColors provides weldo) {
        MaterialTheme(colorScheme = colors, typography = Typography, content = content)
    }
}
