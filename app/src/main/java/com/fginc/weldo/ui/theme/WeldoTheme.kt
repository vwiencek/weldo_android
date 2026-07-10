package com.fginc.weldo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Brand = Color(0xFF4C5BD4)
private val BrandDark = Color(0xFFB3BCFF)

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = Color(0xFF7C4DFF),
    tertiary = Color(0xFF00897B),
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    secondary = Color(0xFFB9A6FF),
    tertiary = Color(0xFF5FD3C4),
)

@Composable
fun WeldoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        window.statusBarColor = colors.surface.toArgb()
    }

    MaterialTheme(colorScheme = colors, content = content)
}
