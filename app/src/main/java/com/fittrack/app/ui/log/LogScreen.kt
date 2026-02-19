package com.fittrack.app.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.data.CustomFood
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum

@Composable
fun LogScreen(
    viewModel: LogViewModel = viewModel()
) {
    val mode by viewModel.mode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val foodLog by viewModel.foodLog.collectAsState()
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val caloriesEaten by viewModel.caloriesEaten.collectAsState()
    val manualName by viewModel.manualName.collectAsState()
    val manualCalories by viewModel.manualCalories.collectAsState()
    val manualProtein by viewModel.manualProtein.collectAsState()
    val manualCarbs by viewModel.manualCarbs.collectAsState()
    val manualFat by viewModel.manualFat.collectAsState()
    val aiInput by viewModel.aiInput.collectAsState()
    val aiResults by viewModel.aiResults.collectAsState()
    val isAiParsing by viewModel.isAiParsing.collectAsState()
    val geminiReady by viewModel.geminiReady.collectAsState()
    val customFoodSuggestions by viewModel.customFoodSuggestions.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SummaryCard(
            caloriesEaten = caloriesEaten,
            calorieGoal = calorieGoal
        )

        Spacer(modifier = Modifier.height(20.dp))

        ModeToggleRow(
            mode = mode,
            onModeChange = { viewModel.setMode(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (mode) {
            "search" -> SearchModeContent(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = { viewModel.searchFood() },
                isSearching = isSearching,
                searchResult = searchResult,
                searchError = searchError,
                customFoodSuggestions = customFoodSuggestions,
                onSelectCustomFood = { viewModel.selectCustomFoodSuggestion(it) },
                onAddFromSearch = { result, grams, unitLabel -> viewModel.addFromSearch(result, grams, unitLabel) }
            )
            "ai" -> AiModeContent(
                aiInput = aiInput,
                onAiInputChange = { viewModel.setAiInput(it) },
                onParse = { viewModel.parseAiInput() },
                isAiParsing = isAiParsing,
                aiResults = aiResults,
                geminiReady = geminiReady,
                onAddParsedItem = { viewModel.addParsedItem(it) },
                onAddAllParsed = { viewModel.addAllParsedItems() }
            )
            "scan" -> ScanModeContent(
                geminiReady = geminiReady,
                isScanning = isScanning,
                scanResult = scanResult,
                scanError = scanError,
                onScan = { bitmap -> viewModel.scanNutritionLabel(bitmap) },
                onAddFromScan = { result, grams, unitLabel -> viewModel.addFromSearch(result, grams, unitLabel) },
                onClearScan = { viewModel.clearScanResult() }
            )
            "manual" -> ManualModeContent(
                manualName = manualName,
                manualCalories = manualCalories,
                manualProtein = manualProtein,
                manualCarbs = manualCarbs,
                manualFat = manualFat,
                onNameChange = { viewModel.setManualName(it) },
                onCaloriesChange = { viewModel.setManualCalories(it) },
                onProteinChange = { viewModel.setManualProtein(it) },
                onCarbsChange = { viewModel.setManualCarbs(it) },
                onFatChange = { viewModel.setManualFat(it) },
                onAdd = { viewModel.addManual() },
                customFoodSuggestions = customFoodSuggestions,
                onSelectCustomFood = { viewModel.selectCustomFoodSuggestion(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Today's Food",
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        foodLog.forEach { entry ->
            FoodItemCard(
                entry = entry,
                onEdit = { viewModel.startEdit(entry) },
                onDelete = { viewModel.removeEntry(entry.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    val editingEntry by viewModel.editingEntry.collectAsState()
    if (editingEntry != null) {
        EditFoodDialog(viewModel = viewModel)
    }
}

@Composable
private fun SummaryCard(
    caloriesEaten: Int,
    calorieGoal: Int
) {
    val progress = if (calorieGoal > 0) (caloriesEaten.toFloat() / calorieGoal).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${fmtNum(caloriesEaten)} / ${fmtNum(calorieGoal)} kcal",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = AppColors.primary,
                trackColor = AppColors.surfaceVariant
            )
        }
    }
}

@Composable
private fun ModeToggleRow(
    mode: String,
    onModeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("search" to "Search", "ai" to "AI", "scan" to "Scan", "manual" to "Manual").forEach { (id, label) ->
            FilterChip(
                selected = mode == id,
                onClick = { onModeChange(id) },
                label = {
                    Text(
                        text = label,
                        fontFamily = interFamily,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = when (id) {
                    "search" -> { { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) } }
                    "ai" -> { { Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) } }
                    "scan" -> { { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) } }
                    "manual" -> { { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp)) } }
                    else -> null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.primary,
                    selectedLabelColor = AppColors.textOnPrimary,
                    selectedLeadingIconColor = AppColors.textOnPrimary
                )
            )
        }
    }
}

@Composable
private fun SearchModeContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    searchResult: com.fittrack.app.data.NutritionResult?,
    searchError: String?,
    customFoodSuggestions: List<CustomFood>,
    onSelectCustomFood: (CustomFood) -> Unit,
    onAddFromSearch: (com.fittrack.app.data.NutritionResult, Float, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search food", fontFamily = interFamily) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = onSearch,
                enabled = !isSearching && searchQuery.isNotBlank()
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = AppColors.textOnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Search", fontFamily = interFamily)
                }
            }
        }

        if (customFoodSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            customFoodSuggestions.take(5).forEach { food ->
                Text(
                    text = food.name,
                    fontFamily = interFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.primary,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable { onSelectCustomFood(food) }
                )
            }
        }

        searchError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontFamily = interFamily,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.error
            )
        }

        searchResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
            NutritionCard(
                result = result,
                onAdd = onAddFromSearch
            )
        }
    }
}

