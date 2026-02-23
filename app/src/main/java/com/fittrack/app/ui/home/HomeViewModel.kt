package com.fittrack.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.DownloadState
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.ModelDownloadManager
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import com.fittrack.app.services.MediaPipeLLMService
import com.fittrack.app.services.PedometerService
import com.fittrack.app.util.todayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel
@JvmOverloads
constructor(
        application: Application,
        private val random: kotlin.random.Random = kotlin.random.Random.Default
) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)
    private val foodRepository = FoodRepository(application)
    private val stepsRepository = StepsRepository(application)
    private val healthConnectService = HealthConnectService()
    private val pedometerService = PedometerService()

    private val modelDownloadManager = ModelDownloadManager(application)
    private val mediaPipeLLMService =
            MediaPipeLLMService(application, modelDownloadManager.getModelFile().absolutePath)
    private val geminiNanoService = GeminiNanoService(application)

    private val coachTipUseCase =
            CoachTipUseCase(
                    foodRepository = foodRepository,
                    stepsRepository = stepsRepository,
                    mediaPipeLLMService = mediaPipeLLMService,
                    geminiNanoService = geminiNanoService,
                    random = random
            )

    private var coachTipJob: kotlinx.coroutines.Job? = null

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

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    init {
        viewModelScope.launch {
            modelDownloadManager.downloadState.collect { _downloadState.value = it }
        }
        viewModelScope.launch {
            modelDownloadManager.downloadProgress.collect { _downloadProgress.value = it }
        }
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

            val weightLbs = goalsRepository.getWeightLbs().toDouble()

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
                    _caloriesBurned.value = estimateCaloriesBurned(stepsToday, weightLbs)

                    generateCoachTip()
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "HealthConnect failed", e)
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
                Log.w("HomeViewModel", "Pedometer failed", e)
            }

            val savedSteps = stepsRepository.getSteps(today)
            _steps.value = savedSteps
            _caloriesBurned.value = estimateCaloriesBurned(savedSteps, weightLbs)

            // Start model download if needed
            viewModelScope.launch { modelDownloadManager.downloadModelIfNeeded() }

            generateCoachTip()
        }
    }

    private fun generateCoachTip() {
        coachTipJob?.cancel()

        val data =
                CoachPromptData(
                        caloriesEaten = _caloriesEaten.value,
                        calorieGoal = _calorieGoal.value,
                        protein = _protein.value,
                        carbs = _carbs.value,
                        fat = _fat.value,
                        sugar = _sugar.value,
                        sugarGoal = _sugarGoalG.value,
                        steps = _steps.value,
                        stepGoal = _stepGoal.value,
                        nickname = _nickname.value
                )

        coachTipJob =
                viewModelScope.launch {
                    val tip = coachTipUseCase.getCoachTip(data)
                    _coachTip.update { tip }
                }
    }

    private fun estimateCaloriesBurned(steps: Int, weightLbs: Double): Int {
        val calPerStep = 0.04 * (weightLbs / 150.0)
        return (steps * calPerStep).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        pedometerService.stop()
        viewModelScope.launch { mediaPipeLLMService.tryClosing() }
    }
}
