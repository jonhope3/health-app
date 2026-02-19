package com.fittrack.app.data

import kotlinx.serialization.Serializable

@Serializable
data class FoodEntry(
    val id: String,
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val timestamp: Long,
    val quantity: String? = null
)

@Serializable
data class NutritionResult(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingDescription: String? = null
)

@Serializable
data class CustomFood(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingDescription: String? = null,
    val usageCount: Int,
    val lastUsed: Long
)

@Serializable
data class ParsedFoodItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val quantity: String = "1 serving"
)