@Composable
private fun AiModeContent(
    aiInput: String,
    onAiInputChange: (String) -> Unit,
    onParse: () -> Unit,
    isAiParsing: Boolean,
    aiResults: List<com.fittrack.app.data.ParsedFoodItem>,
    geminiReady: Boolean,
    onAddParsedItem: (com.fittrack.app.data.ParsedFoodItem) -> Unit,
    onAddAllParsed: () -> Unit
) {
    Column {
        if (!geminiReady) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.warning.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = AppColors.warning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini Nano Unavailable",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "On-device AI requires Gemini Nano, which needs to be downloaded by your device. This can take a few hours after initial setup. Make sure your device is connected to WiFi and try again later.\n\nIn the meantime, use Search or Manual mode to log food.",
                        fontFamily = interFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            return
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = null,
                tint = AppColors.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Describe what you ate...",
                fontFamily = interFamily,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary
            )
        }

        OutlinedTextField(
            value = aiInput,
            onValueChange = onAiInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("e.g. 2 scrambled eggs with toast and butter", fontFamily = interFamily) },
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onParse,
            enabled = !isAiParsing && aiInput.isNotBlank()
        ) {
            if (isAiParsing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp),
                    color = AppColors.textOnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Parse with AI", fontFamily = interFamily)
            }
        }

        if (aiResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            aiResults.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.name} (${item.quantity})",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = "${fmtNum(item.calories)} kcal • ${fmtMacro(item.protein)}g P • ${fmtMacro(item.carbs)}g C • ${fmtMacro(item.fat)}g F",
                            fontFamily = interFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary
                        )
                    }
                    Button(
                        onClick = { onAddParsedItem(item) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Add", fontFamily = interFamily, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddAllParsed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add All", fontFamily = interFamily)
            }
        }
    }
}

