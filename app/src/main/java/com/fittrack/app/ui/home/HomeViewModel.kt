package com.fittrack.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import com.fittrack.app.services.PedometerService
import com.fittrack.app.util.todayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)
    private val foodRepository = FoodRepository(application)
    private val stepsRepository = StepsRepository(application)
    private val healthConnectService = HealthConnectService()
    private val pedometerService = PedometerService()
    private val geminiNanoService = GeminiNanoService()

    private val _calorieGoal = MutableStateFlow(2000)
    val calorieGoal: StateFlow<Int> = _calorieGoal.asStateFlow()

    private val _stepGoal = MutableStateFlow(10000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _caloriesEaten = MutableStateFlow(0)
    val caloriesEaten: StateFlow<Int> = _caloriesEaten.asStateFlow()

    private val _protein = MutableStateFlow(0f)
    val protein: StateFlow<Float> = _protein.asStateFlow()

    private val _carbs = MutableStateFlow(0f)
    val carbs: StateFlow<Float> = _carbs.asStateFlow()

    private val _fat = MutableStateFlow(0f)
    val fat: StateFlow<Float> = _fat.asStateFlow()

    private val _sugar = MutableStateFlow(0f)
    val sugar: StateFlow<Float> = _sugar.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    // Macro goals in grams (from Settings)
    private val _proteinGoalG = MutableStateFlow(100)
    val proteinGoalG: StateFlow<Int> = _proteinGoalG.asStateFlow()

    private val _carbsGoalG = MutableStateFlow(250)
    val carbsGoalG: StateFlow<Int> = _carbsGoalG.asStateFlow()

    private val _fatGoalG = MutableStateFlow(65)
    val fatGoalG: StateFlow<Int> = _fatGoalG.asStateFlow()

    private val _sugarGoalG = MutableStateFlow(50)
    val sugarGoalG: StateFlow<Int> = _sugarGoalG.asStateFlow()

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _coachTip = MutableStateFlow("")
    val coachTip: StateFlow<String> = _coachTip.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _calorieGoal.value = goalsRepository.getCalorieGoal()
            _stepGoal.value = goalsRepository.getStepGoal()
            _caloriesEaten.value = foodRepository.getTotalCaloriesToday()
            _nickname.value = goalsRepository.getNickname()
            val macros = foodRepository.getTodayMacros()
            _protein.value = macros.protein
            _carbs.value = macros.carbs
            _fat.value = macros.fat
            _sugar.value = macros.sugar
            _proteinGoalG.value = goalsRepository.getProteinGoalG()
            _carbsGoalG.value = goalsRepository.getCarbsGoalG()
            _fatGoalG.value = goalsRepository.getFatGoalG()
            _sugarGoalG.value = goalsRepository.getSugarGoalG()

            val app = getApplication<Application>()
            val today = todayKey()

            try {
                if (healthConnectService.isAvailable(app) && healthConnectService.initialize(app)) {
                    val stepsToday = healthConnectService.readStepsToday()
                    _steps.value = stepsToday
                    stepsRepository.saveSteps(stepsToday, today)

                    val history = healthConnectService.readStepsHistory(7)
                    history.forEach { (date, count) -> stepsRepository.saveSteps(count, date) }

                    // Always estimate from steps — HC's ActiveCaloriesBurnedRecord
                    // includes resting/basal metabolism, not just activity.
                    _caloriesBurned.value = estimateCaloriesBurned(stepsToday)

                    generateCoachTip()
                    return@launch
                }
            } catch (_: Exception) {}

            try {
                if (pedometerService.isAvailable(app)) {
                    pedometerService.start(app)
                    val pedSteps = pedometerService.getSteps()
                    _steps.value = pedSteps
                    stepsRepository.saveSteps(pedSteps, today)
                    pedometerService.stop()
                    _caloriesBurned.value = estimateCaloriesBurned(pedSteps)
                    generateCoachTip()
                    return@launch
                }
            } catch (_: Exception) {}

            val savedSteps = stepsRepository.getSteps(today)
            _steps.value = savedSteps
            _caloriesBurned.value = estimateCaloriesBurned(savedSteps)
            generateCoachTip()
        }
    }

    private fun generateCoachTip() {
        val cal = _caloriesEaten.value
        val calGoal = _calorieGoal.value
        val p = _protein.value
        val c = _carbs.value
        val f = _fat.value
        val s = _steps.value
        val sGoal = _stepGoal.value
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val name = _nickname.value.ifBlank { "friend" }

        val timeOfDay =
                when {
                    hour < 12 -> "morning"
                    hour < 17 -> "afternoon"
                    else -> "evening"
                }

        val prompt = buildString {
            append("You are a kind, supportive fitness coach and teammate. ")
            append("The user's name is $name. It is currently $timeOfDay. ")
            append("Here is their data for today:\n")
            append("- Calories eaten: $cal of $calGoal cal goal\n")
            append("- Protein: ${p.toInt()}g, Carbs: ${c.toInt()}g, Fat: ${f.toInt()}g\n")
            append("- Steps: $s of $sGoal step goal\n\n")

            if (cal == 0 && p == 0f && c == 0f && f == 0f) {
                append("They have NOT logged any food yet today. ")
            }
            if (s == 0) {
                append("They have NOT logged any steps yet today. ")
            }

            append("\nGive a short, encouraging coach tip (2-3 sentences max). ")
            append("Be specific to their actual numbers. ")
            append(
                    "If they haven't logged food or steps, gently suggest they consider adding them. "
            )
            append("If they're over their calorie goal, be supportive not judgmental. ")
            append(
                    "If macros are imbalanced (e.g. very high fat, very low protein), give a gentle suggestion. "
            )
            append(
                    "Sound like a friendly teammate, not a robot. Do NOT use emojis. Use only one space after periods. "
            )
            append(
                    "Bold the 1-3 most important short phrases (1-8 words each) by wrapping them in **double asterisks**."
            )
        }

        viewModelScope.launch {
            try {
                if (geminiNanoService.initIfNeeded()) {
                    val response = geminiNanoService.generateContent(prompt)
                    val cleaned =
                            response.trim()
                                    .removePrefix("\"")
                                    .removeSuffix("\"")
                                    .replace(Regex("""\*{3,}"""), "**")
                                    .replace(Regex(""" {2,}"""), " ")
                                    .trim()
                    if (cleaned.length >= 20) {
                        _coachTip.update { cleaned }
                        Log.d("FitTrack_Coach", "Gemini Nano tip: $cleaned")
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w("FitTrack_Coach", "Gemini Nano failed, using fallback: ${e.message}")
            }

            _coachTip.update { fallbackCoachTip(cal, calGoal, p, c, f, s, sGoal, hour, name) }
        }
    }

    private fun fallbackCoachTip(
            cal: Int,
            calGoal: Int,
            p: Float,
            c: Float,
            f: Float,
            s: Int,
            sGoal: Int,
            hour: Int,
            name: String
    ): String {
        val noFood = cal == 0 && p == 0f && c == 0f && f == 0f
        val noSteps = s == 0

        return when {
            noFood && noSteps -> {
                if (hour < 12)
                        "Good morning, $name! A fresh day ahead. Log your breakfast and get moving — you've got this!"
                else
                        "Hey $name, looks like today's a blank slate. No food or steps logged yet — consider adding them so I can help you stay on track!"
            }
            noFood ->
                    "You've got $s steps in already — nice work, $name! No food logged yet though. Consider adding your meals so we can track your nutrition together."
            noSteps ->
                    "Food's logged but no steps yet, $name. Even a short walk counts! Try to get moving when you can — your body will thank you."
            cal > calGoal * 1.15 -> {
                val over = cal - calGoal
                "You're about $over cal over your goal, $name. No stress — it happens! Maybe go lighter on your next meal or take a walk to balance things out."
            }
            cal > calGoal * 0.9 && cal <= calGoal ->
                    "Almost at your calorie goal, $name! You're right on track. Be mindful with snacks from here and you'll finish the day perfectly."
            hour >= 18 && cal < calGoal * 0.5 ->
                    "It's getting late and you're under half your calorie goal, $name. Make sure you're eating enough — your body needs fuel to recover!"
            s >= sGoal ->
                    "Step goal crushed! Amazing work, $name. Keep that energy going — consistency is what makes the difference."
            hour >= 15 && s < sGoal * 0.3 ->
                    "Afternoon check-in, $name: you're under 30% of your step goal. An evening walk could get you back on pace — you've still got time!"
            f > 0 && p > 0 && f * 9 > (p * 4 + c * 4 + f * 9) * 0.45 ->
                    "Your fat intake is high today relative to protein and carbs, $name. Try adding some lean protein or veggies to your next meal to balance things out."
            p > 0 && c > 0 && p * 4 < (p * 4 + c * 4 + f * 9) * 0.15 ->
                    "Protein's low today, $name. Consider adding something like chicken, eggs, or Greek yogurt — your muscles will appreciate it!"
            s.toFloat() / sGoal.coerceAtLeast(1) > 0.7f &&
                    cal.toFloat() / calGoal.coerceAtLeast(1) < 0.8f ->
                    "Great step count so far, $name! Your calories are well managed too. You're having an awesome day — keep it up!"
            else ->
                    "Looking good, $name! You're making progress on your goals today. Keep logging and moving — every bit counts."
        }
    }

    private fun estimateCaloriesBurned(steps: Int): Int {
        val weightLbs = goalsRepository.getWeightLbs()
        val calPerStep = 0.04 * (weightLbs / 150.0)
        return (steps * calPerStep).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        pedometerService.stop()
    }
}
