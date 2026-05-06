package com.hopehealth.app.ui.steps

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hopehealth.app.data.FoodRepository
import com.hopehealth.app.data.GoalsRepository
import com.hopehealth.app.data.StepsRepository
import com.hopehealth.app.services.HealthConnectService
import com.hopehealth.app.services.PedometerService
import com.hopehealth.app.util.todayKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StepsViewModel @Inject constructor(
    application: Application,
    private val stepsRepository: StepsRepository,
    private val goalsRepository: GoalsRepository,
    private val foodRepository: FoodRepository,
) : AndroidViewModel(application) {

    val healthConnectService: HealthConnectService = HealthConnectService()
    private val pedometerService = PedometerService()

    private val _steps                         = MutableStateFlow(0)
    private val _stepGoal                      = MutableStateFlow(10000)
    private val _stepSource                    = MutableStateFlow("manual")
    private val _stepsHistory                  = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    private val _caloriesHistory               = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    private val _caloriesConsumed              = MutableStateFlow(0)
    private val _caloriesBurned                = MutableStateFlow(0)
    private val _showAddDialog                 = MutableStateFlow(false)
    private val _showGoalDialog                = MutableStateFlow(false)
    private val _addStepsText                  = MutableStateFlow("")
    private val _addMode                       = MutableStateFlow("add")
    private val _needsHealthConnectPermissions = MutableStateFlow(false)
    private val _goalText                      = MutableStateFlow("")

    val steps:                         StateFlow<Int>                  = _steps.asStateFlow()
    val stepGoal:                      StateFlow<Int>                  = _stepGoal.asStateFlow()
    val stepSource:                    StateFlow<String>               = _stepSource.asStateFlow()
    val stepsHistory:                  StateFlow<List<Pair<String, Int>>> = _stepsHistory.asStateFlow()
    val caloriesHistory:               StateFlow<List<Pair<String, Int>>> = _caloriesHistory.asStateFlow()
    val caloriesConsumed:              StateFlow<Int>                  = _caloriesConsumed.asStateFlow()
    val caloriesBurned:                StateFlow<Int>                  = _caloriesBurned.asStateFlow()
    val showAddDialog:                 StateFlow<Boolean>              = _showAddDialog.asStateFlow()
    val showGoalDialog:                StateFlow<Boolean>              = _showGoalDialog.asStateFlow()
    val addStepsText:                  StateFlow<String>               = _addStepsText.asStateFlow()
    val addMode:                       StateFlow<String>               = _addMode.asStateFlow()
    val needsHealthConnectPermissions: StateFlow<Boolean>              = _needsHealthConnectPermissions.asStateFlow()
    val goalText:                      StateFlow<String>               = _goalText.asStateFlow()

    /** Approximate distance in km based on average stride length. */
    val distance: Float get() = _steps.value * 0.000762f

    /** Rough active-minutes estimate: ~100 steps/minute brisk walking. */
    val activeMinutes: Int get() = _steps.value / 100

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                loadData()
                delay(120_000)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val today = todayKey()

                when {
                    tryLoadFromHealthConnect(app, today) -> Unit
                    tryLoadFromPedometer(app, today)     -> Unit
                    else                                 -> loadFromDatabase(today)
                }

                _stepGoal.value         = goalsRepository.getStepGoal()
                _caloriesConsumed.value = foodRepository.getTotalCaloriesToday()
                _caloriesHistory.value  = foodRepository.getCaloriesHistory(7)
            } catch (e: Exception) {
                Log.w("StepsViewModel", "loadData failed", e)
                _stepSource.value = "manual"
            }
        }
    }

    private suspend fun tryLoadFromHealthConnect(app: Application, today: String): Boolean {
        return try {
            if (!healthConnectService.isAvailable(app)) {
                _needsHealthConnectPermissions.value = false
                return false
            }
            if (!healthConnectService.initialize(app)) {
                _needsHealthConnectPermissions.value = true
                return false
            }

            val stepsToday = healthConnectService.readStepsToday()
            val history    = healthConnectService.readStepsHistory(7)

            _steps.value                         = stepsToday
            _stepsHistory.value                  = history
            _caloriesBurned.value                = estimateCaloriesBurned(stepsToday)
            _stepSource.value                    = "health_connect"
            _needsHealthConnectPermissions.value = false

            history.forEach { (date, count) -> stepsRepository.saveSteps(count, date) }
            true
        } catch (e: Exception) {
            Log.w("StepsViewModel", "HealthConnect unavailable", e)
            _needsHealthConnectPermissions.value = false
            false
        }
    }

    private suspend fun tryLoadFromPedometer(app: Application, today: String): Boolean {
        return try {
            if (!pedometerService.isAvailable(app)) return false

            pedometerService.onStepUpdate = { count ->
                _steps.value = count
                viewModelScope.launch { stepsRepository.saveSteps(count, today) }
            }
            pedometerService.start(app)

            val count = pedometerService.getSteps()
            _steps.value = count
            stepsRepository.saveSteps(count, today)

            val history = stepsRepository.getStepsHistory(7)
            _stepsHistory.value   = if (history.isNotEmpty()) listOf(today to count) + history.drop(1) else listOf(today to count)
            _caloriesBurned.value = estimateCaloriesBurned(count)
            _stepSource.value     = "pedometer"
            true
        } catch (e: Exception) {
            Log.w("StepsViewModel", "Pedometer unavailable", e)
            false
        }
    }

    private suspend fun loadFromDatabase(today: String) {
        pedometerService.stop()
        val count = stepsRepository.getSteps(today)
        _steps.value          = count
        _stepsHistory.value   = stepsRepository.getStepsHistory(7)
        _caloriesBurned.value = estimateCaloriesBurned(count)
        _stepSource.value     = "manual"
    }

    fun addSteps() {
        val value = _addStepsText.value.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return
        val today = todayKey()

        viewModelScope.launch {
            val newTotal = if (_addMode.value == "add") _steps.value + value else value
            _steps.value = newTotal
            stepsRepository.saveSteps(newTotal, today)

            if (_stepSource.value == "pedometer") {
                pedometerService.stop()
                _stepSource.value = "manual"
            }

            _addStepsText.value  = ""
            _showAddDialog.value = false
            _caloriesBurned.value = estimateCaloriesBurned(newTotal)

            // Sync history list without a full reload
            val history = _stepsHistory.value.toMutableList()
            if (history.isNotEmpty() && history[0].first == today) {
                history[0] = today to newTotal
                _stepsHistory.value = history
            }
        }
    }

    fun setStepGoal(goal: Int) {
        if (goal <= 0) return
        viewModelScope.launch {
            goalsRepository.setStepGoal(goal)
            _stepGoal.value     = goal
            _goalText.value     = ""
            _showGoalDialog.value = false
        }
    }

    /** Estimates calories burned using MET-based approximation for the stored body weight. */
    private suspend fun estimateCaloriesBurned(steps: Int): Int {
        val weightLbs = goalsRepository.getWeightLbs().toDouble()
        return (steps * 0.04 * (weightLbs / 150.0)).toInt()
    }

    fun onHealthConnectResult(granted: Set<String>) {
        if (granted.isNotEmpty()) loadData()
    }

    fun showAddDialog() {
        _addStepsText.value  = ""
        _addMode.value       = "add"
        _showAddDialog.value = true
    }

    fun dismissAddDialog() {
        _addStepsText.value  = ""
        _showAddDialog.value = false
    }

    fun updateAddStepsText(text: String) { _addStepsText.value = text }
    fun setAddMode(mode: String)         { _addMode.value = mode }

    fun showGoalDialog() {
        _goalText.value      = _stepGoal.value.toString()
        _showGoalDialog.value = true
    }

    fun dismissGoalDialog() {
        _goalText.value      = ""
        _showGoalDialog.value = false
    }

    fun updateGoalText(text: String) { _goalText.value = text }

    fun saveGoalFromDialog() {
        val goal = _goalText.value.trim().toIntOrNull() ?: return
        if (goal > 0) setStepGoal(goal)
    }

    override fun onCleared() {
        super.onCleared()
        pedometerService.stop()
    }
}
