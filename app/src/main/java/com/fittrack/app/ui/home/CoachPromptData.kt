package com.fittrack.app.ui.home

import java.time.LocalTime

data class CoachPromptData(
        val caloriesEaten: Int,
        val calorieGoal: Int,
        val protein: Float,
        val carbs: Float,
        val fat: Float,
        val sugar: Float,
        val sugarGoal: Int,
        val steps: Int,
        val stepGoal: Int,
        val nickname: String,
        val now: LocalTime = LocalTime.now()
)
