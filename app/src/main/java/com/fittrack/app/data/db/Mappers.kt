package com.fittrack.app.data.db

import com.fittrack.app.data.DiaryItem
import com.fittrack.app.data.FoodItem
import java.time.Instant
import java.time.ZoneId

// ── Domain → Entity ──────────────────────────────────────────────────────────

fun DiaryItem.toEntity(): DiaryItemEntity {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
    return DiaryItemEntity(
        id = id,
        name = name,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        sugar = sugar,
        timestamp = timestamp,
        date = date,
        quantity = quantity,
        mealType = mealType,
        foodItemId = foodItemId,
    )
}

fun FoodItem.toEntity(): FoodItemEntity = FoodItemEntity(
    nameLower = name.lowercase(),
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    sugar = sugar,
    servingDescription = servingDescription,
    usageCount = usageCount,
    lastUsed = lastUsed,
    firstAdded = firstAdded,
    usageHistory = usageHistory,
)

// ── Entity → Domain ──────────────────────────────────────────────────────────

fun DiaryItemEntity.toDomain() = DiaryItem(
    id = id,
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    sugar = sugar,
    timestamp = timestamp,
    quantity = quantity,
    mealType = mealType,
    foodItemId = foodItemId,
)

fun FoodItemEntity.toDomain() = FoodItem(
    name = name,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    sugar = sugar,
    servingDescription = servingDescription,
    usageCount = usageCount,
    lastUsed = lastUsed,
    firstAdded = firstAdded,
    usageHistory = usageHistory,
)
