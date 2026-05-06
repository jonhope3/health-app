package com.fittrack.app.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // ── Brand palette — Google / FitTrack ────────────────────────────────────
    val primary       = Color(0xFF4285F4) // Google Blue
    val primaryDark   = Color(0xFF174EA6) // Deep blue
    val primaryLight  = Color(0xFF8AB4F8) // Light blue — primary for dark surfaces
    val primarySurface = Color(0xFFD2E3FC)
    val accent        = Color(0xFF4285F4)

    // ── Light-mode layout ─────────────────────────────────────────────────────
    val background     = Color(0xFFF1F3F4)
    val surface        = Color(0xFFFFFFFF)
    val surfaceVariant = Color(0xFFF1F3F4)
    val border         = Color(0xFFDADCE0)
    val divider        = Color(0xFFDADCE0)

    // ── Dark-mode layout ──────────────────────────────────────────────────────
    // Slightly elevated off-black avoids pure-black "hole" effect.
    // Cards at darkSurface sit visibly above the darkBackground.
    val darkBackground      = Color(0xFF141416)  // near-black with a warm tint
    val darkSurface         = Color(0xFF1F2123)  // card surface — clearly above bg
    val darkSurfaceVariant  = Color(0xFF2A2D31)  // secondary surface / chips
    val darkSurfaceElevated = Color(0xFF2E3136)  // modal sheets, dialogs
    val darkBorder          = Color(0xFF3C4043)  // subtle border

    // ── Text ──────────────────────────────────────────────────────────────────
    val textPrimary    = Color(0xFF202124) // Google Black
    val textSecondary  = Color(0xFF5F6368) // Google Grey (light mode)
    val textOnPrimary  = Color(0xFFFFFFFF)

    // Dark-mode text — high contrast against dark surfaces
    val darkTextPrimary   = Color(0xFFE8EAED) // ~87% white
    val darkTextSecondary = Color(0xFFBDC1C6) // ~74% white — was too dim at #9AA0A6
    val darkTextTertiary  = Color(0xFF80868B) // hint / disabled

    // ── Status ────────────────────────────────────────────────────────────────
    val error   = Color(0xFFEA4335) // Red
    val success = Color(0xFF34A853) // Green
    val warning = Color(0xFFFBBC04) // Yellow

    // ── Macro / element colors ─────────────────────────────────────────────────
    val calorie = Color(0xFFEA4335)
    val protein = Color(0xFF34A853)
    val carbs   = Color(0xFFFBBC04)
    val fat     = Color(0xFFE37400)
    val sugar   = Color(0xFFD81B60)
    val steps   = Color(0xFF4285F4)

    // ── Ring chart track colors (visible on dark bg) ───────────────────────────
    // Light mode: subtle gray track
    val ringTrackLight = Color(0xFFE0E0E0)
    // Dark mode: slightly lighter than surface so the arc is visible
    val ringTrackDark  = Color(0xFF35383C)
}

/** Flo-inspired warm pink/teal palette for the Family cycle tracking feature. */
object FamilyColors {
    // ── Primary — warm pink/rose ──
    val primary       = Color(0xFFE91E63)
    val primaryLight  = Color(0xFFF48FB1)
    val primaryDark   = Color(0xFFC2185B)
    val primarySurface = Color(0xFFFCE4EC)

    // ── Cycle phases ──
    val menstrual     = Color(0xFFE57373)
    val follicular    = Color(0xFF80DEEA)
    val ovulatory     = Color(0xFF26A69A)
    val luteal        = Color(0xFFCE93D8)

    // ── Fertility indicators ──
    val fertileLow    = Color(0xFFBDBDBD)
    val fertileMedium = Color(0xFFFFD54F)
    val fertileHigh   = Color(0xFF26A69A)

    // ── Temperature ──
    val tempLine      = Color(0xFFE91E63)
    val coverline     = Color(0xFF26A69A)

    // ── Dark mode variants ──
    val darkPrimary   = Color(0xFFF48FB1)
    val darkSurface   = Color(0xFF2A1A1F)
}
