package com.fittrack.app.data

import android.content.Context

class GoalsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("fittrack_goals", Context.MODE_PRIVATE)

    fun getCalorieGoal(): Int = prefs.getInt("calorie_goal", 2000)
    fun setCalorieGoal(goal: Int) { prefs.edit().putInt("calorie_goal", goal).apply() }

    fun getStepGoal(): Int = prefs.getInt("step_goal", 10000)
    fun setStepGoal(goal: Int) { prefs.edit().putInt("step_goal", goal).apply() }

    fun getWeightLbs(): Float = prefs.getFloat("weight_lbs", 160f)
    fun setWeightLbs(w: Float) { prefs.edit().putFloat("weight_lbs", w).apply() }

    fun getHeightIn(): Float = prefs.getFloat("height_in", 68f)
    fun setHeightIn(h: Float) { prefs.edit().putFloat("height_in", h).apply() }

    fun getNickname(): String = prefs.getString("nickname", "") ?: ""
    fun setNickname(name: String) { prefs.edit().putString("nickname", name.trim()).apply() }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("onboarding_done", false)
    fun setOnboardingCompleted() { prefs.edit().putBoolean("onboarding_done", true).apply() }
}
