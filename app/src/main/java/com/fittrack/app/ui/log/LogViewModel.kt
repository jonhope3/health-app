package com.fittrack.app.ui.log

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
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
import com.fittrack.app.util.formatMacro
import com.fittrack.app.util.parseServingSize
import com.fittrack.app.util.todayKey
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LogViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val goalsRepository: GoalsRepository,
    private val geminiNanoService: GeminiNanoService,
) : ViewModel() {

    private val nutritionSearchService = NutritionSearchService(geminiNanoService)
    private val aiFoodParserService    = AiFoodParserService(geminiNanoService)

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _mode                 = MutableStateFlow("search")
    private val _searchQuery          = MutableStateFlow("")
    private val _searchResult         = MutableStateFlow<NutritionResult?>(null)
    private val _searchError          = MutableStateFlow<String?>(null)
    private val _searchStatus         = MutableStateFlow<String?>(null)
    private val _isSearching          = MutableStateFlow(false)
    private val _foodLog              = MutableStateFlow<List<DiaryItem>>(emptyList())
    private val _calorieGoal          = MutableStateFlow(2000)
    private val _caloriesEaten        = MutableStateFlow(0)
    private val _caloriesHistory      = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    private val _manualName           = MutableStateFlow("")
    private val _manualCalories       = MutableStateFlow("")
    private val _manualProtein        = MutableStateFlow("")
    private val _manualCarbs          = MutableStateFlow("")
    private val _manualFat            = MutableStateFlow("")
    private val _manualSugar          = MutableStateFlow("")
    private val _aiInput              = MutableStateFlow("")
    private val _aiResults            = MutableStateFlow<List<ParsedFoodItem>>(emptyList())
    private val _isAiParsing          = MutableStateFlow(false)
    private val _geminiReady          = MutableStateFlow(false)
    private val _customFoodSuggestions = MutableStateFlow<List<FoodItem>>(emptyList())
    private val _scanResult           = MutableStateFlow<NutritionResult?>(null)
    private val _isScanning           = MutableStateFlow(false)
    private val _scanError            = MutableStateFlow<String?>(null)
    private val _showAllFoods         = MutableStateFlow(false)
    private val _allCustomFoods       = MutableStateFlow<List<FoodItem>>(emptyList())
    /** The meal type name to highlight/scroll to, set from HomeScreen deep-link. */
    private val _mealFilter           = MutableStateFlow<String?>(null)

    // Edit dialog
    private val _editingEntry   = MutableStateFlow<DiaryItem?>(null)
    private val _editName       = MutableStateFlow("")
    private val _editCalories   = MutableStateFlow("")
    private val _editProtein    = MutableStateFlow("")
    private val _editCarbs      = MutableStateFlow("")
    private val _editFat        = MutableStateFlow("")
    private val _editSugar      = MutableStateFlow("")
    private val _editQuantity   = MutableStateFlow("")
    private val _autoScale      = MutableStateFlow(true)

    val mode:                  StateFlow<String>               = _mode.asStateFlow()
    val searchQuery:           StateFlow<String>               = _searchQuery.asStateFlow()
    val searchResult:          StateFlow<NutritionResult?>     = _searchResult.asStateFlow()
    val searchError:           StateFlow<String?>              = _searchError.asStateFlow()
    val searchStatus:          StateFlow<String?>              = _searchStatus.asStateFlow()
    val isSearching:           StateFlow<Boolean>              = _isSearching.asStateFlow()
    val foodLog:               StateFlow<List<DiaryItem>>      = _foodLog.asStateFlow()
    val calorieGoal:           StateFlow<Int>                  = _calorieGoal.asStateFlow()
    val caloriesEaten:         StateFlow<Int>                  = _caloriesEaten.asStateFlow()
    val caloriesHistory:       StateFlow<List<Pair<String, Int>>> = _caloriesHistory.asStateFlow()
    val manualName:            StateFlow<String>               = _manualName.asStateFlow()
    val manualCalories:        StateFlow<String>               = _manualCalories.asStateFlow()
    val manualProtein:         StateFlow<String>               = _manualProtein.asStateFlow()
    val manualCarbs:           StateFlow<String>               = _manualCarbs.asStateFlow()
    val manualFat:             StateFlow<String>               = _manualFat.asStateFlow()
    val manualSugar:           StateFlow<String>               = _manualSugar.asStateFlow()
    val aiInput:               StateFlow<String>               = _aiInput.asStateFlow()
    val aiResults:             StateFlow<List<ParsedFoodItem>> = _aiResults.asStateFlow()
    val isAiParsing:           StateFlow<Boolean>              = _isAiParsing.asStateFlow()
    val geminiReady:           StateFlow<Boolean>              = _geminiReady.asStateFlow()
    val customFoodSuggestions: StateFlow<List<FoodItem>>       = _customFoodSuggestions.asStateFlow()
    val scanResult:            StateFlow<NutritionResult?>     = _scanResult.asStateFlow()
    val isScanning:            StateFlow<Boolean>              = _isScanning.asStateFlow()
    val scanError:             StateFlow<String?>              = _scanError.asStateFlow()
    val showAllFoods:          StateFlow<Boolean>              = _showAllFoods.asStateFlow()
    val allCustomFoods:        StateFlow<List<FoodItem>>       = _allCustomFoods.asStateFlow()
    val editingEntry:          StateFlow<DiaryItem?>           = _editingEntry.asStateFlow()
    val editName:              StateFlow<String>               = _editName.asStateFlow()
    val editCalories:          StateFlow<String>               = _editCalories.asStateFlow()
    val editProtein:           StateFlow<String>               = _editProtein.asStateFlow()
    val editCarbs:             StateFlow<String>               = _editCarbs.asStateFlow()
    val editFat:               StateFlow<String>               = _editFat.asStateFlow()
    val editSugar:             StateFlow<String>               = _editSugar.asStateFlow()
    val editQuantity:          StateFlow<String>               = _editQuantity.asStateFlow()
    val autoScale:             StateFlow<Boolean>              = _autoScale.asStateFlow()
    val mealFilter:            StateFlow<String?>              = _mealFilter.asStateFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun setMealFilter(filter: String?) { _mealFilter.value = filter }

    fun loadData() {
        viewModelScope.launch {
            val today = todayKey()
            _foodLog.value         = foodRepository.getDiary(today)
            _calorieGoal.value     = goalsRepository.getCalorieGoal()
            _caloriesEaten.value   = foodRepository.getTotalCaloriesToday()
            _caloriesHistory.value = foodRepository.getCaloriesHistory(7)
            runCatching { _geminiReady.value = geminiNanoService.initIfNeeded() }
        }
    }

    // ── Search mode ───────────────────────────────────────────────────────────

    fun setMode(mode: String) { _mode.value = mode }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateCustomFoodSuggestions(query)
    }

    fun searchFood() {
        val query = _searchQuery.value.trim().ifBlank { return }
        viewModelScope.launch {
            _isSearching.value  = true
            _searchError.value  = null
            _searchResult.value = null
            _searchStatus.value = "Starting search…"

            val result = nutritionSearchService.searchNutrition(query) { status ->
                _searchStatus.value = status
            }

            _searchStatus.value = null
            _isSearching.value  = false
            _searchResult.value = result
            if (result == null) _searchError.value = "No nutrition data found"
        }
    }

    fun clearSearchResult() {
        _searchResult.value = null
        _searchError.value  = null
    }

    fun addFromSearch(
        targetName: String,
        result: NutritionResult,
        grams: Float,
        unitLabel: String = "g",
    ) {
        val parsedInfo = parseServingSize(result.servingDescription)
        val factor = (grams / parsedInfo.baseGrams.coerceAtLeast(1f)).coerceAtLeast(0.01f)
        val qtyStr = formatQuantityLabel(grams, unitLabel)
        val now = System.currentTimeMillis()

        val entry = DiaryItem(
            id       = UUID.randomUUID().toString(),
            name     = targetName,
            calories = (result.calories * factor).toInt(),
            protein  = scale(result.protein, factor),
            carbs    = scale(result.carbs, factor),
            fat      = scale(result.fat, factor),
            sugar    = scale(result.sugar, factor),
            timestamp = now,
            quantity  = qtyStr,
            mealType  = currentMealType(),
            foodItemId = targetName,
        )

        viewModelScope.launch {
            foodRepository.addDiaryItem(entry, todayKey())
            foodRepository.addOrUpdateFoodItem(
                FoodItem(
                    name               = targetName,
                    calories           = result.calories,
                    protein            = result.protein,
                    carbs              = result.carbs,
                    fat                = result.fat,
                    sugar              = result.sugar,
                    servingDescription = result.servingDescription,
                    usageCount         = 1,
                    lastUsed           = now,
                )
            )
            loadData()
        }

        _searchResult.value = null
        _scanResult.value   = null
        _scanError.value    = null
        _searchQuery.value  = ""
    }

    // ── Manual mode ───────────────────────────────────────────────────────────

    fun setManualName(value: String)     { _manualName.value = value; updateCustomFoodSuggestions(value) }
    fun setManualCalories(value: String) { _manualCalories.value = value }
    fun setManualProtein(value: String)  { _manualProtein.value = value }
    fun setManualCarbs(value: String)    { _manualCarbs.value = value }
    fun setManualFat(value: String)      { _manualFat.value = value }
    fun setManualSugar(value: String)    { _manualSugar.value = value }

    fun addManual() {
        val name     = _manualName.value.trim().ifBlank { return }
        val calories = _manualCalories.value.toIntOrNull()?.takeIf { it > 0 } ?: return
        val now = System.currentTimeMillis()

        val entry = DiaryItem(
            id        = UUID.randomUUID().toString(),
            name      = name,
            calories  = calories,
            protein   = _manualProtein.value.toFloatOrNull() ?: 0f,
            carbs     = _manualCarbs.value.toFloatOrNull()   ?: 0f,
            fat       = _manualFat.value.toFloatOrNull()     ?: 0f,
            sugar     = _manualSugar.value.toFloatOrNull()   ?: 0f,
            timestamp = now,
            quantity  = "1 serving",
            mealType  = currentMealType(),
            foodItemId = name,
        )

        viewModelScope.launch {
            foodRepository.addDiaryItem(entry, todayKey())
            foodRepository.addOrUpdateFoodItem(
                FoodItem(
                    name       = name,
                    calories   = entry.calories,
                    protein    = entry.protein,
                    carbs      = entry.carbs,
                    fat        = entry.fat,
                    sugar      = entry.sugar,
                    usageCount = 1,
                    lastUsed   = now,
                )
            )
            loadData()
        }

        _manualName.value     = ""
        _manualCalories.value = ""
        _manualProtein.value  = ""
        _manualCarbs.value    = ""
        _manualFat.value      = ""
        _manualSugar.value    = ""
    }

    // ── AI mode ───────────────────────────────────────────────────────────────

    fun setAiInput(value: String) { _aiInput.value = value }

    fun parseAiInput() {
        val input = _aiInput.value.trim().ifBlank { return }
        viewModelScope.launch {
            _isAiParsing.value = true
            _aiResults.value   = aiFoodParserService.parseFood(input)
            _isAiParsing.value = false
        }
    }

    fun addParsedItem(item: ParsedFoodItem) {
        viewModelScope.launch {
            logParsedItem(item)
            loadData()
        }
        _aiResults.value = _aiResults.value.filter { it !== item }
    }

    fun addAllParsedItems() {
        val items = _aiResults.value.toList()
        viewModelScope.launch {
            items.forEach { logParsedItem(it) }
            loadData()
        }
        _aiResults.value = emptyList()
        _aiInput.value   = ""
    }

    private suspend fun logParsedItem(item: ParsedFoodItem) {
        val now = System.currentTimeMillis()
        foodRepository.addDiaryItem(
            DiaryItem(
                id        = UUID.randomUUID().toString(),
                name      = item.name,
                calories  = item.calories,
                protein   = item.protein,
                carbs     = item.carbs,
                fat       = item.fat,
                sugar     = item.sugar,
                timestamp = now,
                quantity  = item.quantity,
                mealType  = currentMealType(),
                foodItemId = item.name,
            ),
            todayKey(),
        )
        foodRepository.addOrUpdateFoodItem(
            FoodItem(
                name               = item.name,
                calories           = item.calories,
                protein            = item.protein,
                carbs              = item.carbs,
                fat                = item.fat,
                sugar              = item.sugar,
                servingDescription = item.quantity,
                usageCount         = 1,
                lastUsed           = now,
            )
        )
    }

    // ── Scan mode ─────────────────────────────────────────────────────────────

    fun scanNutritionLabel(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value  = true
            _scanError.value   = null
            _scanResult.value  = null
            val result         = aiFoodParserService.parseNutritionLabel(bitmap)
            _isScanning.value  = false
            if (result != null) _scanResult.value = result
            else _scanError.value = "Could not read label. Try a clearer photo."
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
        _scanError.value  = null
    }

    // ── Food history / suggestions ────────────────────────────────────────────

    fun updateCustomFoodSuggestions(query: String) {
        viewModelScope.launch {
            _customFoodSuggestions.value = foodRepository.searchFoodItems(query)
        }
    }

    fun selectCustomFoodSuggestion(food: FoodItem) {
        when (_mode.value) {
            "search" -> {
                _searchQuery.value  = food.name
                _searchResult.value = NutritionResult(
                    name               = food.name,
                    calories           = food.calories,
                    protein            = food.protein,
                    carbs              = food.carbs,
                    fat                = food.fat,
                    sugar              = food.sugar,
                    servingDescription = food.servingDescription,
                )
                _customFoodSuggestions.value = emptyList()
            }
            "manual" -> {
                _manualName.value     = food.name
                _manualCalories.value = food.calories.toString()
                _manualProtein.value  = food.protein.toString()
                _manualCarbs.value    = food.carbs.toString()
                _manualFat.value      = food.fat.toString()
                _manualSugar.value    = food.sugar.toString()
                _customFoodSuggestions.value = emptyList()
            }
        }
    }

    fun toggleShowAllFoods(show: Boolean) {
        viewModelScope.launch {
            if (show) _allCustomFoods.value = foodRepository.getAllFoodItems()
                .sortedByDescending { it.usageCount }
            _showAllFoods.value = show
        }
    }

    fun removeFoodFromHistory(name: String) {
        viewModelScope.launch {
            foodRepository.removeFoodItem(name)
            _allCustomFoods.value = foodRepository.getAllFoodItems()
                .sortedByDescending { it.usageCount }
        }
    }

    // ── Edit dialog ───────────────────────────────────────────────────────────

    fun startEdit(entry: DiaryItem) {
        _editingEntry.value = entry
        _editName.value     = entry.name
        _editCalories.value = entry.calories.toString()
        _editProtein.value  = formatMacro(entry.protein)
        _editCarbs.value    = formatMacro(entry.carbs)
        _editFat.value      = formatMacro(entry.fat)
        _editSugar.value    = formatMacro(entry.sugar)
        _editQuantity.value = entry.quantity ?: ""
        _autoScale.value    = true
    }

    fun setEditName(v: String)     { _editName.value = v }
    fun setEditCalories(v: String) { _editCalories.value = v }
    fun setEditProtein(v: String)  { _editProtein.value = v }
    fun setEditCarbs(v: String)    { _editCarbs.value = v }
    fun setEditFat(v: String)      { _editFat.value = v }
    fun setEditSugar(v: String)    { _editSugar.value = v }
    fun setAutoScale(v: Boolean)   { _autoScale.value = v }

    fun setEditQuantity(v: String) {
        _editQuantity.value = v
        if (!_autoScale.value) return

        val entry    = _editingEntry.value ?: return
        val oldNum   = Regex("""([\d.]+)""").find(entry.quantity ?: "1")?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        val newNum   = Regex("""([\d.]+)""").find(v)?.groupValues?.get(1)?.toFloatOrNull() ?: 1f
        if (oldNum <= 0f) return

        val ratio = newNum / oldNum
        _editCalories.value = (entry.calories * ratio).toInt().toString()
        _editProtein.value  = formatMacro(entry.protein * ratio)
        _editCarbs.value    = formatMacro(entry.carbs   * ratio)
        _editFat.value      = formatMacro(entry.fat     * ratio)
        _editSugar.value    = formatMacro(entry.sugar   * ratio)
    }

    fun adjustQuantity(increment: Boolean) {
        val current = _editQuantity.value
        val match   = Regex("""([\d.]+)""").find(current) ?: return
        val num     = match.groupValues[1].toFloatOrNull() ?: return
        val step    = when {
            num >= 10f && (num % 5f == 0f) -> 5f
            num >= 10f                      -> 1f
            num >= 1f                       -> 1f
            else                            -> 0.5f
        }
        val newNum = (if (increment) num + step else num - step).takeIf { it > 0f } ?: return
        val formatted = if (newNum % 1f == 0f) newNum.toInt().toString() else formatMacro(newNum)
        setEditQuantity(current.replaceFirst(match.groupValues[1], formatted))
    }

    fun saveEdit() {
        val entry   = _editingEntry.value ?: return
        val updated = entry.copy(
            name     = _editName.value.trim().ifBlank { entry.name },
            calories = _editCalories.value.toIntOrNull()   ?: entry.calories,
            protein  = _editProtein.value.toFloatOrNull()  ?: entry.protein,
            carbs    = _editCarbs.value.toFloatOrNull()    ?: entry.carbs,
            fat      = _editFat.value.toFloatOrNull()      ?: entry.fat,
            sugar    = _editSugar.value.toFloatOrNull()    ?: entry.sugar,
            quantity = _editQuantity.value.trim().ifBlank { null },
        )
        viewModelScope.launch {
            foodRepository.updateDiaryItem(updated)
            loadData()
        }
        _editingEntry.value = null
    }

    fun cancelEdit() { _editingEntry.value = null }

    fun removeEntry(id: String) {
        viewModelScope.launch {
            foodRepository.removeDiaryItem(id)
            loadData()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun currentMealType(): MealType = when (LocalTime.now().hour) {
        in 0..9   -> MealType.BREAKFAST
        in 10..14 -> MealType.LUNCH
        in 15..20 -> MealType.DINNER
        else      -> MealType.SNACK
    }

    private fun scale(value: Float, factor: Float): Float =
        round(value * factor * 10f) / 10f

    private fun formatQuantityLabel(grams: Float, unitLabel: String): String {
        val numStr = if (grams == grams.toInt().toFloat()) grams.toInt().toString()
                     else "%.1f".format(grams)
        return if (unitLabel == "g") "${numStr}g"
               else if (unitLabel.any { it.isLetter() } && unitLabel.any { it.isDigit() }) unitLabel
               else "$numStr $unitLabel"
    }
}
