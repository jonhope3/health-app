package com.fittrack.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColors.primary,
    onPrimary = AppColors.textOnPrimary,
    primaryContainer = AppColors.primarySurface,
    onPrimaryContainer = AppColors.primaryDark,
    secondary = AppColors.accent,
    onSecondary = AppColors.textOnPrimary,
    background = AppColors.background,
    onBackground = AppColors.textPrimary,
    surface = AppColors.surface,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.textSecondary,
    error = AppColors.error,
    onError = AppColors.textOnPrimary,
    outline = AppColors.border,
)

@Composable
fun FitTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
