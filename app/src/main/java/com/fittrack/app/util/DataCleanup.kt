package com.fittrack.app.util

import android.content.Context
import java.time.LocalDate

fun pruneOldData(context: Context) {
    val metaPrefs = context.getSharedPreferences("fittrack_meta", Context.MODE_PRIVATE)
    val lastCleanup = metaPrefs.getString("last_cleanup", null)
    val today = LocalDate.now().toString()
    if (lastCleanup == today) return

    val cutoff = LocalDate.now().minusDays(90)

    val foodPrefs = context.getSharedPreferences("fittrack_food", Context.MODE_PRIVATE)
    val foodEditor = foodPrefs.edit()
    foodPrefs.all.keys
        .filter { it.startsWith("food_log_") }
        .filter { key ->
            try {
                LocalDate.parse(key.removePrefix("food_log_")).isBefore(cutoff)
            } catch (_: Exception) {
                false
            }
        }
        .forEach { foodEditor.remove(it) }
    foodEditor.apply()

    val stepsPrefs = context.getSharedPreferences("fittrack_steps", Context.MODE_PRIVATE)
    val stepsEditor = stepsPrefs.edit()
    stepsPrefs.all.keys
        .filter { it.startsWith("steps_") }
        .filter { key ->
            try {
                LocalDate.parse(key.removePrefix("steps_")).isBefore(cutoff)
            } catch (_: Exception) {
                false
            }
        }
        .forEach { stepsEditor.remove(it) }
    stepsEditor.apply()

    metaPrefs.edit().putString("last_cleanup", today).apply()
}
