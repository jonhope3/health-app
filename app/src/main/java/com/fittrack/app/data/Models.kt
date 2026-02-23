package com.fittrack.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, OTHER
}

@Serializable
data class DiaryItem(
    val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val timestamp: Long,
    val quantity: String? = null,
    val mealType: MealType = MealType.OTHER,
    val foodItemId: String? = null
)

@Serializable
data class NutritionResult(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val servingDescription: String? = null,
    val confidence: Float? = null
)

@Serializable
data class FoodItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val servingDescription: String? = null,
    val usageCount: Int,
    val lastUsed: Long,
    val firstAdded: Long = lastUsed,
    val usageHistory: List<Long> = emptyList()
)

@Serializable
data class ParsedFoodItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val quantity: String = "1 serving",
    val confidence: Float? = null
)
