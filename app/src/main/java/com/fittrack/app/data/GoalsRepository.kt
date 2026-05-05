package com.fittrack.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property — creates a single DataStore per Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_goals")

class GoalsRepository(context: Context) {

    private val dataStore = context.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────

    companion object Keys {
        val CALORIE_GOAL    = intPreferencesKey("calorie_goal")
        val STEP_GOAL       = intPreferencesKey("step_goal")
        val WEIGHT_LBS      = floatPreferencesKey("weight_lbs")
        val HEIGHT_IN       = floatPreferencesKey("height_in")
        val AGE             = intPreferencesKey("age")
        val PROTEIN_GOAL_G  = intPreferencesKey("protein_goal_g")
        val CARBS_GOAL_G    = intPreferencesKey("carbs_goal_g")
        val FAT_GOAL_G      = intPreferencesKey("fat_goal_g")
        val SUGAR_GOAL_G    = intPreferencesKey("sugar_goal_g")
        val NICKNAME        = stringPreferencesKey("nickname")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    // ── Flows ────────────────────────────────────────────────────────────────

    val calorieGoalFlow: Flow<Int>    = dataStore.data.map { it[CALORIE_GOAL] ?: 2000 }
    val stepGoalFlow: Flow<Int>       = dataStore.data.map { it[STEP_GOAL] ?: 10000 }
    val weightLbsFlow: Flow<Float>    = dataStore.data.map { it[WEIGHT_LBS] ?: 160f }
    val heightInFlow: Flow<Float>     = dataStore.data.map { it[HEIGHT_IN] ?: 68f }
    val ageFlow: Flow<Int>            = dataStore.data.map { it[AGE] ?: 30 }
    val proteinGoalFlow: Flow<Int>    = dataStore.data.map { it[PROTEIN_GOAL_G] ?: 100 }
    val carbsGoalFlow: Flow<Int>      = dataStore.data.map { it[CARBS_GOAL_G] ?: 250 }
    val fatGoalFlow: Flow<Int>        = dataStore.data.map { it[FAT_GOAL_G] ?: 65 }
    val sugarGoalFlow: Flow<Int>      = dataStore.data.map { it[SUGAR_GOAL_G] ?: 50 }
    val nicknameFlow: Flow<String>    = dataStore.data.map { it[NICKNAME] ?: "" }
    val onboardingDoneFlow: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    // ── One-shot reads (suspend) ──────────────────────────────────────────────

    suspend fun getCalorieGoal(): Int  = calorieGoalFlow.first()
    suspend fun getStepGoal(): Int     = stepGoalFlow.first()
    suspend fun getWeightLbs(): Float  = weightLbsFlow.first()
    suspend fun getHeightIn(): Float   = heightInFlow.first()
    suspend fun getAge(): Int          = ageFlow.first()
    suspend fun getProteinGoalG(): Int = proteinGoalFlow.first()
    suspend fun getCarbsGoalG(): Int   = carbsGoalFlow.first()
    suspend fun getFatGoalG(): Int     = fatGoalFlow.first()
    suspend fun getSugarGoalG(): Int   = sugarGoalFlow.first()
    suspend fun getNickname(): String  = nicknameFlow.first()
    suspend fun hasCompletedOnboarding(): Boolean = onboardingDoneFlow.first()

    // ── Writes ───────────────────────────────────────────────────────────────

    suspend fun setCalorieGoal(goal: Int)   = dataStore.edit { it[CALORIE_GOAL] = goal }
    suspend fun setStepGoal(goal: Int)      = dataStore.edit { it[STEP_GOAL] = goal }
    suspend fun setWeightLbs(w: Float)      = dataStore.edit { it[WEIGHT_LBS] = w }
    suspend fun setHeightIn(h: Float)       = dataStore.edit { it[HEIGHT_IN] = h }
    suspend fun setAge(age: Int)            = dataStore.edit { it[AGE] = age }
    suspend fun setProteinGoalG(g: Int)     = dataStore.edit { it[PROTEIN_GOAL_G] = g }
    suspend fun setCarbsGoalG(g: Int)       = dataStore.edit { it[CARBS_GOAL_G] = g }
    suspend fun setFatGoalG(g: Int)         = dataStore.edit { it[FAT_GOAL_G] = g }
    suspend fun setSugarGoalG(g: Int)       = dataStore.edit { it[SUGAR_GOAL_G] = g }
    suspend fun setNickname(name: String)   = dataStore.edit { it[NICKNAME] = name.trim() }
    suspend fun setOnboardingCompleted()    = dataStore.edit { it[ONBOARDING_DONE] = true }

    /** Wipe everything (used by Reset & Start Fresh). */
    suspend fun clearAll() = dataStore.edit { it.clear() }
}
