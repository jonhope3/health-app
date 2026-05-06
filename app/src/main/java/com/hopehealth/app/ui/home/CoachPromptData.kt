package com.hopehealth.app.ui.home

import java.time.LocalTime

data class CoachPromptData(
        val caloriesEaten: Int,
        val calorieGoal: Int,
        val protein: Float,
        val proteinGoal: Int,
        val carbs: Float,
        val carbsGoal: Int,
        val fat: Float,
        val fatGoal: Int,
        val sugar: Float,
        val sugarGoal: Int,
        val steps: Int,
        val stepGoal: Int,
        val nickname: String,
        val now: LocalTime = LocalTime.now()
)
