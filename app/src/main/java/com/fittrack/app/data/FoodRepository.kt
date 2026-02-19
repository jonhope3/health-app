package com.fittrack.app.data

import android.content.Context
import com.fittrack.app.util.todayKey
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FoodRepository(context: Context) {

    private val prefs = context.getSharedPreferences("fittrack_food", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun getFoodLog(date: String): List<FoodEntry> {
        val jsonStr = prefs.getString("food_log_$date", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<FoodEntry>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addFoodEntry(entry: FoodEntry, date: String) {
        val list = getFoodLog(date).toMutableList()
        list.add(entry.copy(id = UUID.randomUUID().toString()))
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun removeFoodEntry(id: String, date: String) {
        val list = getFoodLog(date).filter { it.id != id }
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun updateFoodEntry(updated: FoodEntry, date: String) {
        val list = getFoodLog(date).map { if (it.id == updated.id) updated else it }
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun getTotalCaloriesToday(): Int =
        getFoodLog(todayKey()).sumOf { it.calories }

    fun getTodayMacros(): Triple<Float, Float, Float> {
        val entries = getFoodLog(todayKey())
        val protein = entries.sumOf { it.protein.toDouble() }.toFloat()
        val carbs = entries.sumOf { it.carbs.toDouble() }.toFloat()
        val fat = entries.sumOf { it.fat.toDouble() }.toFloat()
        return Triple(protein, carbs, fat)
    }

    fun addCustomFood(food: CustomFood) {
        val all = getAllCustomFoods().toMutableList()
        val existing = all.find { it.name.equals(food.name, ignoreCase = true) }
        val updated = if (existing != null) {
            all.filter { !it.name.equals(food.name, ignoreCase = true) } +
                food.copy(
                    usageCount = existing.usageCount + 1,
                    lastUsed = System.currentTimeMillis()
                )
        } else {
            all + food.copy(
                usageCount = 1,
                lastUsed = System.currentTimeMillis()
            )
        }
        prefs.edit().putString("custom_foods", json.encodeToString(updated)).apply()
    }

    fun searchCustomFoods(query: String): List<CustomFood> {
        if (query.isBlank()) return emptyList()
        val lower = query.lowercase()
        return getAllCustomFoods()
            .filter { it.name.lowercase().contains(lower) }
            .sortedByDescending { it.usageCount }
    }

    fun getAllCustomFoods(): List<CustomFood> {
        val jsonStr = prefs.getString("custom_foods", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<CustomFood>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun cleanupOldData(retainDays: Int = 90) {
        val cutoff = LocalDate.now().minusDays(retainDays.toLong())
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith("food_log_") }
            .forEach { key ->
                val dateStr = key.removePrefix("food_log_")
                try {
                    if (LocalDate.parse(dateStr).isBefore(cutoff)) editor.remove(key)
                } catch (_: Exception) { }
            }
        editor.apply()
    }
}