@Composable
private fun ManualModeContent(
    manualName: String,
    manualCalories: String,
    manualProtein: String,
    manualCarbs: String,
    manualFat: String,
    onNameChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onAdd: () -> Unit,
    customFoodSuggestions: List<CustomFood>,
    onSelectCustomFood: (CustomFood) -> Unit
) {
    Column {
        OutlinedTextField(
            value = manualName,
            onValueChange = onNameChange,
            label = { Text("Food name", fontFamily = interFamily) },
            modifier = Modifier.fillMaxWidth()
        )

        if (customFoodSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            customFoodSuggestions.take(5).forEach { food ->
                Text(
                    text = food.name,
                    fontFamily = interFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.primary,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clickable { onSelectCustomFood(food) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = manualCalories,
            onValueChange = onCaloriesChange,
            label = { Text("Calories", fontFamily = interFamily) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = manualProtein,
                onValueChange = onProteinChange,
                label = { Text("Protein (g)", fontFamily = interFamily) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = manualCarbs,
                onValueChange = onCarbsChange,
                label = { Text("Carbs (g)", fontFamily = interFamily) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = manualFat,
                onValueChange = onFatChange,
                label = { Text("Fat (g)", fontFamily = interFamily) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            enabled = manualName.isNotBlank() && manualCalories.toIntOrNull() != null && (manualCalories.toIntOrNull() ?: 0) > 0
        ) {
            Text("Add Entry", fontFamily = interFamily)
        }
    }
}

@Composable
private fun ScanModeContent(
    geminiReady: Boolean,
    isScanning: Boolean,
    scanResult: NutritionResult?,
    scanError: String?,
    onScan: (Bitmap) -> Unit,
    onAddFromScan: (NutritionResult, Float, String) -> Unit,
    onClearScan: () -> Unit
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            onScan(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    onScan(bitmap)
                }
            } catch (_: Exception) { }
        }
    }

    Column {
        if (!geminiReady) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gemini Nano Required",
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = "Label scanning requires Gemini Nano, which is not ready on this device yet.",
                        fontFamily = interFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary
                    )
                }
            }
            return
        }

        Text(
            text = "Scan a nutrition label",
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textPrimary
        )
        Text(
            text = "Take a photo or pick from gallery",
            fontFamily = interFamily,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.textOnPrimary
                )
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera", fontFamily = interFamily)
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Gallery", fontFamily = interFamily)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isScanning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                Text("Reading label...", fontFamily = interFamily, color = AppColors.textSecondary)
            }
        }

        scanError?.let { error ->
            Text(
                text = error,
                fontFamily = interFamily,
                color = AppColors.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        scanResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            NutritionCard(
                result = result,
                onAdd = onAddFromScan
            )
        }
    }
}

private fun fmtMacro(v: Float): String {
    val rounded = kotlin.math.round(v * 10) / 10f
    return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString()
    else "%.1f".format(rounded)
}

@Composable
private fun EditFoodDialog(viewModel: LogViewModel) {
    val editName by viewModel.editName.collectAsState()
    val editCalories by viewModel.editCalories.collectAsState()
    val editProtein by viewModel.editProtein.collectAsState()
    val editCarbs by viewModel.editCarbs.collectAsState()
    val editFat by viewModel.editFat.collectAsState()
    val editQuantity by viewModel.editQuantity.collectAsState()

    AlertDialog(
        onDismissRequest = { viewModel.cancelEdit() },
        title = {
            Text(
                text = "Edit Entry",
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { viewModel.setEditName(it) },
                    label = { Text("Name", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editQuantity,
                    onValueChange = { viewModel.setEditQuantity(it) },
                    label = { Text("Quantity (e.g. 3 large, 1 lb)", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editCalories,
                    onValueChange = { viewModel.setEditCalories(it) },
                    label = { Text("Calories", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProtein,
                        onValueChange = { viewModel.setEditProtein(it) },
                        label = { Text("Protein (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCarbs,
                        onValueChange = { viewModel.setEditCarbs(it) },
                        label = { Text("Carbs (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editFat,
                        onValueChange = { viewModel.setEditFat(it) },
                        label = { Text("Fat (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveEdit() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primary,
                    contentColor = AppColors.textOnPrimary
                )
            ) {
                Text("Save", fontFamily = interFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelEdit() }) {
                Text("Cancel", fontFamily = interFamily, color = AppColors.textSecondary)
            }
        }
    )
}
