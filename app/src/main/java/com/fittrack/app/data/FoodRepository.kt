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

    fun getDiary(date: String): List<DiaryItem> {
        val jsonStr = prefs.getString("food_log_$date", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<DiaryItem>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addDiaryItem(entry: DiaryItem, date: String) {
        val list = getDiary(date).toMutableList()
        list.add(entry.copy(id = UUID.randomUUID().toString()))
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun removeDiaryItem(id: String, date: String) {
        val list = getDiary(date).filter { it.id != id }
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun updateDiaryItem(updated: DiaryItem, date: String) {
        val list = getDiary(date).map { if (it.id == updated.id) updated else it }
        prefs.edit().putString("food_log_$date", json.encodeToString(list)).apply()
    }

    fun getTotalCaloriesToday(): Int =
        getDiary(todayKey()).sumOf { it.calories }

    fun getTodayMacros(): Triple<Float, Float, Float> {
        val entries = getDiary(todayKey())
        val protein = entries.sumOf { it.protein.toDouble() }.toFloat()
        val carbs = entries.sumOf { it.carbs.toDouble() }.toFloat()
        val fat = entries.sumOf { it.fat.toDouble() }.toFloat()
        return Triple(protein, carbs, fat)
    }

    fun getCaloriesHistory(days: Int = 7): List<Pair<String, Int>> {
        val today = LocalDate.now()
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            val key = date.toString()
            val totalCal = getDiary(key).sumOf { it.calories }
            key to totalCal
        }
    }

    fun addOrUpdateFoodItem(food: FoodItem) {
        val all = getAllFoodItems().toMutableList()
        val existing = all.find { it.name.equals(food.name, ignoreCase = true) }
        val now = System.currentTimeMillis()
        val updated = if (existing != null) {
            all.filter { !it.name.equals(food.name, ignoreCase = true) } +
                existing.copy(
                    usageCount = existing.usageCount + 1,
                    lastUsed = now,
                    usageHistory = existing.usageHistory + now
                )
        } else {
            all + food.copy(
                usageCount = 1,
                lastUsed = now,
                firstAdded = now,
                usageHistory = listOf(now)
            )
        }
        prefs.edit().putString("custom_foods", json.encodeToString(updated)).apply()
    }

    fun removeFoodItem(name: String) {
        val all = getAllFoodItems().filter { !it.name.equals(name, ignoreCase = true) }
        prefs.edit().putString("custom_foods", json.encodeToString(all)).apply()
    }

    fun searchFoodItems(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        val lower = query.lowercase()
        return getAllFoodItems()
            .filter { it.name.lowercase().contains(lower) }
            .sortedByDescending { it.usageCount }
    }

    fun getAllFoodItems(): List<FoodItem> {
        val jsonStr = prefs.getString("custom_foods", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<FoodItem>>(jsonStr)
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
