package com.fittrack.app.ui.log

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.fittrack.app.data.FoodItem
import com.fittrack.app.data.DiaryItem
import com.fittrack.app.data.NutritionResult
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel

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
    val showAllFoods by viewModel.showAllFoods.collectAsState()
    val allCustomFoods by viewModel.allCustomFoods.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "Log Food",
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = AppColors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                onAddFromSearch = { targetName, result, grams, unitLabel -> viewModel.addFromSearch(targetName, result, grams, unitLabel) }
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
                onAddFromScan = { targetName, result, grams, unitLabel -> viewModel.addFromSearch(targetName, result, grams, unitLabel) },
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Food Diary",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.textPrimary
            )
            TextButton(onClick = { viewModel.toggleShowAllFoods(true) }) {
                Text(
                    text = "Browse History",
                    fontFamily = interFamily,
                    color = AppColors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        foodLog.forEachIndexed { index, entry ->
            TimelineDiaryItem(
                entry = entry,
                isFirst = index == 0,
                isLast = index == foodLog.size - 1,
                onEdit = { viewModel.startEdit(entry) },
                onDelete = { viewModel.removeEntry(entry.id) }
            )
        }
    }

    val editingEntry by viewModel.editingEntry.collectAsState()
    if (editingEntry != null) {
        EditFoodDialog(viewModel = viewModel)
    }

    if (showAllFoods) {
        AllFoodsDialog(
            foods = allCustomFoods,
            onDismiss = { viewModel.toggleShowAllFoods(false) },
            onSelect = { 
                viewModel.selectCustomFoodSuggestion(it)
                viewModel.toggleShowAllFoods(false)
            },
            onRemove = { viewModel.removeFoodFromHistory(it.name) }
        )
    }
}

