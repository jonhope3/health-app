package com.hopehealth.app.data

// ── Domain models (no Room annotations — pure data layer) ────────────────────

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, OTHER
}

/** Controls app color scheme. Stored in Room `user_settings`. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

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
    val foodItemId: String? = null,
)

data class NutritionResult(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val servingDescription: String? = null,
    val confidence: Float? = null,
)

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
    val usageHistory: List<Long> = emptyList(),
)

data class ParsedFoodItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val sugar: Float = 0f,
    val quantity: String = "1 serving",
    val confidence: Float? = null,
)

// ── Family / Cycle Tracking domain models ────────────────────────────────────

enum class CyclePhase { MENSTRUAL, FOLLICULAR, OVULATORY, LUTEAL }
enum class FlowIntensity { SPOTTING, LIGHT, MEDIUM, HEAVY }
enum class CervicalMucusType { DRY, STICKY, CREAMY, WATERY, EGG_WHITE }
enum class MoodType { GOOD, NEUTRAL, LOW, IRRITABLE, ENERGETIC }
enum class SexDriveLevel { LOW, NORMAL, HIGH }
enum class SexualActivityType { PROTECTED, UNPROTECTED, NONE }
enum class PredictionConfidence { LOW, MEDIUM, HIGH, VERY_HIGH }
enum class FamilyMode { TRACKING, TTC }

enum class Symptom {
    CRAMPS, BLOATING, HEADACHE, BREAST_TENDERNESS, FATIGUE
}
