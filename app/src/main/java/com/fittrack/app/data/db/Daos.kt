package com.fittrack.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── DiaryItemDao ─────────────────────────────────────────────────────────────

@Dao
interface DiaryItemDao {

    @Query("SELECT * FROM diary_item WHERE date = :date ORDER BY timestamp ASC")
    fun getDiaryForDateFlow(date: String): Flow<List<DiaryItemEntity>>

    @Query("SELECT * FROM diary_item WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getDiaryForDate(date: String): List<DiaryItemEntity>

    @Query("SELECT SUM(calories) FROM diary_item WHERE date = :date")
    suspend fun getTotalCaloriesForDate(date: String): Int?

    @Query("""
        SELECT date, SUM(calories) as total
        FROM diary_item
        WHERE date IN (:dates)
        GROUP BY date
    """)
    suspend fun getCaloriesTotalsForDates(dates: List<String>): List<DateCaloriesRow>

    @Query("""
        SELECT SUM(protein) as protein, SUM(carbs) as carbs,
               SUM(fat) as fat, SUM(sugar) as sugar
        FROM diary_item WHERE date = :date
    """)
    suspend fun getMacrosForDate(date: String): MacroSumRow?

    /**
     * Returns total calories grouped by meal type for the given date.
     * Unlogged meal types simply won't appear in the result list.
     */
    @Query("""
        SELECT mealType, SUM(calories) as totalCalories
        FROM diary_item
        WHERE date = :date
        GROUP BY mealType
    """)
    suspend fun getCaloriesByMealType(date: String): List<MealTypeCaloriesRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DiaryItemEntity)

    @Update
    suspend fun update(item: DiaryItemEntity)

    @Query("DELETE FROM diary_item WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM diary_item WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("DELETE FROM diary_item")
    suspend fun deleteAll()
}

data class DateCaloriesRow(val date: String, val total: Int)

data class MealTypeCaloriesRow(val mealType: String, val totalCalories: Int)

data class MacroSumRow(
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float,
)

// ── FoodItemDao ──────────────────────────────────────────────────────────────

@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_item ORDER BY usageCount DESC")
    fun getAllFlow(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_item ORDER BY usageCount DESC")
    suspend fun getAll(): List<FoodItemEntity>

    @Query("SELECT * FROM food_item WHERE nameLower LIKE '%' || :query || '%' ORDER BY usageCount DESC")
    suspend fun search(query: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_item WHERE nameLower = :nameLower LIMIT 1")
    suspend fun getByNameLower(nameLower: String): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItemEntity)

    @Update
    suspend fun update(item: FoodItemEntity)

    @Query("DELETE FROM food_item WHERE nameLower = :nameLower")
    suspend fun deleteByNameLower(nameLower: String)

    @Query("DELETE FROM food_item")
    suspend fun deleteAll()
}

// ── StepsRecordDao ───────────────────────────────────────────────────────────

@Dao
interface StepsRecordDao {

    @Query("SELECT * FROM steps_record WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): StepsRecordEntity?

    @Query("SELECT * FROM steps_record WHERE date IN (:dates) ORDER BY date DESC")
    suspend fun getForDates(dates: List<String>): List<StepsRecordEntity>

    @Query("SELECT * FROM steps_record ORDER BY date DESC LIMIT :limit")
    fun getRecentFlow(limit: Int): Flow<List<StepsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StepsRecordEntity)

    @Query("DELETE FROM steps_record WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("DELETE FROM steps_record")
    suspend fun deleteAll()
}

// ── UserSettingsDao ───────────────────────────────────────────────────────────

@Dao
interface UserSettingsDao {

    /** Reactive stream of the singleton settings row. Emits defaults until the row exists. */
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<UserSettingsEntity?>

    /** Insert-or-replace the entire settings row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettingsEntity)

    @Query("UPDATE user_settings SET calorieGoal  = :v WHERE id = 1") suspend fun setCalorieGoal(v: Int)
    @Query("UPDATE user_settings SET stepGoal      = :v WHERE id = 1") suspend fun setStepGoal(v: Int)
    @Query("UPDATE user_settings SET weightLbs     = :v WHERE id = 1") suspend fun setWeightLbs(v: Float)
    @Query("UPDATE user_settings SET heightIn      = :v WHERE id = 1") suspend fun setHeightIn(v: Float)
    @Query("UPDATE user_settings SET age           = :v WHERE id = 1") suspend fun setAge(v: Int)
    @Query("UPDATE user_settings SET proteinGoalG  = :v WHERE id = 1") suspend fun setProteinGoalG(v: Int)
    @Query("UPDATE user_settings SET carbsGoalG    = :v WHERE id = 1") suspend fun setCarbsGoalG(v: Int)
    @Query("UPDATE user_settings SET fatGoalG      = :v WHERE id = 1") suspend fun setFatGoalG(v: Int)
    @Query("UPDATE user_settings SET sugarGoalG    = :v WHERE id = 1") suspend fun setSugarGoalG(v: Int)
    @Query("UPDATE user_settings SET nickname      = :v WHERE id = 1") suspend fun setNickname(v: String)
    @Query("UPDATE user_settings SET onboardingDone = :v WHERE id = 1") suspend fun setOnboardingDone(v: Boolean)
    @Query("UPDATE user_settings SET themeMode     = :v WHERE id = 1") suspend fun setThemeMode(v: String)
}
