package com.fittrack.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.MealType
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import com.fittrack.app.services.PedometerService
import com.fittrack.app.util.todayKey
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val goalsRepository: GoalsRepository,
    private val foodRepository: FoodRepository,
    private val stepsRepository: StepsRepository,
    private val geminiNanoService: GeminiNanoService,
) : AndroidViewModel(application) {

    private val healthConnectService = HealthConnectService()
    private val pedometerService = PedometerService()

    private val coachTipUseCase = CoachTipUseCase(
        foodRepository    = foodRepository,
        stepsRepository   = stepsRepository,
        geminiNanoService = geminiNanoService,
    )

    private var coachTipJob: Job? = null

    private val _calorieGoal    = MutableStateFlow(2000)
    private val _stepGoal       = MutableStateFlow(10000)
    private val _caloriesEaten  = MutableStateFlow(0)
    private val _protein        = MutableStateFlow(0f)
    private val _carbs          = MutableStateFlow(0f)
    private val _fat            = MutableStateFlow(0f)
    private val _sugar          = MutableStateFlow(0f)
    private val _caloriesBurned = MutableStateFlow(0)
    private val _proteinGoalG   = MutableStateFlow(100)
    private val _carbsGoalG     = MutableStateFlow(250)
    private val _fatGoalG       = MutableStateFlow(65)
    private val _sugarGoalG     = MutableStateFlow(50)
    private val _steps          = MutableStateFlow(0)
    private val _nickname       = MutableStateFlow("")
    private val _coachTip       = MutableStateFlow("")
    private val _mealBreakdown  = MutableStateFlow<Map<MealType, Int>>(emptyMap())
    private val _selectedMealType = MutableStateFlow(defaultMealTypeForTime())

    val calorieGoal:    StateFlow<Int>    = _calorieGoal.asStateFlow()
    val stepGoal:       StateFlow<Int>    = _stepGoal.asStateFlow()
    val caloriesEaten:  StateFlow<Int>    = _caloriesEaten.asStateFlow()
    val protein:        StateFlow<Float>  = _protein.asStateFlow()
    val carbs:          StateFlow<Float>  = _carbs.asStateFlow()
    val fat:            StateFlow<Float>  = _fat.asStateFlow()
    val sugar:          StateFlow<Float>  = _sugar.asStateFlow()
    val caloriesBurned: StateFlow<Int>    = _caloriesBurned.asStateFlow()
    val proteinGoalG:   StateFlow<Int>    = _proteinGoalG.asStateFlow()
    val carbsGoalG:     StateFlow<Int>    = _carbsGoalG.asStateFlow()
    val fatGoalG:       StateFlow<Int>    = _fatGoalG.asStateFlow()
    val sugarGoalG:     StateFlow<Int>    = _sugarGoalG.asStateFlow()
    val steps:          StateFlow<Int>    = _steps.asStateFlow()
    val nickname:       StateFlow<String> = _nickname.asStateFlow()
    val coachTip:       StateFlow<String> = _coachTip.asStateFlow()
    val mealBreakdown:  StateFlow<Map<MealType, Int>> = _mealBreakdown.asStateFlow()
    val selectedMealType: StateFlow<MealType> = _selectedMealType.asStateFlow()

    /** True once the user has dismissed the onboarding flow. */
    val onboardingDone: StateFlow<Boolean> = goalsRepository.onboardingDoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _calorieGoal.value   = goalsRepository.getCalorieGoal()
            _stepGoal.value      = goalsRepository.getStepGoal()
            _caloriesEaten.value = foodRepository.getTotalCaloriesToday()
            _nickname.value      = goalsRepository.getNickname()

            val macros = foodRepository.getTodayMacros()
            _protein.value = macros.protein
            _carbs.value   = macros.carbs
            _fat.value     = macros.fat
            _sugar.value   = macros.sugar

            _proteinGoalG.value = goalsRepository.getProteinGoalG()
            _carbsGoalG.value   = goalsRepository.getCarbsGoalG()
            _fatGoalG.value     = goalsRepository.getFatGoalG()
            _sugarGoalG.value   = goalsRepository.getSugarGoalG()

            // Load per-meal calorie breakdown
            _mealBreakdown.value = foodRepository.getMealTypeCalories()

            val weightLbs = goalsRepository.getWeightLbs().toDouble()
            val app = getApplication<Application>()
            val today = todayKey()

            try {
                if (healthConnectService.isAvailable(app) && healthConnectService.initialize(app)) {
                    val stepsToday = healthConnectService.readStepsToday()
                    _steps.value = stepsToday
                    stepsRepository.saveSteps(stepsToday, today)

                    healthConnectService.readStepsHistory(7).forEach { (date, count) ->
                        stepsRepository.saveSteps(count, date)
                    }

                    _caloriesBurned.value = estimateCaloriesBurned(stepsToday, weightLbs)
                    generateCoachTip()
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "HealthConnect unavailable", e)
            }

            try {
                if (pedometerService.isAvailable(app)) {
                    pedometerService.start(app)
                    val pedSteps = pedometerService.getSteps()
                    _steps.value = pedSteps
                    stepsRepository.saveSteps(pedSteps, today)
                    pedometerService.stop()
                    _caloriesBurned.value = estimateCaloriesBurned(pedSteps, weightLbs)
                    generateCoachTip()
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Pedometer unavailable", e)
            }

            // Fallback: use last saved value from the database
            val savedSteps = stepsRepository.getSteps(today)
            _steps.value = savedSteps
            _caloriesBurned.value = estimateCaloriesBurned(savedSteps, weightLbs)
            generateCoachTip()
        }
    }

    private fun generateCoachTip() {
        coachTipJob?.cancel()
        coachTipJob = viewModelScope.launch {
            val tip = coachTipUseCase.getCoachTip(
                CoachPromptData(
                    caloriesEaten = _caloriesEaten.value,
                    calorieGoal   = _calorieGoal.value,
                    protein       = _protein.value,
                    proteinGoal   = _proteinGoalG.value,
                    carbs         = _carbs.value,
                    carbsGoal     = _carbsGoalG.value,
                    fat           = _fat.value,
                    fatGoal       = _fatGoalG.value,
                    sugar         = _sugar.value,
                    sugarGoal     = _sugarGoalG.value,
                    steps         = _steps.value,
                    stepGoal      = _stepGoal.value,
                    nickname      = _nickname.value,
                )
            )
            _coachTip.value = tip
        }
    }

    private fun estimateCaloriesBurned(steps: Int, weightLbs: Double): Int =
        (steps * 0.04 * (weightLbs / 150.0)).toInt()

    fun onboardingSave(name: String, calorieGoal: Int, stepGoal: Int) {
        viewModelScope.launch {
            if (name.isNotBlank()) goalsRepository.setNickname(name)
            goalsRepository.setCalorieGoal(calorieGoal)
            goalsRepository.setStepGoal(stepGoal)
            goalsRepository.setOnboardingCompleted()
        }
    }

    /** User manually picks a meal type tab — overrides the time-based default. */
    fun selectMealType(meal: MealType) {
        _selectedMealType.value = meal
    }

    fun markOnboardingComplete() {
        viewModelScope.launch { goalsRepository.setOnboardingCompleted() }
    }

    override fun onCleared() {
        super.onCleared()
        pedometerService.stop()
    }

    companion object {
        /** Maps the current hour to the most contextually relevant meal type. */
        fun defaultMealTypeForTime(): MealType = when (LocalTime.now().hour) {
            in 5..10  -> MealType.BREAKFAST
            in 11..14 -> MealType.LUNCH
            in 15..16 -> MealType.SNACK
            in 17..21 -> MealType.DINNER
            else      -> MealType.SNACK
        }
    }
}
