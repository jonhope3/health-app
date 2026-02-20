package com.fittrack.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.services.HealthConnectService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

data class DbEntry(
    val table: String,
    val key: String,
    val value: String
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)
    private val healthConnectService = HealthConnectService()

    private val _healthConnectGranted = MutableStateFlow(false)
    val healthConnectGranted: StateFlow<Boolean> = _healthConnectGranted.asStateFlow()

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

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    init {
        ensureDefaults()
        loadData()
        checkHealthConnect()
    }

    private fun ensureDefaults() {
        val app = getApplication<Application>()
        val p = app.getSharedPreferences("fittrack_goals", Context.MODE_PRIVATE)
        if (!p.contains("calorie_goal")) {
            p.edit()
                .putInt("calorie_goal", 2000)
                .putInt("step_goal", 10000)
                .putFloat("weight_lbs", 160f)
                .putFloat("height_in", 68f)
                .apply()
        }

        val f = app.getSharedPreferences("fittrack_food", Context.MODE_PRIVATE)
        if (f.getString("custom_foods", null) == null) {
            f.edit().putString("custom_foods", "[]").apply()
        }
    }

    private fun checkHealthConnect() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (healthConnectService.isAvailable(app)) {
                _healthConnectGranted.value = healthConnectService.initialize(app)
            }
        }
    }

    fun onHealthConnectResult(granted: Set<String>) {
        _healthConnectGranted.value = granted.isNotEmpty()
    }

    fun loadData() {
        _calorieGoal.value = goalsRepository.getCalorieGoal().toString()
        _stepGoal.value = goalsRepository.getStepGoal().toString()
        _weightLbs.value = goalsRepository.getWeightLbs().let { fmtNum(it) }
        val totalInches = goalsRepository.getHeightIn()
        _heightFt.value = (totalInches / 12).toInt().toString()
        _heightIn.value = (totalInches % 12).toInt().toString()
        _nickname.value = goalsRepository.getNickname()
    }

    fun setCalorieGoal(v: String) { _calorieGoal.value = v.filter { it.isDigit() } }
    fun setStepGoal(v: String) { _stepGoal.value = v.filter { it.isDigit() } }
    fun setWeightLbs(v: String) { _weightLbs.value = v.filter { it.isDigit() || it == '.' } }
    fun setHeightFt(v: String) { _heightFt.value = v.filter { it.isDigit() } }
    fun setHeightIn(v: String) { _heightIn.value = v.filter { it.isDigit() } }
    fun setNickname(v: String) { _nickname.value = v }

    fun save() {
        _calorieGoal.value.toIntOrNull()?.let { if (it in 500..10000) goalsRepository.setCalorieGoal(it) }
        _stepGoal.value.toIntOrNull()?.let { if (it in 100..200000) goalsRepository.setStepGoal(it) }
        _weightLbs.value.toFloatOrNull()?.let { if (it in 50f..700f) goalsRepository.setWeightLbs(it) }
        val ft = _heightFt.value.toIntOrNull() ?: 0
        val inches = _heightIn.value.toIntOrNull() ?: 0
        val totalInches = ft * 12f + inches
        if (totalInches in 36f..96f) goalsRepository.setHeightIn(totalInches)
        val nick = _nickname.value.trim()
        if (nick.isNotEmpty()) goalsRepository.setNickname(nick)
    }

    private fun fmtNum(v: Float): String {
        return if (v == v.toLong().toFloat()) v.toLong().toString()
        else "%.1f".format(v)
    }

    fun nukeDb() {
        val app = getApplication<Application>()
        app.getSharedPreferences("fittrack_food", Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("fittrack_goals", Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("fittrack_steps", Context.MODE_PRIVATE).edit().clear().apply()
        ensureDefaults()
        loadData()
    }

    fun getDbOverview(): List<DbEntry> {
        val app = getApplication<Application>()
        val list = mutableListOf<DbEntry>()
        listOf("fittrack_food", "fittrack_goals", "fittrack_steps").forEach { prefName ->
            val p = app.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val allEntries = p.all
            if (allEntries.isEmpty()) {
                list.add(DbEntry(prefName, "(empty)", "No data records found in this table"))
            } else {
                allEntries.forEach { (key, value) ->
                    list.add(DbEntry(prefName, key, value.toString()))
                }
            }
        }
        return list.sortedBy { it.table }
    }
}
