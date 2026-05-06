package com.fittrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.ThemeMode
import com.fittrack.app.data.db.DiaryItemDao
import com.fittrack.app.data.db.FoodItemDao
import com.fittrack.app.data.db.StepsRecordDao
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A single row in the database explorer.
 * [fields] is an ordered map of column name → display value for that row.
 */
data class DbEntry(
    val table: String,
    val fields: Map<String, String>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val goalsRepository: GoalsRepository,
    private val diaryItemDao: DiaryItemDao,
    private val foodItemDao: FoodItemDao,
    private val stepsRecordDao: StepsRecordDao,
    private val geminiNanoService: GeminiNanoService,
) : ViewModel() {

    private val healthConnectService = HealthConnectService()
    private val json = Json { ignoreUnknownKeys = true }

    private val _healthConnectGranted = MutableStateFlow(false)
    private val _calorieGoal          = MutableStateFlow("")
    private val _stepGoal             = MutableStateFlow("")
    private val _weightLbs            = MutableStateFlow("")
    private val _heightFt             = MutableStateFlow("")
    private val _heightIn             = MutableStateFlow("")
    private val _age                  = MutableStateFlow("")
    private val _nickname             = MutableStateFlow("")
    private val _proteinGoal          = MutableStateFlow("")
    private val _carbsGoal            = MutableStateFlow("")
    private val _fatGoal              = MutableStateFlow("")
    private val _sugarGoal            = MutableStateFlow("")
    private val _isGeneratingMacros   = MutableStateFlow(false)
    private val _macroGenerateError   = MutableStateFlow<String?>(null)
    private val _geminiReady          = MutableStateFlow(false)
    private val _dbEntries            = MutableStateFlow<List<DbEntry>>(emptyList())

    val healthConnectGranted: StateFlow<Boolean>      = _healthConnectGranted.asStateFlow()
    val calorieGoal:          StateFlow<String>       = _calorieGoal.asStateFlow()
    val stepGoal:             StateFlow<String>       = _stepGoal.asStateFlow()
    val weightLbs:            StateFlow<String>       = _weightLbs.asStateFlow()
    val heightFt:             StateFlow<String>       = _heightFt.asStateFlow()
    val heightIn:             StateFlow<String>       = _heightIn.asStateFlow()
    val age:                  StateFlow<String>       = _age.asStateFlow()
    val nickname:             StateFlow<String>       = _nickname.asStateFlow()
    val proteinGoal:          StateFlow<String>       = _proteinGoal.asStateFlow()
    val carbsGoal:            StateFlow<String>       = _carbsGoal.asStateFlow()
    val fatGoal:              StateFlow<String>       = _fatGoal.asStateFlow()
    val sugarGoal:            StateFlow<String>       = _sugarGoal.asStateFlow()
    val isGeneratingMacros:   StateFlow<Boolean>      = _isGeneratingMacros.asStateFlow()
    val macroGenerateError:   StateFlow<String?>      = _macroGenerateError.asStateFlow()
    val geminiReady:          StateFlow<Boolean>      = _geminiReady.asStateFlow()
    val dbEntries:            StateFlow<List<DbEntry>> = _dbEntries.asStateFlow()

    /** Reactive theme mode — drives the 3-way picker in Settings UI. */
    val themeMode: StateFlow<ThemeMode> = goalsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.LIGHT)

    init {
        loadData()
        viewModelScope.launch { _geminiReady.value = geminiNanoService.initIfNeeded() }
    }

    fun onHealthConnectResult(granted: Set<String>) {
        _healthConnectGranted.value = granted.isNotEmpty()
    }

    fun loadData() {
        viewModelScope.launch {
            _calorieGoal.value = goalsRepository.getCalorieGoal().toString()
            _stepGoal.value    = goalsRepository.getStepGoal().toString()
            _weightLbs.value   = formatFloat(goalsRepository.getWeightLbs())
            _age.value         = goalsRepository.getAge().toString()
            _nickname.value    = goalsRepository.getNickname()
            _proteinGoal.value = goalsRepository.getProteinGoalG().toString()
            _carbsGoal.value   = goalsRepository.getCarbsGoalG().toString()
            _fatGoal.value     = goalsRepository.getFatGoalG().toString()
            _sugarGoal.value   = goalsRepository.getSugarGoalG().toString()

            val totalInches = goalsRepository.getHeightIn()
            _heightFt.value = (totalInches / 12).toInt().toString()
            _heightIn.value = (totalInches % 12).toInt().toString()
        }
    }

    // ── Field setters — filter to valid character sets ────────────────────────

    fun setCalorieGoal(v: String) { _calorieGoal.value = v.filter(Char::isDigit) }
    fun setStepGoal(v: String)    { _stepGoal.value    = v.filter(Char::isDigit) }
    fun setWeightLbs(v: String)   { _weightLbs.value   = v.filter { it.isDigit() || it == '.' } }
    fun setHeightFt(v: String)    { _heightFt.value    = v.filter(Char::isDigit) }
    fun setHeightIn(v: String)    { _heightIn.value    = v.filter(Char::isDigit) }
    fun setAge(v: String)         { _age.value         = v.filter(Char::isDigit) }
    fun setNickname(v: String)    { _nickname.value    = v }
    fun setProteinGoal(v: String) { _proteinGoal.value = v.filter(Char::isDigit) }
    fun setCarbsGoal(v: String)   { _carbsGoal.value   = v.filter(Char::isDigit) }
    fun setFatGoal(v: String)     { _fatGoal.value     = v.filter(Char::isDigit) }
    fun setSugarGoal(v: String)   { _sugarGoal.value   = v.filter(Char::isDigit) }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { goalsRepository.setThemeMode(mode) }
    }

    // ── Persist ───────────────────────────────────────────────────────────────

    fun save() {
        viewModelScope.launch {
            _calorieGoal.value.toIntOrNull()?.takeIf { it in 500..10000 }
                ?.let { goalsRepository.setCalorieGoal(it) }
            _stepGoal.value.toIntOrNull()?.takeIf { it in 100..200000 }
                ?.let { goalsRepository.setStepGoal(it) }
            _weightLbs.value.toFloatOrNull()?.takeIf { it in 50f..700f }
                ?.let { goalsRepository.setWeightLbs(it) }
            val totalInches = (_heightFt.value.toIntOrNull() ?: 0) * 12f +
                              (_heightIn.value.toIntOrNull() ?: 0)
            if (totalInches in 36f..96f) goalsRepository.setHeightIn(totalInches)
            _age.value.toIntOrNull()?.takeIf { it in 1..120 }
                ?.let { goalsRepository.setAge(it) }
            _nickname.value.trim().takeIf { it.isNotEmpty() }
                ?.let { goalsRepository.setNickname(it) }
        }
    }

    fun saveMacroGoals() {
        viewModelScope.launch {
            _proteinGoal.value.toIntOrNull()?.takeIf { it in 0..500 }
                ?.let { goalsRepository.setProteinGoalG(it) }
            _carbsGoal.value.toIntOrNull()?.takeIf { it in 0..1000 }
                ?.let { goalsRepository.setCarbsGoalG(it) }
            _fatGoal.value.toIntOrNull()?.takeIf { it in 0..500 }
                ?.let { goalsRepository.setFatGoalG(it) }
            _sugarGoal.value.toIntOrNull()?.takeIf { it in 0..500 }
                ?.let { goalsRepository.setSugarGoalG(it) }
        }
    }

    /**
     * Formula-based macro estimator — works on any device, no AI required.
     *
     * Uses Mifflin-St Jeor BMR and standard dietary macro splits:
     *   Protein : 0.7–1.0 g per lb bodyweight (higher if more active)
     *   Fat     : 25% of calorie goal → grams = (cals * 0.25) / 9
     *   Carbs   : remaining calories → grams = remainder / 4
     *   Sugar   : WHO recommendation ≤ 10% of total calories → grams = (cals * 0.10) / 4
     *
     * Falls back to the calorie goal alone if body measurements aren't set.
     */
    fun generateLocalMacroGoals() {
        viewModelScope.launch {
            _isGeneratingMacros.value = true
            _macroGenerateError.value = null

            val cals      = _calorieGoal.value.toIntOrNull()
                            ?: goalsRepository.getCalorieGoal().takeIf { it > 0 }
                            ?: 2000
            val weightLbs = _weightLbs.value.toFloatOrNull()
                            ?: goalsRepository.getWeightLbs().takeIf { it > 0f }
            val totalIn   = (_heightFt.value.toIntOrNull() ?: 0) * 12 +
                            (_heightIn.value.toIntOrNull() ?: 0)
            val age       = _age.value.toIntOrNull() ?: goalsRepository.getAge()

            // Protein: 0.8g/lb if we have weight, else 30% of calories / 4
            val proteinG = if (weightLbs != null && weightLbs > 0f) {
                (weightLbs * 0.8f).toInt().coerceIn(50, 250)
            } else {
                ((cals * 0.30f) / 4f).toInt()
            }

            // Fat: 25% of total calories, 9 kcal/g
            val fatG = ((cals * 0.25f) / 9f).toInt().coerceIn(30, 150)

            // Carbs: fill remaining calories after protein + fat, 4 kcal/g
            val remainingCals = (cals - proteinG * 4 - fatG * 9).coerceAtLeast(0)
            val carbsG = (remainingCals / 4).coerceIn(50, 400)

            // Sugar: WHO ≤10% of total calories, 4 kcal/g
            val sugarG = ((cals * 0.10f) / 4f).toInt().coerceIn(20, 80)

            _proteinGoal.value = proteinG.toString()
            _carbsGoal.value   = carbsG.toString()
            _fatGoal.value     = fatG.toString()
            _sugarGoal.value   = sugarG.toString()
            saveMacroGoals()

            val source = if (weightLbs != null) "weight & calorie goal" else "calorie goal"
            _macroGenerateError.value = null  // clear any prior error
            // Brief confirmation — surfaced in UI via a separate success message state
            _isGeneratingMacros.value = false
        }
    }

    fun generateAiMacroGoals() {
        viewModelScope.launch {
            _isGeneratingMacros.value = true
            _macroGenerateError.value = null

            if (!geminiNanoService.initIfNeeded()) {
                // Fall back to local formula instead of showing an error
                _isGeneratingMacros.value = false
                generateLocalMacroGoals()
                return@launch
            }

            runCatching {
                val cals      = _calorieGoal.value.toIntOrNull() ?: goalsRepository.getCalorieGoal()
                val weightLbs = _weightLbs.value.toFloatOrNull() ?: goalsRepository.getWeightLbs()
                val totalIn   = (_heightFt.value.toIntOrNull() ?: 0) * 12 +
                                (_heightIn.value.toIntOrNull() ?: 0)
                val age       = _age.value.toIntOrNull() ?: goalsRepository.getAge()

                val prompt = buildMacroPrompt(cals, weightLbs.toInt(), totalIn, age)
                val response = geminiNanoService.generateContent(prompt)

                if (response.isBlank()) {
                    _macroGenerateError.value = "No response — try again"
                    return@runCatching
                }

                val obj = Regex("""\{[\s\S]*?}""").find(response)
                    ?.let { runCatching { json.parseToJsonElement(it.value).jsonObject }.getOrNull() }

                if (obj == null) {
                    _macroGenerateError.value = "Couldn't parse AI response — try again"
                    return@runCatching
                }

                val intField: (String) -> Int? = { key ->
                    obj[key]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
                }
                intField("protein")?.let { _proteinGoal.value = it.toString() }
                intField("carbs")?.let   { _carbsGoal.value   = it.toString() }
                intField("fat")?.let     { _fatGoal.value     = it.toString() }
                intField("sugar")?.let   { _sugarGoal.value   = it.toString() }
                saveMacroGoals()
            }.onFailure { e ->
                _macroGenerateError.value = "Error: ${e.message}"
            }

            _isGeneratingMacros.value = false
        }
    }

    /**
     * Clears all Room tables and resets user settings, then resets the UI fields.
     * Intended for the "Reset & Start Fresh" action.
     */
    fun nukeDb() {
        viewModelScope.launch {
            goalsRepository.clearAll()
            // clearAllTables() runs within Room's executor on a background thread
            diaryItemDao.deleteAll()
            foodItemDao.deleteAll()
            stepsRecordDao.deleteAll()
            loadData()
        }
    }

    /**
     * Populates [dbEntries] with a snapshot of the last 30 days from each Room table
     * and all DataStore goal values. Called when the user opens the DB Explorer.
     */
    fun loadDbOverview() {
        viewModelScope.launch {
            val recentDates = List(30) { i ->
                LocalDate.now().minusDays(i.toLong()).toString()
            }
            val list = mutableListOf<DbEntry>()

            // diary_item — most recent 30 days
            val allDiary = recentDates.flatMap { diaryItemDao.getDiaryForDate(it) }
            if (allDiary.isEmpty()) {
                list += DbEntry("diary_item", mapOf("(empty)" to "No diary entries in the last 30 days"))
            } else {
                allDiary.forEach { e ->
                    list += DbEntry(
                        table  = "diary_item",
                        fields = linkedMapOf(
                            "date"     to e.date,
                            "name"     to e.name,
                            "calories" to "${e.calories} kcal",
                            "protein"  to "${e.protein}g",
                            "carbs"    to "${e.carbs}g",
                            "fat"      to "${e.fat}g",
                            "meal"     to e.mealType.name.lowercase(),
                        ),
                    )
                }
            }

            // food_item
            val foods = foodItemDao.getAll()
            if (foods.isEmpty()) {
                list += DbEntry("food_item", mapOf("(empty)" to "No custom food items saved"))
            } else {
                foods.forEach { f ->
                    list += DbEntry(
                        table  = "food_item",
                        fields = linkedMapOf(
                            "name"     to f.name,
                            "calories" to "${f.calories} kcal",
                            "protein"  to "${f.protein}g",
                            "carbs"    to "${f.carbs}g",
                            "fat"      to "${f.fat}g",
                            "used"     to "${f.usageCount}×",
                        ),
                    )
                }
            }

            // steps_record
            val steps = stepsRecordDao.getForDates(recentDates)
            if (steps.isEmpty()) {
                list += DbEntry("steps_record", mapOf("(empty)" to "No step records found"))
            } else {
                steps.forEach { s ->
                    list += DbEntry(
                        table  = "steps_record",
                        fields = linkedMapOf(
                            "date"  to s.date,
                            "steps" to s.steps.toString(),
                        ),
                    )
                }
            }

            // user_settings — goals from Room
            val goals = linkedMapOf(
                "calorie_goal" to goalsRepository.getCalorieGoal().toString(),
                "step_goal"    to goalsRepository.getStepGoal().toString(),
                "weight_lbs"   to goalsRepository.getWeightLbs().toString(),
                "height_in"    to goalsRepository.getHeightIn().toString(),
                "age"          to goalsRepository.getAge().toString(),
                "nickname"     to goalsRepository.getNickname().ifBlank { "(not set)" },
                "protein_goal" to goalsRepository.getProteinGoalG().toString(),
                "carbs_goal"   to goalsRepository.getCarbsGoalG().toString(),
                "fat_goal"     to goalsRepository.getFatGoalG().toString(),
                "sugar_goal"   to goalsRepository.getSugarGoalG().toString(),
                "theme_mode"   to goalsRepository.getThemeMode().name,
            )
            // Each goal key is its own row with "key" and "value" columns
            goals.forEach { (k, v) ->
                list += DbEntry(
                    table  = "user_settings",
                    fields = linkedMapOf("key" to k, "value" to v),
                )
            }

            _dbEntries.value = list.sortedBy { it.table }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun formatFloat(v: Float): String =
        if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)

    private fun buildMacroPrompt(cals: Int, weightLbs: Int, heightIn: Int, age: Int) = """
        <instruction>
        You are a registered dietitian. Recommend daily macro intake goals for this user.
        Return ONLY a JSON object with integer values in grams: protein, carbs, fat, sugar.
        Base recommendations on the Dietary Guidelines for Americans and ISSN protein recommendations.
        Return ONLY the JSON object — no other text.
        </instruction>

        <user_profile>
        Age: $age years
        Weight: $weightLbs lbs
        Height: $heightIn inches
        Daily calorie goal: $cals kcal
        </user_profile>

        <example_output>
        {"protein":120,"carbs":225,"fat":65,"sugar":50}
        </example_output>
    """.trimIndent()
}
