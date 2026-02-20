package com.fittrack.app.ui.steps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.services.HealthConnectService
import com.fittrack.app.services.PedometerService
import com.fittrack.app.util.todayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StepsViewModel(application: Application) : AndroidViewModel(application) {

    private val stepsRepository = StepsRepository(application)
    private val goalsRepository = GoalsRepository(application)
    private val foodRepository = FoodRepository(application)
    val healthConnectService = HealthConnectService()
    private val pedometerService = PedometerService()

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    private val _stepGoal = MutableStateFlow(10000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _stepSource = MutableStateFlow("manual")
    val stepSource: StateFlow<String> = _stepSource.asStateFlow()

    private val _stepsHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val stepsHistory: StateFlow<List<Pair<String, Int>>> = _stepsHistory.asStateFlow()

    private val _caloriesHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val caloriesHistory: StateFlow<List<Pair<String, Int>>> = _caloriesHistory.asStateFlow()

    private val _caloriesConsumed = MutableStateFlow(0)
    val caloriesConsumed: StateFlow<Int> = _caloriesConsumed.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _showGoalDialog = MutableStateFlow(false)
    val showGoalDialog: StateFlow<Boolean> = _showGoalDialog.asStateFlow()

    private val _addStepsText = MutableStateFlow("")
    val addStepsText: StateFlow<String> = _addStepsText.asStateFlow()

    private val _addMode = MutableStateFlow("add")
    val addMode: StateFlow<String> = _addMode.asStateFlow()

    private val _needsHealthConnectPermissions = MutableStateFlow(false)
    val needsHealthConnectPermissions: StateFlow<Boolean> = _needsHealthConnectPermissions.asStateFlow()

    private val _goalText = MutableStateFlow("")
    val goalText: StateFlow<String> = _goalText.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                loadData()
                kotlinx.coroutines.delay(30000) // Poll every 30 seconds
            }
        }
    }

    val distance: Float
        get() = _steps.value * 0.000762f

    val activeMinutes: Int
        get() = _steps.value / 100

    fun loadData() {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val today = todayKey()

                var loaded = false

                // 1. Try Health Connect
                try {
                    if (healthConnectService.isAvailable(app)) {
                        val initialized = healthConnectService.initialize(app)
                        if (initialized) {
                            val stepsToday = healthConnectService.readStepsToday()
                            val history = healthConnectService.readStepsHistory(7)
                            val burned = healthConnectService.readCaloriesBurnedToday()

                            _steps.update { stepsToday }
                            _stepsHistory.update { history }
                            _caloriesBurned.update { burned }
                            _stepSource.update { "health_connect" }
                            _needsHealthConnectPermissions.value = false

                            history.forEach { (date, count) ->
                                stepsRepository.saveSteps(count, date)
                            }

                            loaded = true
                        } else {
                            _needsHealthConnectPermissions.value = true
                        }
                    } else {
                        _needsHealthConnectPermissions.value = false
                    }
                } catch (_: Exception) {
                    _needsHealthConnectPermissions.value = false
                }

                // 2. Try Pedometer
                if (!loaded) {
                    try {
                        if (pedometerService.isAvailable(app)) {
                            pedometerService.onStepUpdate = { stepCount ->
                                _steps.update { stepCount }
                                stepsRepository.saveSteps(stepCount, today)
                            }
                            pedometerService.start(app)
                            val pedometerSteps = pedometerService.getSteps()
                            _steps.update { pedometerSteps }
                            stepsRepository.saveSteps(pedometerSteps, today)
                            val repoHistory = stepsRepository.getStepsHistory(7)
                            val mergedHistory = if (repoHistory.isNotEmpty()) {
                                listOf(today to pedometerSteps) + repoHistory.drop(1)
                            } else {
                                listOf(today to pedometerSteps)
                            }
                            _stepsHistory.update { mergedHistory }
                            _caloriesBurned.update { caloriesBurnedEstimate() }
                            _stepSource.update { "pedometer" }
                            loaded = true
                        }
                    } catch (_: Exception) { }
                }

                // 3. Fallback to manual
                if (!loaded) {
                    pedometerService.stop()
                    val manualSteps = stepsRepository.getSteps(today)
                    _steps.update { manualSteps }
                    _stepsHistory.update { stepsRepository.getStepsHistory(7) }
                    _caloriesBurned.update { caloriesBurnedEstimate() }
                    _stepSource.update { "manual" }
                }

                _stepGoal.update { goalsRepository.getStepGoal() }
                _caloriesConsumed.update { foodRepository.getTotalCaloriesToday() }
                _caloriesHistory.update { foodRepository.getCaloriesHistory(7) }
            } catch (_: Exception) {
                _stepSource.update { "manual" }
            }
        }
    }

    fun addSteps() {
        val text = _addStepsText.value.trim()
        val value = text.toIntOrNull() ?: return
        if (value < 0) return

        val today = todayKey()
        val current = _steps.value

        if (_addMode.value == "add") {
            val newTotal = current + value
            _steps.update { newTotal }
            stepsRepository.saveSteps(newTotal, today)
        } else {
            _steps.update { value }
            stepsRepository.saveSteps(value, today)
        }

        // When manually adding, switch to manual to avoid sensor overwriting local manual
        if (_stepSource.value == "pedometer") {
            pedometerService.stop()
            _stepSource.update { "manual" }
        }

        _addStepsText.update { "" }
        _showAddDialog.update { false }
        _caloriesBurned.update { caloriesBurnedEstimate() }

        // Refresh history for today
        val history = _stepsHistory.value.toMutableList()
        if (history.isNotEmpty() && history[0].first == today) {
            history[0] = today to _steps.value
            _stepsHistory.update { history }
        }
    }

    fun setStepGoal(goal: Int) {
        if (goal < 0) return
        goalsRepository.setStepGoal(goal)
        _stepGoal.update { goal }
        _goalText.update { "" }
        _showGoalDialog.update { false }
    }

    fun caloriesBurnedEstimate(): Int {
        return if (_stepSource.value == "health_connect") {
            _caloriesBurned.value
        } else {
            val weightLbs = goalsRepository.getWeightLbs()
            val calPerStep = 0.04 * (weightLbs / 150.0)
            (_steps.value * calPerStep).toInt()
        }
    }

    fun showAddDialog() {
        _showAddDialog.update { true }
        _addStepsText.update { "" }
        _addMode.update { "add" }
    }

    fun dismissAddDialog() {
        _showAddDialog.update { false }
        _addStepsText.update { "" }
    }

    fun updateAddStepsText(text: String) {
        _addStepsText.update { text }
    }

    fun setAddMode(mode: String) {
        _addMode.update { mode }
    }

    fun showGoalDialog() {
        _showGoalDialog.update { true }
        _goalText.update { _stepGoal.value.toString() }
    }

    fun dismissGoalDialog() {
        _showGoalDialog.update { false }
        _goalText.update { "" }
    }

    fun updateGoalText(text: String) {
        _goalText.update { text }
    }

    fun saveGoalFromDialog() {
        val goal = _goalText.value.trim().toIntOrNull() ?: return
        if (goal > 0) setStepGoal(goal)
    }

    override fun onCleared() {
        super.onCleared()
        pedometerService.stop()
    }
}
