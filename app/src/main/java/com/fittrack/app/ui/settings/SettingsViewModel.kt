package com.fittrack.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.fittrack.app.data.GoalsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)

    private val _calorieGoal = MutableStateFlow("")
    val calorieGoal: StateFlow<String> = _calorieGoal.asStateFlow()

    private val _stepGoal = MutableStateFlow("")
    val stepGoal: StateFlow<String> = _stepGoal.asStateFlow()

    private val _weightLbs = MutableStateFlow("")
    val weightLbs: StateFlow<String> = _weightLbs.asStateFlow()

    private val _heightFt = MutableStateFlow("")
    val heightFt: StateFlow<String> = _heightFt.asStateFlow()

    private val _heightIn = MutableStateFlow("")
    val heightIn: StateFlow<String> = _heightIn.asStateFlow()

    init { loadData() }

    fun loadData() {
        _calorieGoal.value = goalsRepository.getCalorieGoal().toString()
        _stepGoal.value = goalsRepository.getStepGoal().toString()
        _weightLbs.value = goalsRepository.getWeightLbs().let { fmtNum(it) }
        val totalInches = goalsRepository.getHeightIn()
        _heightFt.value = (totalInches / 12).toInt().toString()
        _heightIn.value = (totalInches % 12).toInt().toString()
    }

    fun setCalorieGoal(v: String) { _calorieGoal.value = v.filter { it.isDigit() } }
    fun setStepGoal(v: String) { _stepGoal.value = v.filter { it.isDigit() } }
    fun setWeightLbs(v: String) { _weightLbs.value = v.filter { it.isDigit() || it == '.' } }
    fun setHeightFt(v: String) { _heightFt.value = v.filter { it.isDigit() } }
    fun setHeightIn(v: String) { _heightIn.value = v.filter { it.isDigit() } }

    fun save() {
        _calorieGoal.value.toIntOrNull()?.let { if (it in 500..10000) goalsRepository.setCalorieGoal(it) }
        _stepGoal.value.toIntOrNull()?.let { if (it in 100..200000) goalsRepository.setStepGoal(it) }
        _weightLbs.value.toFloatOrNull()?.let { if (it in 50f..700f) goalsRepository.setWeightLbs(it) }
        val ft = _heightFt.value.toIntOrNull() ?: 0
        val inches = _heightIn.value.toIntOrNull() ?: 0
        val totalInches = ft * 12f + inches
        if (totalInches in 36f..96f) goalsRepository.setHeightIn(totalInches)
    }

    private fun fmtNum(v: Float): String {
        return if (v == v.toLong().toFloat()) v.toLong().toString()
        else "%.1f".format(v)
    }
}
