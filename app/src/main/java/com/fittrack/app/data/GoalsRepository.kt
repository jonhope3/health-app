package com.fittrack.app.data

import com.fittrack.app.data.db.UserSettingsDao
import com.fittrack.app.data.db.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all user goals and preferences.
 *
 * Backed by Room [UserSettingsDao] — reactive via Flow, no DataStore dependency.
 * The settings table holds exactly one row (id = 1); we insert defaults on first access.
 */
@Singleton
class GoalsRepository @Inject constructor(private val dao: UserSettingsDao) {

    // ── Reactive flows ────────────────────────────────────────────────────────

    private val settingsFlow: Flow<UserSettingsEntity> =
        dao.observe().map { it ?: UserSettingsEntity() }

    val calorieGoalFlow:    Flow<Int>       = settingsFlow.map { it.calorieGoal }
    val stepGoalFlow:       Flow<Int>       = settingsFlow.map { it.stepGoal }
    val weightLbsFlow:      Flow<Float>     = settingsFlow.map { it.weightLbs }
    val heightInFlow:       Flow<Float>     = settingsFlow.map { it.heightIn }
    val ageFlow:            Flow<Int>       = settingsFlow.map { it.age }
    val proteinGoalFlow:    Flow<Int>       = settingsFlow.map { it.proteinGoalG }
    val carbsGoalFlow:      Flow<Int>       = settingsFlow.map { it.carbsGoalG }
    val fatGoalFlow:        Flow<Int>       = settingsFlow.map { it.fatGoalG }
    val sugarGoalFlow:      Flow<Int>       = settingsFlow.map { it.sugarGoalG }
    val nicknameFlow:       Flow<String>    = settingsFlow.map { it.nickname }
    val onboardingDoneFlow: Flow<Boolean>   = settingsFlow.map { it.onboardingDone }
    val themeModeFlow:      Flow<ThemeMode> = settingsFlow.map { it.themeMode }

    // ── One-shot reads ────────────────────────────────────────────────────────

    suspend fun getCalorieGoal():  Int      = calorieGoalFlow.first()
    suspend fun getStepGoal():     Int      = stepGoalFlow.first()
    suspend fun getWeightLbs():    Float    = weightLbsFlow.first()
    suspend fun getHeightIn():     Float    = heightInFlow.first()
    suspend fun getAge():          Int      = ageFlow.first()
    suspend fun getProteinGoalG(): Int      = proteinGoalFlow.first()
    suspend fun getCarbsGoalG():   Int      = carbsGoalFlow.first()
    suspend fun getFatGoalG():     Int      = fatGoalFlow.first()
    suspend fun getSugarGoalG():   Int      = sugarGoalFlow.first()
    suspend fun getNickname():     String   = nicknameFlow.first()
    suspend fun getThemeMode():    ThemeMode = themeModeFlow.first()
    suspend fun hasCompletedOnboarding(): Boolean = onboardingDoneFlow.first()

    // ── Writes ────────────────────────────────────────────────────────────────

    /** Ensure the singleton row exists before attempting targeted UPDATE queries. */
    private suspend fun ensureRow() {
        if (dao.observe().first() == null) dao.upsert(UserSettingsEntity())
    }

    suspend fun setCalorieGoal(goal: Int)  { ensureRow(); dao.setCalorieGoal(goal) }
    suspend fun setStepGoal(goal: Int)     { ensureRow(); dao.setStepGoal(goal) }
    suspend fun setWeightLbs(w: Float)     { ensureRow(); dao.setWeightLbs(w) }
    suspend fun setHeightIn(h: Float)      { ensureRow(); dao.setHeightIn(h) }
    suspend fun setAge(age: Int)           { ensureRow(); dao.setAge(age) }
    suspend fun setProteinGoalG(g: Int)    { ensureRow(); dao.setProteinGoalG(g) }
    suspend fun setCarbsGoalG(g: Int)      { ensureRow(); dao.setCarbsGoalG(g) }
    suspend fun setFatGoalG(g: Int)        { ensureRow(); dao.setFatGoalG(g) }
    suspend fun setSugarGoalG(g: Int)      { ensureRow(); dao.setSugarGoalG(g) }
    suspend fun setNickname(name: String)  { ensureRow(); dao.setNickname(name.trim()) }
    suspend fun setOnboardingCompleted()   { ensureRow(); dao.setOnboardingDone(true) }
    suspend fun setThemeMode(mode: ThemeMode) { ensureRow(); dao.setThemeMode(mode.name) }

    /** Wipe all user goals (Reset & Start Fresh). */
    suspend fun clearAll() = dao.upsert(UserSettingsEntity())
}
