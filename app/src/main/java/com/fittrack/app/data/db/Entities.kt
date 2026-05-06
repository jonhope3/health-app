package com.fittrack.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fittrack.app.data.MealType
import com.fittrack.app.data.ThemeMode

// ── Type Converters ──────────────────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)

    @TypeConverter
    fun fromLongList(value: List<Long>): String = value.joinToString(",")

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.toLongOrNull() }
}

// ── diary_item table ─────────────────────────────────────────────────────────

@Entity(tableName = "diary_item")
@TypeConverters(Converters::class)
data class DiaryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float,
    val timestamp: Long,
    /** ISO-8601 date string, e.g. "2025-02-23" — used to query by day */
    val date: String,
    val quantity: String?,
    val mealType: MealType,
    val foodItemId: String?,
)

// ── food_item table ──────────────────────────────────────────────────────────

@Entity(tableName = "food_item", indices = [Index(value = ["nameLower"], unique = true)])
@TypeConverters(Converters::class)
data class FoodItemEntity(
    @PrimaryKey val nameLower: String,   // lowercase normalised name — natural PK
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float,
    val servingDescription: String?,
    val usageCount: Int,
    val lastUsed: Long,
    val firstAdded: Long,
    val usageHistory: List<Long>,
)

// ── steps_record table ───────────────────────────────────────────────────────

@Entity(tableName = "steps_record")
data class StepsRecordEntity(
    @PrimaryKey val date: String,   // ISO-8601 date, e.g. "2025-02-23"
    val steps: Int,
)

// ── user_settings table (singleton row, id = 1) ───────────────────────────────
// Replaces DataStore — all user goals and preferences live here.
// Room gives us reactive Flow<> and transactional updates for free.

@Entity(tableName = "user_settings")
@TypeConverters(Converters::class)
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,          // always 1 — singleton row
    val calorieGoal: Int   = 2000,
    val stepGoal: Int      = 10_000,
    val weightLbs: Float   = 160f,
    val heightIn: Float    = 68f,
    val age: Int           = 30,
    val proteinGoalG: Int  = 100,
    val carbsGoalG: Int    = 250,
    val fatGoalG: Int      = 65,
    val sugarGoalG: Int    = 50,
    val nickname: String   = "",
    val onboardingDone: Boolean = false,
    /** LIGHT = always light  |  DARK = always dark  |  SYSTEM = follow OS */
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    // ── Family feature ──
    val familyEnabled: Boolean = true,
    val familyMode: String = "TRACKING",     // TRACKING or TTC
    val lastPeriodStart: String? = null,      // ISO-8601 date for initial setup
)

// ── cycle_record table ───────────────────────────────────────────────────────
// Tracks each menstrual cycle from period start to next period start.

@Entity(tableName = "cycle_record")
data class CycleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,            // ISO-8601 date — first day of period
    val endDate: String?,             // last day of period bleeding
    val cycleLength: Int?,            // days until next period started (null if current)
    val periodLength: Int?,           // days of bleeding
    val ovulationDate: String?,       // confirmed or estimated ovulation day
    val ovulationConfidence: String = "LOW", // LOW, MEDIUM, HIGH, VERY_HIGH
)

// ── daily_cycle_log table ────────────────────────────────────────────────────
// Per-day symptom/biomarker log — the heart of the tracking system.

@Entity(
    tableName = "daily_cycle_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyCycleLogEntity(
    @PrimaryKey val date: String,        // ISO-8601 date
    val cycleRecordId: Long? = null,     // FK to cycle_record (nullable for unlinked days)
    val cycleDay: Int? = null,           // day within current cycle (1-indexed)
    val phase: String? = null,           // MENSTRUAL, FOLLICULAR, OVULATORY, LUTEAL
    val flowIntensity: String? = null,   // SPOTTING, LIGHT, MEDIUM, HEAVY
    val cervicalMucus: String? = null,   // DRY, STICKY, CREAMY, WATERY, EGG_WHITE
    val symptoms: String = "",           // comma-separated: "CRAMPS,BLOATING"
    val mood: String? = null,            // GOOD, NEUTRAL, LOW, IRRITABLE, ENERGETIC
    val sexDrive: String? = null,        // LOW, NORMAL, HIGH
    val sexualActivity: String? = null,  // PROTECTED, UNPROTECTED, NONE
    val temperature: Float? = null,      // resolved temp for the day (°F)
    val temperatureSource: String? = null, // MANUAL, HEALTH_CONNECT_SKIN
)

// ── temperature_reading table ────────────────────────────────────────────────
// Raw temperature readings (may have multiple per day from different sources).

@Entity(
    tableName = "temperature_reading",
    indices = [Index(value = ["date", "source"])]
)
data class TemperatureReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,             // ISO-8601 date
    val temperatureF: Float,      // stored in Fahrenheit
    val source: String,           // MANUAL, HEALTH_CONNECT_SKIN
    val timestamp: Long,          // epoch millis of the reading
)

