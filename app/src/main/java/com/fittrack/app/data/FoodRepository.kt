package com.fittrack.app.data

import com.fittrack.app.data.db.DateCaloriesRow
import com.fittrack.app.data.db.DiaryItemDao
import com.fittrack.app.data.db.FoodItemDao
import com.fittrack.app.data.db.FoodItemEntity
import com.fittrack.app.data.db.toDomain
import com.fittrack.app.data.db.toEntity
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for food diary entries and the custom food item library.
 *
 * All operations are suspend functions or return Flow — callers must invoke them
 * from a coroutine context (e.g. viewModelScope). Room automatically dispatches
 * DAO queries off the main thread when using room-ktx.
 */
class FoodRepository(
    private val diaryDao: DiaryItemDao,
    private val foodDao: FoodItemDao,
) {

    // ── Diary ─────────────────────────────────────────────────────────────────

    /** Reactive stream of diary entries for [date] (ISO-8601, e.g. "2025-02-23"). */
    fun getDiaryFlow(date: String): Flow<List<DiaryItem>> =
        diaryDao.getDiaryForDateFlow(date).map { it.map(::toDomain) }

    suspend fun getDiary(date: String): List<DiaryItem> =
        diaryDao.getDiaryForDate(date).map(::toDomain)

    suspend fun addDiaryItem(entry: DiaryItem, date: String) {
        val item = if (entry.id.isBlank()) entry.copy(id = UUID.randomUUID().toString()) else entry
        diaryDao.insert(item.toEntity())
    }

    suspend fun updateDiaryItem(updated: DiaryItem) = diaryDao.update(updated.toEntity())

    suspend fun removeDiaryItem(id: String) = diaryDao.deleteById(id)

    suspend fun getTotalCaloriesToday(): Int =
        diaryDao.getTotalCaloriesForDate(LocalDate.now().toString()) ?: 0

    suspend fun getTodayMacros(): TodayMacros {
        val row = diaryDao.getMacrosForDate(LocalDate.now().toString())
        return TodayMacros(
            protein = row?.protein ?: 0f,
            carbs   = row?.carbs   ?: 0f,
            fat     = row?.fat     ?: 0f,
            sugar   = row?.sugar   ?: 0f,
        )
    }

    /**
     * Returns (date, totalCalories) pairs for the last [days] days, newest first.
     * Days with no entries produce a 0 total.
     */
    suspend fun getCaloriesHistory(days: Int = 7): List<Pair<String, Int>> {
        val today = LocalDate.now()
        val dates = List(days) { i -> today.minusDays(i.toLong()).toString() }
        val rowMap: Map<String, Int> = diaryDao
            .getCaloriesTotalsForDates(dates)
            .associate { it.date to it.total }
        return dates.map { it to (rowMap[it] ?: 0) }
    }

    suspend fun cleanupOldData(retainDays: Int = 90) {
        val cutoff = LocalDate.now().minusDays(retainDays.toLong()).toString()
        diaryDao.deleteOlderThan(cutoff)
    }

    // ── Custom Food Library ───────────────────────────────────────────────────

    /** Reactive stream of all saved food items, sorted by usage frequency. */
    fun getAllFoodItemsFlow(): Flow<List<FoodItem>> =
        foodDao.getAllFlow().map { it.map(::toDomain) }

    suspend fun getAllFoodItems(): List<FoodItem> =
        foodDao.getAll().map(::toDomain)

    suspend fun searchFoodItems(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        return foodDao.search(query.lowercase()).map(::toDomain)
    }

    /**
     * Inserts a new food item or bumps the usage count if it already exists.
     * The food library acts as a log — existing entries accumulate history.
     */
    suspend fun addOrUpdateFoodItem(food: FoodItem) {
        val key = food.name.lowercase()
        val now = System.currentTimeMillis()
        val existing = foodDao.getByNameLower(key)
        val entity = if (existing != null) {
            existing.copy(
                usageCount   = existing.usageCount + 1,
                lastUsed     = now,
                usageHistory = (existing.usageHistory + now).takeLast(50),
            )
        } else {
            FoodItemEntity(
                nameLower        = key,
                name             = food.name,
                calories         = food.calories,
                protein          = food.protein,
                carbs            = food.carbs,
                fat              = food.fat,
                sugar            = food.sugar,
                servingDescription = food.servingDescription,
                usageCount       = 1,
                lastUsed         = now,
                firstAdded       = now,
                usageHistory     = listOf(now),
            )
        }
        foodDao.insert(entity)
    }

    suspend fun removeFoodItem(name: String) = foodDao.deleteByNameLower(name.lowercase())

    // ── Helper types ──────────────────────────────────────────────────────────

    data class TodayMacros(
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val sugar: Float,
    )
}

// Private mapper reference helpers (used in map { } lambdas above)
private fun toDomain(entity: com.fittrack.app.data.db.DiaryItemEntity): DiaryItem =
    entity.toDomain()

private fun toDomain(entity: com.fittrack.app.data.db.FoodItemEntity): FoodItem =
    entity.toDomain()
