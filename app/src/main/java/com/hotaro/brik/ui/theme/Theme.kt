package com.hotaro.brik.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrikPrimaryLight,
    onPrimary = BrikOnPrimaryLight,
    primaryContainer = BrikPrimaryContainerLight,
    onPrimaryContainer = BrikOnPrimaryContainerLight,
    secondary = BrikSecondaryLight,
    onSecondary = BrikOnSecondaryLight,
    secondaryContainer = BrikSecondaryContainerLight,
    onSecondaryContainer = BrikOnSecondaryContainerLight,
    tertiary = BrikTertiaryLight,
    onTertiary = BrikOnTertiaryLight,
    tertiaryContainer = BrikTertiaryContainerLight,
    onTertiaryContainer = BrikOnTertiaryContainerLight,
    background = BrikBackgroundLight,
    onBackground = BrikOnBackgroundLight,
    surface = BrikSurfaceLight,
    onSurface = BrikOnSurfaceLight,
    surfaceVariant = BrikSurfaceVariantLight,
    onSurfaceVariant = BrikOnSurfaceVariantLight,
    outline = BrikOutlineLight,
    surfaceContainerLowest = BrikSurfaceContainerLowestLight,
    surfaceContainerLow = BrikSurfaceContainerLowLight,
    surfaceContainer = BrikSurfaceContainerLight,
    surfaceContainerHigh = BrikSurfaceContainerHighLight,
    surfaceContainerHighest = BrikSurfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrikPrimaryDark,
    onPrimary = BrikOnPrimaryDark,
    primaryContainer = BrikPrimaryContainerDark,
    onPrimaryContainer = BrikOnPrimaryContainerDark,
    secondary = BrikSecondaryDark,
    onSecondary = BrikOnSecondaryDark,
    secondaryContainer = BrikSecondaryContainerDark,
    onSecondaryContainer = BrikOnSecondaryContainerDark,
    tertiary = BrikTertiaryDark,
    onTertiary = BrikOnTertiaryDark,
    tertiaryContainer = BrikTertiaryContainerDark,
    onTertiaryContainer = BrikOnTertiaryContainerDark,
    background = BrikBackgroundDark,
    onBackground = BrikOnBackgroundDark,
    surface = BrikSurfaceDark,
    onSurface = BrikOnSurfaceDark,
    surfaceVariant = BrikSurfaceVariantDark,
    onSurfaceVariant = BrikOnSurfaceVariantDark,
    outline = BrikOutlineDark,
    surfaceContainerLowest = BrikSurfaceContainerLowestDark,
    surfaceContainerLow = BrikSurfaceContainerLowDark,
    surfaceContainer = BrikSurfaceContainerDark,
    surfaceContainerHigh = BrikSurfaceContainerHighDark,
    surfaceContainerHighest = BrikSurfaceContainerHighestDark,
)

@Composable
fun BrikTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                window.decorView.setBackgroundColor(if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
