package com.fittrack.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.StepsRepository
import com.fittrack.app.util.todayKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)
    private val foodRepository = FoodRepository(application)
    private val stepsRepository = StepsRepository(application)

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

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _calorieGoal.value = goalsRepository.getCalorieGoal()
            _stepGoal.value = goalsRepository.getStepGoal()
            _caloriesEaten.value = foodRepository.getTotalCaloriesToday()
            val (proteinVal, carbsVal, fatVal) = foodRepository.getTodayMacros()
            _protein.value = proteinVal
            _carbs.value = carbsVal
            _fat.value = fatVal
            _steps.value = stepsRepository.getSteps(todayKey())
        }
    }

}
