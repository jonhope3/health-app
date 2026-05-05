package com.fittrack.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fittrack.app.data.MealType

// ── Type Converters ──────────────────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromMealType(value: MealType): String = value.name

    @TypeConverter
    fun toMealType(value: String): MealType = MealType.valueOf(value)

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
