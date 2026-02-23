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

    fun getAge(): Int = prefs.getInt("age", 30)
    fun setAge(age: Int) { prefs.edit().putInt("age", age).apply() }

    // Macro goals in grams. Defaults based on a 2000-cal balanced diet.
    fun getProteinGoalG(): Int = prefs.getInt("protein_goal_g", 100)
    fun setProteinGoalG(g: Int) { prefs.edit().putInt("protein_goal_g", g).apply() }

    fun getCarbsGoalG(): Int = prefs.getInt("carbs_goal_g", 250)
    fun setCarbsGoalG(g: Int) { prefs.edit().putInt("carbs_goal_g", g).apply() }

    fun getFatGoalG(): Int = prefs.getInt("fat_goal_g", 65)
    fun setFatGoalG(g: Int) { prefs.edit().putInt("fat_goal_g", g).apply() }

    fun getSugarGoalG(): Int = prefs.getInt("sugar_goal_g", 50)
    fun setSugarGoalG(g: Int) { prefs.edit().putInt("sugar_goal_g", g).apply() }

    fun getNickname(): String = prefs.getString("nickname", "") ?: ""
    fun setNickname(name: String) { prefs.edit().putString("nickname", name.trim()).apply() }

    fun hasCompletedOnboarding(): Boolean = prefs.getBoolean("onboarding_done", false)
    fun setOnboardingCompleted() { prefs.edit().putBoolean("onboarding_done", true).apply() }
}
