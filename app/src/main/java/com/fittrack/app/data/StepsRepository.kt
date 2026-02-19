package com.fittrack.app.data

import android.content.Context
import java.time.LocalDate

class StepsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("fittrack_steps", Context.MODE_PRIVATE)

    fun getSteps(date: String): Int = prefs.getInt("steps_$date", 0)

    fun saveSteps(count: Int, date: String) {
        prefs.edit().putInt("steps_$date", count).apply()
    }

    fun getStepsHistory(days: Int = 7): List<Pair<String, Int>> {
        val today = LocalDate.now()
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            key to getSteps(key)
        }
    }

    fun cleanupOldData(retainDays: Int = 90) {
        val cutoff = LocalDate.now().minusDays(retainDays.toLong())
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith("steps_") }
            .forEach { key ->
                val dateStr = key.removePrefix("steps_")
                try {
                    if (LocalDate.parse(dateStr).isBefore(cutoff)) editor.remove(key)
                } catch (_: Exception) { }
            }
        editor.apply()
    }
}
