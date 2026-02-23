package com.fittrack.app.ui.log

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.DiaryItem
import com.fittrack.app.data.FoodItem
import com.fittrack.app.data.FoodRepository
import com.fittrack.app.data.GoalsRepository
import com.fittrack.app.data.MealType
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.data.ParsedFoodItem
import com.fittrack.app.services.AiFoodParserService
import com.fittrack.app.services.GeminiNanoService
import com.fittrack.app.services.NutritionSearchService
import com.fittrack.app.util.todayKey
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val foodRepository = FoodRepository(application)
    private val goalsRepository = GoalsRepository(application)
    private val geminiNanoService = GeminiNanoService()
    private val nutritionSearchService = NutritionSearchService(geminiNanoService)
    private val aiFoodParserService = AiFoodParserService(geminiNanoService)

    private val _mode = MutableStateFlow("search")
    val mode: StateFlow<String> = _mode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<NutritionResult?>(null)
    val searchResult: StateFlow<NutritionResult?> = _searchResult.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _searchStatus = MutableStateFlow<String?>(null)
    val searchStatus: StateFlow<String?> = _searchStatus.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _foodLog = MutableStateFlow<List<DiaryItem>>(emptyList())
    val foodLog: StateFlow<List<DiaryItem>> = _foodLog.asStateFlow()

    private val _calorieGoal = MutableStateFlow(2000)
    val calorieGoal: StateFlow<Int> = _calorieGoal.asStateFlow()

    private val _caloriesEaten = MutableStateFlow(0)
    val caloriesEaten: StateFlow<Int> = _caloriesEaten.asStateFlow()

    private val _caloriesHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val caloriesHistory: StateFlow<List<Pair<String, Int>>> = _caloriesHistory.asStateFlow()

    private val _manualName = MutableStateFlow("")
    val manualName: StateFlow<String> = _manualName.asStateFlow()

    private val _manualCalories = MutableStateFlow("")
    val manualCalories: StateFlow<String> = _manualCalories.asStateFlow()

    private val _manualProtein = MutableStateFlow("")
    val manualProtein: StateFlow<String> = _manualProtein.asStateFlow()

    private val _manualCarbs = MutableStateFlow("")
    val manualCarbs: StateFlow<String> = _manualCarbs.asStateFlow()

    private val _manualFat = MutableStateFlow("")
    val manualFat: StateFlow<String> = _manualFat.asStateFlow()

    private val _manualSugar = MutableStateFlow("")
    val manualSugar: StateFlow<String> = _manualSugar.asStateFlow()

    private val _aiInput = MutableStateFlow("")
    val aiInput: StateFlow<String> = _aiInput.asStateFlow()

    private val _aiResults = MutableStateFlow<List<ParsedFoodItem>>(emptyList())
    val aiResults: StateFlow<List<ParsedFoodItem>> = _aiResults.asStateFlow()

    private val _isAiParsing = MutableStateFlow(false)
    val isAiParsing: StateFlow<Boolean> = _isAiParsing.asStateFlow()

    private val _geminiReady = MutableStateFlow(false)
    val geminiReady: StateFlow<Boolean> = _geminiReady.asStateFlow()

    private val _customFoodSuggestions = MutableStateFlow<List<FoodItem>>(emptyList())
    val customFoodSuggestions: StateFlow<List<FoodItem>> = _customFoodSuggestions.asStateFlow()

    private val _scanResult = MutableStateFlow<NutritionResult?>(null)
    val scanResult: StateFlow<NutritionResult?> = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _showAllFoods = MutableStateFlow(false)
    val showAllFoods: StateFlow<Boolean> = _showAllFoods.asStateFlow()

    private val _allCustomFoods = MutableStateFlow<List<FoodItem>>(emptyList())
    val allCustomFoods: StateFlow<List<FoodItem>> = _allCustomFoods.asStateFlow()

    fun toggleShowAllFoods(show: Boolean) {
        if (show) {
            _allCustomFoods.value =
                    foodRepository.getAllFoodItems().sortedByDescending { it.usageCount }
        }
        _showAllFoods.value = show
    }

    fun removeFoodFromHistory(name: String) {
        foodRepository.removeFoodItem(name)
        _allCustomFoods.value =
                foodRepository.getAllFoodItems().sortedByDescending { it.usageCount }
    }

    fun setMode(mode: String) {
        _mode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateCustomFoodSuggestions(query)
    }

    fun setManualName(value: String) {
        _manualName.value = value
        updateCustomFoodSuggestions(value)
    }
    fun setManualCalories(value: String) {
        _manualCalories.value = value
    }
    fun setManualProtein(value: String) {
        _manualProtein.value = value
    }
    fun setManualCarbs(value: String) {
        _manualCarbs.value = value
    }
    fun setManualFat(value: String) {
        _manualFat.value = value
    }
    fun setManualSugar(value: String) {
        _manualSugar.value = value
    }

    fun setAiInput(value: String) {
        _aiInput.value = value
    }

    fun loadData() {
        viewModelScope.launch {
            val today = todayKey()
            _foodLog.value = foodRepository.getDiary(today)
            _calorieGoal.value = goalsRepository.getCalorieGoal()
            _caloriesEaten.value = foodRepository.getTotalCaloriesToday()
            _caloriesHistory.value = foodRepository.getCaloriesHistory(7)
            _geminiReady.value = geminiNanoService.initIfNeeded()
        }
    }

    fun searchFood() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            _searchResult.value = null
            _searchStatus.value = "Starting search..."
            val result =
                    nutritionSearchService.searchNutrition(query) { status ->
                        _searchStatus.value = status
                    }
            _searchStatus.value = null
            _isSearching.value = false
            if (result != null) {
                _searchResult.value = result
                _searchError.value = null
            } else {
                _searchResult.value = null
                _searchError.value = "No nutrition data found"
            }
        }
    }

    fun addFromSearch(
            targetName: String,
            result: NutritionResult,
            grams: Float,
            unitLabel: String = "g"
    ) {
        val parsedInfo = com.fittrack.app.util.parseServingSize(result.servingDescription)

        // Find if the unitLogLabel matches the original unit
        val baseUnitLabel = parsedInfo.unit.label.lowercase()
        val loggingUnitLabel = unitLabel.lowercase()

        val factor =
                if (loggingUnitLabel.contains(baseUnitLabel) ||
                                baseUnitLabel.contains(loggingUnitLabel.filter { it.isLetter() })
                ) {
                    // If logging in base units, derive amount from grams
                    val amountLogged = grams / parsedInfo.unit.gramsPerUnit
                    amountLogged / parsedInfo.amount.coerceAtLeast(0.1f)
                } else {
                    // Standard weight-based scaling
                    (grams / parsedInfo.baseGrams.coerceAtLeast(1f)).coerceAtLeast(0.01f)
                }
        val qtyStr =
                if (unitLabel == "g") {
                    if (grams == grams.toInt().toFloat()) "${grams.toInt()}g"
                    else "${"%.1f".format(grams)}g"
                } else if (unitLabel.any { it.isLetter() } && unitLabel.any { it.isDigit() }) {
                    // If unitLabel already contains both digits and letters, it's likely a full
                    // string like "100g"
                    unitLabel
                } else {
                    // Otherwise, combine them
                    if (grams == grams.toInt().toFloat()) "${grams.toInt()} $unitLabel"
                    else "${"%.1f".format(grams)} $unitLabel"
                }
        val now = System.currentTimeMillis()
        val entry =
                DiaryItem(
                        id = UUID.randomUUID().toString(),
                        name = targetName,
                        calories = (result.calories * factor).toInt(),
                        protein = round1(result.protein * factor),
                        carbs = round1(result.carbs * factor),
                        fat = round1(result.fat * factor),
                        sugar = round1(result.sugar * factor),
                        timestamp = now,
                        quantity = qtyStr,
                        mealType = getMealType(),
                        foodItemId = targetName
                )
        foodRepository.addDiaryItem(entry, todayKey())
        foodRepository.addOrUpdateFoodItem(
                FoodItem(
                        name = targetName,
                        calories = result.calories,
                        protein = result.protein,
                        carbs = result.carbs,
                        fat = result.fat,
                        sugar = result.sugar,
                        servingDescription = result.servingDescription,
                        usageCount = 1,
                        lastUsed = now
                )
        )
        loadData()
        _searchResult.value = null
        _scanResult.value = null
        _scanError.value = null
        _searchQuery.value = ""
    }

    fun addManual() {
        val name = _manualName.value.trim()
        if (name.isBlank()) return
        val calories = _manualCalories.value.toIntOrNull() ?: 0
        val protein = _manualProtein.value.toFloatOrNull() ?: 0f
        val carbs = _manualCarbs.value.toFloatOrNull() ?: 0f
        val fat = _manualFat.value.toFloatOrNull() ?: 0f
        if (calories <= 0) return
        val now = System.currentTimeMillis()
        val entry =
                DiaryItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        sugar = _manualSugar.value.toFloatOrNull() ?: 0f,
                        timestamp = now,
                        quantity = "1 serving",
                        mealType = getMealType(),
                        foodItemId = name
                )
        foodRepository.addDiaryItem(entry, todayKey())
        foodRepository.addOrUpdateFoodItem(
                FoodItem(
                        name = name,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        sugar = _manualSugar.value.toFloatOrNull() ?: 0f,
                        servingDescription = null,
                        usageCount = 1,
                        lastUsed = now
                )
        )
        loadData()
        _manualName.value = ""
        _manualCalories.value = ""
        _manualProtein.value = ""
        _manualCarbs.value = ""
        _manualFat.value = ""
        _manualSugar.value = ""
    }

    private fun getMealType(): MealType {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 10 -> MealType.BREAKFAST
            hour in 11..15 -> MealType.LUNCH
            hour in 17..21 -> MealType.DINNER
            else -> MealType.SNACK
        }
    }

    fun parseAiInput() {
        val input = _aiInput.value.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            _isAiParsing.value = true
            _aiResults.value = aiFoodParserService.parseFood(input)
            _isAiParsing.value = false
        }
    }

    fun scanNutritionLabel(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            val result = aiFoodParserService.parseNutritionLabel(bitmap)
            _isScanning.value = false
            if (result != null) {
                _scanResult.value = result
            } else {
                _scanError.value = "Could not read nutrition label. Try a clearer photo."
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
        _scanError.value = null
    }

    fun addParsedItem(item: ParsedFoodItem) {
        val now = System.currentTimeMillis()
        val entry =
                DiaryItem(
                        id = UUID.randomUUID().toString(),
                        name = item.name,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        sugar = item.sugar,
                        timestamp = now,
                        quantity = item.quantity,
                        mealType = getMealType(),
                        foodItemId = item.name
                )
        foodRepository.addDiaryItem(entry, todayKey())
        foodRepository.addOrUpdateFoodItem(
                FoodItem(
                        name = item.name,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        sugar = item.sugar,
                        servingDescription = item.quantity,
                        usageCount = 1,
                        lastUsed = now
                )
        )
        // Remove this item from the AI results list immediately
        _aiResults.value = _aiResults.value.filter { it !== item }
        loadData()
    }

    fun addAllParsedItems() {
        _aiResults.value.forEach { addParsedItem(it) }
        _aiResults.value = emptyList()
        _aiInput.value = ""
    }

    private val _editingEntry = MutableStateFlow<DiaryItem?>(null)
    val editingEntry: StateFlow<DiaryItem?> = _editingEntry.asStateFlow()

    private val _editName = MutableStateFlow("")
    val editName: StateFlow<String> = _editName.asStateFlow()

    private val _editCalories = MutableStateFlow("")
    val editCalories: StateFlow<String> = _editCalories.asStateFlow()

    private val _editProtein = MutableStateFlow("")
    val editProtein: StateFlow<String> = _editProtein.asStateFlow()

    private val _editCarbs = MutableStateFlow("")
    val editCarbs: StateFlow<String> = _editCarbs.asStateFlow()

    private val _editFat = MutableStateFlow("")
    val editFat: StateFlow<String> = _editFat.asStateFlow()

    private val _editQuantity = MutableStateFlow("")
    val editQuantity: StateFlow<String> = _editQuantity.asStateFlow()

    private val _autoScale = MutableStateFlow(true)
    val autoScale: StateFlow<Boolean> = _autoScale.asStateFlow()

    fun startEdit(entry: DiaryItem) {
        _editingEntry.value = entry
        _editName.value = entry.name
        _editCalories.value = entry.calories.toString()
        _editProtein.value = fmtVal(entry.protein)
        _editCarbs.value = fmtVal(entry.carbs)
        _editFat.value = fmtVal(entry.fat)
        _editQuantity.value = entry.quantity ?: ""
        _autoScale.value = true
    }

    fun setEditName(v: String) {
        _editName.value = v
    }
    fun setEditCalories(v: String) {
        _editCalories.value = v
    }
    fun setEditProtein(v: String) {
        _editProtein.value = v
    }
    fun setEditCarbs(v: String) {
        _editCarbs.value = v
    }
    fun setEditFat(v: String) {
        _editFat.value = v
    }
    fun setAutoScale(v: Boolean) {
        _autoScale.value = v
    }

    fun setEditQuantity(v: String) {
        _editQuantity.value = v
        if (_autoScale.value) {
            val entry = _editingEntry.value ?: return
            val oldQtyStr = entry.quantity ?: "1"
            val oldQtyMatch = Regex("([0-9]*\\.?[0-9]+)").find(oldQtyStr)
            val newQtyMatch = Regex("([0-9]*\\.?[0-9]+)").find(v)

            if (oldQtyMatch != null && newQtyMatch != null) {
                val oldNum = oldQtyMatch.groupValues[1].toFloatOrNull() ?: 1f
                val newNum = newQtyMatch.groupValues[1].toFloatOrNull() ?: 1f
                if (oldNum > 0) {
                    val ratio = newNum / oldNum
                    _editCalories.value = (entry.calories * ratio).toInt().toString()
                    _editProtein.value = fmtVal(entry.protein * ratio)
                    _editCarbs.value = fmtVal(entry.carbs * ratio)
                    _editFat.value = fmtVal(entry.fat * ratio)
                }
            }
        }
    }

    fun adjustQuantity(increment: Boolean) {
        val currentQty = _editQuantity.value
        val match = Regex("([0-9]*\\.?[0-9]+)").find(currentQty)
        if (match != null) {
            val currentNum = match.groupValues[1].toFloatOrNull() ?: 0f
            val step =
                    if (currentNum >= 10) {
                        if (currentNum % 10 == 0f || currentNum % 5 == 0f) 5f else 1f
                    } else if (currentNum >= 1) 1f else 0.5f
            val newNum = if (increment) currentNum + step else currentNum - step
            if (newNum > 0) {
                val formattedStr =
                        if (newNum % 1f == 0f) newNum.toInt().toString() else fmtVal(newNum)
                val newQtyStr = currentQty.replaceFirst(match.groupValues[1], formattedStr)
                setEditQuantity(newQtyStr)
            }
        }
    }

    fun saveEdit() {
        val entry = _editingEntry.value ?: return
        val updated =
                entry.copy(
                        name = _editName.value.trim().ifBlank { entry.name },
                        calories = _editCalories.value.toIntOrNull() ?: entry.calories,
                        protein = _editProtein.value.toFloatOrNull() ?: entry.protein,
                        carbs = _editCarbs.value.toFloatOrNull() ?: entry.carbs,
                        fat = _editFat.value.toFloatOrNull() ?: entry.fat,
                        quantity = _editQuantity.value.trim().ifBlank { null }
                )
        foodRepository.updateDiaryItem(updated, todayKey())
        _editingEntry.value = null
        loadData()
    }

    fun cancelEdit() {
        _editingEntry.value = null
    }

    fun removeEntry(id: String) {
        foodRepository.removeDiaryItem(id, todayKey())
        loadData()
    }

    fun updateCustomFoodSuggestions(query: String) {
        _customFoodSuggestions.value = foodRepository.searchFoodItems(query)
    }

    fun selectCustomFoodSuggestion(food: FoodItem) {
        when (_mode.value) {
            "search" -> {
                _searchQuery.value = food.name
                _searchResult.value =
                        NutritionResult(
                                name = food.name,
                                calories = food.calories,
                                protein = food.protein,
                                carbs = food.carbs,
                                fat = food.fat,
                                servingDescription = food.servingDescription
                        )
                _customFoodSuggestions.value = emptyList()
            }
            "manual" -> {
                _manualName.value = food.name
                _manualCalories.value = food.calories.toString()
                _manualProtein.value = food.protein.toString()
                _manualCarbs.value = food.carbs.toString()
                _manualFat.value = food.fat.toString()
                _customFoodSuggestions.value = emptyList()
            }
            else -> {}
        }
    }

    private fun round1(v: Float): Float = kotlin.math.round(v * 10) / 10f

    private fun fmtVal(v: Float): String {
        val rounded = round1(v)
        return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString()
        else "%.1f".format(rounded)
    }

    fun clearSearchResult() {
        _searchResult.value = null
        _searchError.value = null
    }
}