@Composable
private fun TimelineDiaryItem(
    entry: DiaryItem,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(if (isFirst) Color.Transparent else AppColors.border)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(AppColors.primary, CircleShape)
                    .border(2.dp, AppColors.surface, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(if (isLast) Color.Transparent else AppColors.border)
            )
        }
        
        Column(modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)) {
            DiaryItemCard(entry, onEdit, onDelete)
        }
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AppColors.primary,
                trackColor = AppColors.surfaceVariant,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun ModeToggleRow(
    mode: String,
    onModeChange: (String) -> Unit
) {
    val modes = listOf(
        Triple("search", "Search", Icons.Filled.Search),
        Triple("ai", "AI", Icons.Filled.SmartToy),
        Triple("scan", "Scan", Icons.Filled.CameraAlt),
        Triple("manual", "Manual", Icons.Filled.Add)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeCard(
                modeItem = modes[0],
                isSelected = mode == modes[0].first,
                onModeChange = onModeChange,
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                modeItem = modes[1],
                isSelected = mode == modes[1].first,
                onModeChange = onModeChange,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeCard(
                modeItem = modes[2],
                isSelected = mode == modes[2].first,
                onModeChange = onModeChange,
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                modeItem = modes[3],
                isSelected = mode == modes[3].first,
                onModeChange = onModeChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeCard(
    modeItem: Triple<String, String, ImageVector>,
    isSelected: Boolean,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (id, label, icon) = modeItem
    
    val containerColor by animateColorAsState(
        if (isSelected) AppColors.primary else AppColors.surfaceVariant,
        label = "bg_anim"
    )
    val contentColor by animateColorAsState(
        if (isSelected) AppColors.surface else AppColors.textPrimary,
        label = "text_anim"
    )
    val elevation by animateDpAsState(
        if (isSelected) 6.dp else 2.dp,
        label = "elev_anim"
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onModeChange(id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontFamily = interFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
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
    customFoodSuggestions: List<FoodItem>,
    onSelectCustomFood: (FoodItem) -> Unit,
    onAddFromSearch: (String, com.fittrack.app.data.NutritionResult, Float, String) -> Unit
) {
    Column {
        Text(
            text = "Search Database",
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
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
                .height(140.dp),
            placeholder = { Text("e.g. 2 scrambled eggs with toast and butter", fontFamily = interFamily) },
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.surface,
                unfocusedContainerColor = AppColors.surface,
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.border
            )
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
    customFoodSuggestions: List<FoodItem>,
    onSelectCustomFood: (FoodItem) -> Unit
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
    onAddFromScan: (String, NutritionResult, Float, String) -> Unit,
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
            // ... (keep existing warning card logic if needed, or redesign it too)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.warning.copy(alpha = 0.1f))
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
            text = "Scan Nutrition Label",
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.textPrimary
        )
        Text(
            text = "Use your camera or upload an image to interpret nutrition facts instantly.",
            fontFamily = interFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Card (Take up more space)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .clickable {
                        val permission = android.Manifest.permission.CAMERA
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = AppColors.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Take Photo",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.primary
                        )
                    }
                }
            }

            // Gallery Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp)
                    .clickable { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppColors.accent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = AppColors.accent
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Upload Image",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.accent
                        )
                    }
                }
            }
        }

        if (isScanning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.primarySurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing nutrition info...",
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.primary
                    )
                }
            }
        }

        scanError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.error.copy(alpha = 0.1f))
            ) {
                Row(
                   modifier = Modifier.padding(16.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = AppColors.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        fontFamily = interFamily,
                        color = AppColors.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        scanResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))
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
    val autoScale by viewModel.autoScale.collectAsState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AppColors.primary,
        unfocusedBorderColor = AppColors.textPrimary
    )

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { viewModel.setEditName(it) },
                    label = { Text("Name", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = editQuantity,
                    onValueChange = { viewModel.setEditQuantity(it) },
                    label = { Text("Quantity (e.g. 1.5 servings)", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { viewModel.adjustQuantity(false) }) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                            }
                            IconButton(onClick = { viewModel.adjustQuantity(true) }) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase")
                            }
                        }
                    }
                )
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoScale, onCheckedChange = { viewModel.setAutoScale(it) })
                    Text("Auto-scale calories & macros", fontFamily = interFamily, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = editCalories,
                    onValueChange = { viewModel.setEditCalories(it) },
                    label = { Text("Calories", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editProtein,
                        onValueChange = { viewModel.setEditProtein(it) },
                        label = { Text("Protein (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = editCarbs,
                        onValueChange = { viewModel.setEditCarbs(it) },
                        label = { Text("Carbs (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors
                    )
                    OutlinedTextField(
                        value = editFat,
                        onValueChange = { viewModel.setEditFat(it) },
                        label = { Text("Fat (g)", fontFamily = interFamily) },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveEdit() }) {
                Text("Save", fontFamily = interFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelEdit() }) {
                Text("Cancel", fontFamily = interFamily)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllFoodsDialog(
    foods: List<FoodItem>,
    onDismiss: () -> Unit,
    onSelect: (FoodItem) -> Unit,
    onRemove: (FoodItem) -> Unit
) {
    var jsonToShow by remember { mutableStateOf<String?>(null) }
    var foodForOptions by remember { mutableStateOf<FoodItem?>(null) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Saved Foods History",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (foods.isEmpty()) {
                    Text(
                        text = "No saved foods found.",
                        fontFamily = interFamily,
                        color = AppColors.textSecondary
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(foods.size) { index ->
                            val food = foods[index]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onSelect(food) },
                                        onLongClick = { foodForOptions = food }
                                    )
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = food.name,
                                    fontFamily = interFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = AppColors.textPrimary
                                )
                                Text(
                                    text = "${food.calories} kcal • ${food.protein}g Protein • ${food.carbs}g Carbs • ${food.fat}g Fat",
                                    fontFamily = interFamily,
                                    fontSize = 14.sp,
                                    color = AppColors.textSecondary
                                )
                            }
                            if (index < foods.size - 1) {
                                HorizontalDivider(color = AppColors.divider)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primary,
                        contentColor = AppColors.textOnPrimary
                    )
                ) {
                    Text("Close", fontFamily = interFamily)
                }
            }
        }
    }

    if (jsonToShow != null) {
        AlertDialog(
            onDismissRequest = { jsonToShow = null },
            confirmButton = {
                TextButton(onClick = { jsonToShow = null }) {
                    Text("Close", fontFamily = interFamily)
                }
            },
            title = {
                Text(
                    text = "Raw Item Data",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = jsonToShow!!,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AppColors.textPrimary
                    )
                }
            }
        )
    }

    if (foodForOptions != null) {
        AlertDialog(
            onDismissRequest = { foodForOptions = null },
            title = { Text(foodForOptions!!.name, fontFamily = interFamily, fontWeight = FontWeight.Bold) },
            text = { Text("What would you like to do with this item?", fontFamily = interFamily) },
            confirmButton = {
                TextButton(onClick = {
                    val prettyJson = Json { prettyPrint = true }
                    jsonToShow = prettyJson.encodeToString(foodForOptions!!)
                    foodForOptions = null
                }) {
                    Text("View Entity", fontFamily = interFamily)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onRemove(foodForOptions!!)
                        foodForOptions = null
                    }) {
                        Text("Remove Entity", fontFamily = interFamily, color = AppColors.error)
                    }
                    TextButton(onClick = { foodForOptions = null }) {
                        Text("Cancel", fontFamily = interFamily)
                    }
                }
            }
        )
    }
}
