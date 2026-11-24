package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TvDarkColorScheme = darkColorScheme(
    primary = TvColors.Primary,
    onPrimary = TvColors.OnPrimary,
    primaryContainer = TvColors.FocusContainer,
    onPrimaryContainer = TvColors.PrimaryTextAccent,
    secondary = TvColors.ActiveChipContainer,
    onSecondary = TvColors.ActiveChipText,
    background = TvColors.Background,
    onBackground = TvColors.TextPrimary,
    surface = TvColors.Surface,
    onSurface = TvColors.TextPrimary,
    surfaceVariant = TvColors.SurfaceVariant,
    onSurfaceVariant = TvColors.TextSecondary,
    outline = TvColors.BorderSubtle,
    error = TvColors.ErrorRed
)

private val TvShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun TVReaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = TvReaderTypography,
        shapes = TvShapes,
        content = content
    )
}
