package com.hopehealth.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary            = AppColors.primaryLight,
    onPrimary          = AppColors.primaryDark,
    primaryContainer   = AppColors.primaryDark,
    onPrimaryContainer = AppColors.primaryLight,
    secondary          = AppColors.primaryLight,
    onSecondary        = AppColors.primaryDark,
    background         = AppColors.darkBackground,
    onBackground       = AppColors.darkTextPrimary,
    surface            = AppColors.darkSurface,
    onSurface          = AppColors.darkTextPrimary,
    surfaceVariant     = AppColors.darkSurfaceVariant,
    onSurfaceVariant   = AppColors.darkTextSecondary,
    surfaceContainer   = AppColors.darkSurfaceElevated,
    error              = AppColors.error,
    onError            = AppColors.textOnPrimary,
    outline            = AppColors.darkBorder,
    outlineVariant     = AppColors.darkBorder.copy(alpha = 0.5f),
)

/**
 * HopeHealth Material3 theme.
 *
 * Always uses the explicitly defined [LightColorScheme] / [DarkColorScheme] from [AppColors].
 * Dynamic color (Material You) is intentionally disabled — it overrides the brand palette with
 * wallpaper-derived tones that break contrast and visual consistency.
 */
@Composable
fun HopeHealthTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

