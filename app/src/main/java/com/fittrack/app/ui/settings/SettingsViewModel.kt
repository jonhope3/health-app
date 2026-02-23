package com.fittrack.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.ModelDownloadManager
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.HealthConnectService
import com.fittrack.app.services.MediaPipeLLMService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class DbEntry(val table: String, val key: String, val value: String)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val goalsRepository = GoalsRepository(application)
    private val healthConnectService = HealthConnectService()

    private val modelDownloadManager = ModelDownloadManager(application)
    private val mediaPipeLLMService =
            MediaPipeLLMService(application, modelDownloadManager.getModelFile().absolutePath)
    private val geminiNanoService = GeminiNanoService(application)
    private val json = Json { ignoreUnknownKeys = true }

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

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age.asStateFlow()

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    // Macro goals (grams)
    private val _proteinGoal = MutableStateFlow("")
    val proteinGoal: StateFlow<String> = _proteinGoal.asStateFlow()

    private val _carbsGoal = MutableStateFlow("")
    val carbsGoal: StateFlow<String> = _carbsGoal.asStateFlow()

    private val _fatGoal = MutableStateFlow("")
    val fatGoal: StateFlow<String> = _fatGoal.asStateFlow()

    private val _sugarGoal = MutableStateFlow("")
    val sugarGoal: StateFlow<String> = _sugarGoal.asStateFlow()

    private val _isGeneratingMacros = MutableStateFlow(false)
    val isGeneratingMacros: StateFlow<Boolean> = _isGeneratingMacros.asStateFlow()

    private val _macroGenerateError = MutableStateFlow<String?>(null)
    val macroGenerateError: StateFlow<String?> = _macroGenerateError.asStateFlow()

    private val _geminiReady = MutableStateFlow(false)
    val geminiReady: StateFlow<Boolean> = _geminiReady.asStateFlow()

    init {
        ensureDefaults()
        loadData()
        checkHealthConnect()
        viewModelScope.launch {
            val gemmaDownloaded = modelDownloadManager.isModelDownloaded()
            val nanoAvailable = geminiNanoService.isAvailable()
            _geminiReady.value = gemmaDownloaded || nanoAvailable

            if (!gemmaDownloaded) {
                modelDownloadManager.downloadModelIfNeeded()
                _geminiReady.value =
                        modelDownloadManager.isModelDownloaded() || geminiNanoService.isAvailable()
            }
        }
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
        _age.value = goalsRepository.getAge().toString()
        _nickname.value = goalsRepository.getNickname()
        _proteinGoal.value = goalsRepository.getProteinGoalG().toString()
        _carbsGoal.value = goalsRepository.getCarbsGoalG().toString()
        _fatGoal.value = goalsRepository.getFatGoalG().toString()
        _sugarGoal.value = goalsRepository.getSugarGoalG().toString()
    }

    fun setCalorieGoal(v: String) {
        _calorieGoal.value = v.filter { it.isDigit() }
    }
    fun setStepGoal(v: String) {
        _stepGoal.value = v.filter { it.isDigit() }
    }
    fun setWeightLbs(v: String) {
        _weightLbs.value = v.filter { it.isDigit() || it == '.' }
    }
    fun setHeightFt(v: String) {
        _heightFt.value = v.filter { it.isDigit() }
    }
    fun setHeightIn(v: String) {
        _heightIn.value = v.filter { it.isDigit() }
    }
    fun setAge(v: String) {
        _age.value = v.filter { it.isDigit() }
    }
    fun setNickname(v: String) {
        _nickname.value = v
    }
    fun setProteinGoal(v: String) {
        _proteinGoal.value = v.filter { it.isDigit() }
    }
    fun setCarbsGoal(v: String) {
        _carbsGoal.value = v.filter { it.isDigit() }
    }
    fun setFatGoal(v: String) {
        _fatGoal.value = v.filter { it.isDigit() }
    }
    fun setSugarGoal(v: String) {
        _sugarGoal.value = v.filter { it.isDigit() }
    }

    fun save() {
        _calorieGoal.value.toIntOrNull()?.let {
            if (it in 500..10000) goalsRepository.setCalorieGoal(it)
        }
        _stepGoal.value.toIntOrNull()?.let {
            if (it in 100..200000) goalsRepository.setStepGoal(it)
        }
        _weightLbs.value.toFloatOrNull()?.let {
            if (it in 50f..700f) goalsRepository.setWeightLbs(it)
        }
        val ft = _heightFt.value.toIntOrNull() ?: 0
        val inches = _heightIn.value.toIntOrNull() ?: 0
        val totalInches = ft * 12f + inches
        if (totalInches in 36f..96f) goalsRepository.setHeightIn(totalInches)
        _age.value.toIntOrNull()?.let { if (it in 1..120) goalsRepository.setAge(it) }
        val nick = _nickname.value.trim()
        if (nick.isNotEmpty()) goalsRepository.setNickname(nick)
    }

    fun saveMacroGoals() {
        _proteinGoal.value.toIntOrNull()?.let {
            if (it in 0..500) goalsRepository.setProteinGoalG(it)
        }
        _carbsGoal.value.toIntOrNull()?.let { if (it in 0..1000) goalsRepository.setCarbsGoalG(it) }
        _fatGoal.value.toIntOrNull()?.let { if (it in 0..500) goalsRepository.setFatGoalG(it) }
        _sugarGoal.value.toIntOrNull()?.let { if (it in 0..500) goalsRepository.setSugarGoalG(it) }
    }

    fun generateAiMacroGoals() {
        viewModelScope.launch {
            _isGeneratingMacros.value = true
            _macroGenerateError.value = null
            try {
                var response = ""
                if (mediaPipeLLMService.initialize()) {
                    val cals = _calorieGoal.value.toIntOrNull() ?: goalsRepository.getCalorieGoal()
                    val weightLbs =
                            _weightLbs.value.toFloatOrNull() ?: goalsRepository.getWeightLbs()
                    val heightFt = _heightFt.value.toIntOrNull() ?: 0
                    val heightIn = _heightIn.value.toIntOrNull() ?: 0
                    val totalIn = heightFt * 12 + heightIn
                    val age = _age.value.toIntOrNull() ?: goalsRepository.getAge()

                    val prompt = buildMacroPrompt(age, weightLbs, totalIn, cals)
                    response = mediaPipeLLMService.generateContent(prompt)
                }

                if (response.isBlank() && geminiNanoService.initIfNeeded()) {
                    val cals = _calorieGoal.value.toIntOrNull() ?: goalsRepository.getCalorieGoal()
                    val weightLbs =
                            _weightLbs.value.toFloatOrNull() ?: goalsRepository.getWeightLbs()
                    val heightFt = _heightFt.value.toIntOrNull() ?: 0
                    val heightIn = _heightIn.value.toIntOrNull() ?: 0
                    val totalIn = heightFt * 12 + heightIn
                    val age = _age.value.toIntOrNull() ?: goalsRepository.getAge()

                    val prompt = buildMacroPrompt(age, weightLbs, totalIn, cals)
                    response = geminiNanoService.generateContent(prompt)
                }

                if (response.isBlank()) {
                    _macroGenerateError.value = "AI Coach not available — set goals manually"
                    _isGeneratingMacros.value = false
                    return@launch
                }

                val jsonMatch = Regex("""\{[\s\S]*?\}""").find(response)
                val obj =
                        jsonMatch?.let {
                            try {
                                json.parseToJsonElement(it.value).jsonObject
                            } catch (_: Exception) {
                                null
                            }
                        }

                if (obj == null) {
                    _macroGenerateError.value = "Couldn't parse response — try again"
                    _isGeneratingMacros.value = false
                    return@launch
                }

                obj["protein"]?.jsonPrimitive?.content?.toIntOrNull()?.let {
                    _proteinGoal.value = it.toString()
                }
                obj["carbs"]?.jsonPrimitive?.content?.toIntOrNull()?.let {
                    _carbsGoal.value = it.toString()
                }
                obj["fat"]?.jsonPrimitive?.content?.toIntOrNull()?.let {
                    _fatGoal.value = it.toString()
                }
                obj["sugar"]?.jsonPrimitive?.content?.toIntOrNull()?.let {
                    _sugarGoal.value = it.toString()
                }

                // Auto-save immediately after generation
                saveMacroGoals()
            } catch (e: Exception) {
                _macroGenerateError.value = "Error: ${e.message}"
            }
            _isGeneratingMacros.value = false
        }
    }

    private fun buildMacroPrompt(age: Int, weightLbs: Float, totalIn: Int, cals: Int): String {
        return """You are a registered dietitian. Based on the following user profile, recommend daily macro intake goals.

User: age=$age years, weight=${weightLbs.toInt()} lbs, height=${totalIn}in, daily calorie goal=$cals kcal.

Return ONLY a JSON object with these fields (all integers, in grams):
- protein: recommended daily protein in grams
- carbs: recommended daily carbs in grams  
- fat: recommended daily fat in grams
- sugar: recommended daily added sugar limit in grams

Base on standard nutrition guidelines (e.g. Dietary Guidelines for Americans, ISSN protein recommendations).
Example: {"protein":120,"carbs":225,"fat":65,"sugar":50}

Return ONLY the JSON object, no other text."""
    }

    private fun fmtNum(v: Float): String {
        return if (v == v.toLong().toFloat()) v.toLong().toString() else "%.1f".format(v)
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
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { mediaPipeLLMService.tryClosing() }
    }
}
