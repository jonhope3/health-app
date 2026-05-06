package com.fittrack.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.ThemeMode
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val calorieGoal by viewModel.calorieGoal.collectAsStateWithLifecycle()
    val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
    val weightLbs by viewModel.weightLbs.collectAsStateWithLifecycle()
    val heightFt by viewModel.heightFt.collectAsStateWithLifecycle()
    val heightIn by viewModel.heightIn.collectAsStateWithLifecycle()
    val age by viewModel.age.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val healthConnectGranted by viewModel.healthConnectGranted.collectAsStateWithLifecycle()
    val proteinGoal by viewModel.proteinGoal.collectAsStateWithLifecycle()
    val carbsGoal by viewModel.carbsGoal.collectAsStateWithLifecycle()
    val fatGoal by viewModel.fatGoal.collectAsStateWithLifecycle()
    val sugarGoal by viewModel.sugarGoal.collectAsStateWithLifecycle()
    val isGeneratingMacros by viewModel.isGeneratingMacros.collectAsStateWithLifecycle()
    val macroGenerateError by viewModel.macroGenerateError.collectAsStateWithLifecycle()
    val geminiReady by viewModel.geminiReady.collectAsStateWithLifecycle()
    val geminiStatus by viewModel.geminiStatus.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var editingGoals by remember { mutableStateOf(false) }
    var editingBody by remember { mutableStateOf(false) }
    var editingMacros by remember { mutableStateOf(false) }
    var editingNickname by remember { mutableStateOf(false) }
    var showNukeConfirm by remember { mutableStateOf(false) }
    var showDbViewer by remember { mutableStateOf(false) }
    val dbEntries by viewModel.dbEntries.collectAsStateWithLifecycle()
    var selectedEntry by remember { mutableStateOf<DbEntry?>(null) }

    val hcAvailability = HealthConnectClient.getSdkStatus(context)
    val hcInstalled = hcAvailability == HealthConnectClient.SDK_AVAILABLE

    val permissionLauncher =
            rememberLauncherForActivityResult(
                    contract = PermissionController.createRequestPermissionResultContract()
            ) { granted ->
                Log.d("FitTrack_HC", "Permission result: granted=$granted")
                viewModel.onHealthConnectResult(granted)
            }

    ScreenScaffold {
        Text(
                text = "Settings",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ── Appearance ──
        SectionHeader("Appearance")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = "Color Scheme",
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                // 3-way segmented control: Light | Dark | System
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                                shape =
                                        SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = ThemeMode.entries.size,
                                        ),
                                onClick = { viewModel.setThemeMode(mode) },
                                selected = themeMode == mode,
                                label = {
                                    Text(
                                            text =
                                                    mode.name.lowercase().replaceFirstChar {
                                                        it.uppercase()
                                                    },
                                            fontFamily = interFamily,
                                            fontSize = 13.sp,
                                    )
                                },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ── Profile / Nickname ──
        SectionHeader("Profile")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (editingNickname) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                            value = nickname,
                            onValueChange = { viewModel.setNickname(it) },
                            label = { Text("Nickname", fontFamily = interFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                                onClick = {
                                    viewModel.save()
                                    editingNickname = false
                                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary
                                        )
                        ) { Text("Save", fontFamily = interFamily) }
                        TextButton(
                                onClick = {
                                    viewModel.loadData()
                                    editingNickname = false
                                }
                        ) {
                            Text(
                                    "Cancel",
                                    fontFamily = interFamily,
                                    color = AppColors.textSecondary
                            )
                        }
                    }
                }
            } else {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                                "Nickname",
                                fontFamily = interFamily,
                                fontSize = 12.sp,
                                color = AppColors.textSecondary
                        )
                        Text(
                                nickname,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = AppColors.textPrimary
                        )
                    }
                    IconButton(onClick = { editingNickname = true }) {
                        Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = AppColors.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Data Sources ──
        SectionHeader("Data Sources")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .combinedClickable(
                                            interactionSource =
                                                    remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {},
                                            onLongClick = {
                                                val service =
                                                        com.fittrack.app.services
                                                                .HealthConnectService()
                                                val intents = service.getSettingsIntents(context)
                                                for (intent in intents) {
                                                    try {
                                                        context.startActivity(intent)
                                                        break
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                    )
                                    .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Filled.MonitorHeart,
                            contentDescription = null,
                            tint =
                                    if (healthConnectGranted) AppColors.success
                                    else AppColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                                "Health Connect",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = AppColors.textPrimary
                        )
                        Text(
                                text =
                                        if (healthConnectGranted)
                                                "Connected — syncing steps & calories"
                                        else "Not connected",
                                fontFamily = interFamily,
                                fontSize = 12.sp,
                                color =
                                        if (healthConnectGranted) AppColors.success
                                        else AppColors.textSecondary
                        )
                    }
                }
                if (!healthConnectGranted) {
                    Button(
                            onClick = {
                                Log.d(
                                        "FitTrack_HC",
                                        "Connect button tapped. hcInstalled=$hcInstalled"
                                )
                                if (hcInstalled) {
                                    val permissions =
                                            setOf(
                                                    HealthPermission.getReadPermission(
                                                            StepsRecord::class
                                                    ),
                                                    HealthPermission.getReadPermission(
                                                            ActiveCaloriesBurnedRecord::class
                                                    )
                                            )
                                    try {
                                        permissionLauncher.launch(permissions)
                                    } catch (e: Exception) {
                                        Log.e(
                                                "FitTrack_HC",
                                                "permissionLauncher.launch threw: ${e.message}",
                                                e
                                        )
                                    }
                                } else {
                                    val intent =
                                            Intent(Intent.ACTION_VIEW).apply {
                                                data =
                                                        Uri.parse(
                                                                "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                                                        )
                                                setPackage("com.android.vending")
                                            }
                                    runCatching { context.startActivity(intent) }.onFailure {
                                        Toast.makeText(
                                                        context,
                                                        "Health Connect is not available on this device",
                                                        Toast.LENGTH_LONG
                                                )
                                                .show()
                                    }
                                }
                            },
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = AppColors.primary
                                    ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                                if (hcInstalled) "Connect" else "Install",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = AppColors.textOnPrimary
                        )
                    }
                } else {
                    Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Connected",
                            tint = AppColors.success,
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Daily Goals (collapsed / edit) ──
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Daily Goals")
            if (!editingGoals) {
                IconButton(onClick = { editingGoals = true }) {
                    Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (editingGoals) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                            value = calorieGoal,
                            onValueChange = { viewModel.setCalorieGoal(it) },
                            label = { Text("Calorie Goal", fontFamily = interFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text("Recommended: 1,500 – 3,000", fontFamily = interFamily)
                            }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                            value = stepGoal,
                            onValueChange = { viewModel.setStepGoal(it) },
                            label = { Text("Step Goal", fontFamily = interFamily) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = {
                                Text("Recommended: 8,000 – 12,000", fontFamily = interFamily)
                            }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                                onClick = {
                                    viewModel.save()
                                    editingGoals = false
                                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary
                                        )
                        ) { Text("Save", fontFamily = interFamily) }
                        TextButton(
                                onClick = {
                                    viewModel.loadData()
                                    editingGoals = false
                                }
                        ) {
                            Text(
                                    "Cancel",
                                    fontFamily = interFamily,
                                    color = AppColors.textSecondary
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRow("Calorie Goal", "$calorieGoal cal")
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow("Step Goal", "$stepGoal steps")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Body Measurements (collapsed / edit) ──
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionHeader("Body Measurements")
                Text(
                        "Used to estimate calories burned from steps",
                        fontFamily = interFamily,
                        fontSize = 13.sp,
                        color = AppColors.textSecondary
                )
            }
            if (!editingBody) {
                IconButton(onClick = { editingBody = true }) {
                    Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (editingBody) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                            value = weightLbs,
                            onValueChange = { viewModel.setWeightLbs(it) },
                            label = { Text("Weight", fontFamily = interFamily) },
                            suffix = {
                                Text(
                                        "lbs",
                                        fontFamily = interFamily,
                                        color = AppColors.textSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                            "Height",
                            fontFamily = interFamily,
                            fontSize = 14.sp,
                            color = AppColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                                value = heightFt,
                                onValueChange = { viewModel.setHeightFt(it) },
                                label = { Text("Feet", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "ft",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                                value = heightIn,
                                onValueChange = { viewModel.setHeightIn(it) },
                                label = { Text("Inches", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "in",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                            value = age,
                            onValueChange = { viewModel.setAge(it) },
                            label = { Text("Age", fontFamily = interFamily) },
                            suffix = {
                                Text(
                                        "yrs",
                                        fontFamily = interFamily,
                                        color = AppColors.textSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                                onClick = {
                                    viewModel.save()
                                    editingBody = false
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary
                                        )
                        ) { Text("Save", fontFamily = interFamily) }
                        TextButton(
                                onClick = {
                                    viewModel.loadData()
                                    editingBody = false
                                }
                        ) {
                            Text(
                                    "Cancel",
                                    fontFamily = interFamily,
                                    color = AppColors.textSecondary
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsRow("Weight", "$weightLbs lbs")
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow("Height", "${heightFt}' ${heightIn}\"")
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsRow("Age", "$age yrs")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Daily Macro Goals ──
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionHeader("Macro Goals")
                Text(
                        "Daily targets in grams",
                        fontFamily = interFamily,
                        fontSize = 13.sp,
                        color = AppColors.textSecondary
                )
            }
            if (!editingMacros) {
                IconButton(onClick = { editingMacros = true }) {
                    Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-Estimate: always available, uses nutritional formulas
                Button(
                        onClick = { viewModel.generateLocalMacroGoals() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeneratingMacros,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary)
                ) {
                    if (isGeneratingMacros) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AppColors.textOnPrimary,
                                strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Estimating…", fontFamily = interFamily)
                    } else {
                        Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                                "Auto-Estimate Goals",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                        "Based on your calorie goal & body measurements",
                        fontFamily = interFamily,
                        fontSize = 11.sp,
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
                // Gemini upgrade — only shown when on-device AI is ready
                if (geminiReady) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                            onClick = { viewModel.generateAiMacroGoals() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGeneratingMacros
                    ) {
                        Text(
                                "✨ Refine with AI",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                        )
                    }
                }
                // Gemini Nano status chip — always shown for diagnostics
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val (statusColor, statusLabel) = when (geminiStatus) {
                        "available" -> AppColors.success to "AI: ready"
                        "downloadable" -> AppColors.warning to "AI: needs download"
                        "downloading" -> AppColors.primary to "AI: downloading…"
                        "checking…" -> AppColors.textSecondary to "AI: checking…"
                        else -> AppColors.textSecondary to "AI: unavailable on this device"
                    }
                    Box(
                            modifier = Modifier
                                    .size(6.dp)
                                    .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                    )
                    Text(
                            statusLabel,
                            fontFamily = interFamily,
                            fontSize = 10.sp,
                            color = statusColor
                    )
                }
                macroGenerateError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                            it,
                            fontFamily = interFamily,
                            fontSize = 12.sp,
                            color = AppColors.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (editingMacros) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                                value = proteinGoal,
                                onValueChange = { viewModel.setProteinGoal(it) },
                                label = { Text("Protein", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "g",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                                value = carbsGoal,
                                onValueChange = { viewModel.setCarbsGoal(it) },
                                label = { Text("Carbs", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "g",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                                value = fatGoal,
                                onValueChange = { viewModel.setFatGoal(it) },
                                label = { Text("Fat", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "g",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                                value = sugarGoal,
                                onValueChange = { viewModel.setSugarGoal(it) },
                                label = { Text("Sugar", fontFamily = interFamily) },
                                suffix = {
                                    Text(
                                            "g",
                                            fontFamily = interFamily,
                                            color = AppColors.textSecondary
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                                onClick = {
                                    viewModel.saveMacroGoals()
                                    editingMacros = false
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.primary
                                        )
                        ) { Text("Save", fontFamily = interFamily) }
                        TextButton(
                                onClick = {
                                    viewModel.loadData()
                                    editingMacros = false
                                }
                        ) {
                            Text(
                                    "Cancel",
                                    fontFamily = interFamily,
                                    color = AppColors.textSecondary
                            )
                        }
                    }
                } else {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroGoalChip(
                                "Protein",
                                proteinGoal,
                                AppColors.protein,
                                modifier = Modifier.weight(1f)
                        )
                        MacroGoalChip(
                                "Carbs",
                                carbsGoal,
                                AppColors.carbs,
                                modifier = Modifier.weight(1f)
                        )
                        MacroGoalChip("Fat", fatGoal, AppColors.fat, modifier = Modifier.weight(1f))
                        MacroGoalChip(
                                "Sugar",
                                sugarGoal,
                                AppColors.sugar,
                                modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Reset & Start Fresh ──
        SectionHeader("Reset")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        "Clear all data and start over. This removes food logs, step history, goals, and settings.",
                        fontFamily = interFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                        onClick = { showNukeConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = AppColors.error,
                                        contentColor = AppColors.textOnPrimary
                                )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            "Reset & Start Fresh",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Developer Settings ──
        SectionHeader("Developer Settings")
        Spacer(modifier = Modifier.height(8.dp))
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                        onClick = {
                            viewModel.loadDbOverview()
                            showDbViewer = true
                        },
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View DB", fontFamily = interFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── Dialogs ──

    if (showNukeConfirm) {
        AlertDialog(
                onDismissRequest = { showNukeConfirm = false },
                title = {
                    Text(
                            "Reset Everything?",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                            "This will permanently delete all food logs, step history, custom foods, goals, and settings. The app will restart fresh with onboarding. This cannot be undone.",
                            fontFamily = interFamily
                    )
                },
                confirmButton = {
                    Button(
                            onClick = {
                                viewModel.nukeDb()
                                showNukeConfirm = false
                                Toast.makeText(
                                                context,
                                                "All data cleared — restart the app",
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.error)
                    ) { Text("Reset", fontFamily = interFamily) }
                },
                dismissButton = {
                    TextButton(onClick = { showNukeConfirm = false }) {
                        Text("Cancel", fontFamily = interFamily)
                    }
                }
        )
    }

    if (showDbViewer) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDbViewer = false }) {
            Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                            "Database Explorer",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AppColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val grouped = dbEntries.groupBy { it.table }
                        grouped.forEach { (table, rows) ->

                            // Table header chip
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                            color = AppColors.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                                text = table.replace("_", " ").uppercase(),
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 10.dp,
                                                                vertical = 4.dp
                                                        ),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AppColors.primary,
                                                letterSpacing = 1.sp,
                                                fontFamily = interFamily
                                        )
                                    }
                                    Text(
                                            text =
                                                    "${rows.size} row${if (rows.size != 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = AppColors.textSecondary,
                                            fontFamily = interFamily
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Derive column headers from the first row's keys
                            val columns = rows.firstOrNull()?.fields?.keys?.toList() ?: emptyList()
                            val isEmpty =
                                    columns.size == 1 &&
                                            rows.firstOrNull()?.fields?.containsKey("(empty)") ==
                                                    true

                            if (!isEmpty) {
                                // Column header row
                                item {
                                    Row(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .background(
                                                                    AppColors.surfaceVariant,
                                                                    RoundedCornerShape(
                                                                            topStart = 8.dp,
                                                                            topEnd = 8.dp
                                                                    )
                                                            )
                                                            .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 6.dp
                                                            )
                                    ) {
                                        columns.forEach { col ->
                                            Text(
                                                    text = col.uppercase(),
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppColors.textSecondary,
                                                    letterSpacing = 0.8.sp,
                                                    fontFamily = interFamily,
                                                    maxLines = 1,
                                                    overflow =
                                                            androidx.compose.ui.text.style
                                                                    .TextOverflow.Clip
                                            )
                                        }
                                    }
                                }
                            }

                            // Data rows
                            itemsIndexed(rows) { idx, entry ->
                                if (isEmpty) {
                                    // Empty-state placeholder
                                    Box(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .background(
                                                                    AppColors.surfaceVariant.copy(
                                                                            alpha = 0.5f
                                                                    ),
                                                                    RoundedCornerShape(
                                                                            bottomStart = 8.dp,
                                                                            bottomEnd = 8.dp
                                                                    )
                                                            )
                                                            .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 14.dp
                                                            ),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                                text = entry.fields.values.firstOrNull() ?: "Empty",
                                                fontFamily = interFamily,
                                                fontSize = 13.sp,
                                                color = AppColors.textSecondary
                                        )
                                    }
                                } else {
                                    val isLast = idx == rows.lastIndex
                                    val rowBg =
                                            if (idx % 2 == 0) AppColors.surface
                                            else AppColors.surfaceVariant.copy(alpha = 0.4f)
                                    val bottomShape =
                                            if (isLast)
                                                    RoundedCornerShape(
                                                            bottomStart = 8.dp,
                                                            bottomEnd = 8.dp
                                                    )
                                            else RoundedCornerShape(0.dp)

                                    Row(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .background(rowBg, bottomShape)
                                                            .combinedClickable(
                                                                    onClick = {},
                                                                    onLongClick = {
                                                                        selectedEntry = entry
                                                                    }
                                                            )
                                                            .padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 8.dp
                                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        columns.forEach { col ->
                                            val cellValue = entry.fields[col] ?: ""
                                            Text(
                                                    text = cellValue,
                                                    modifier = Modifier.weight(1f),
                                                    fontFamily = interFamily,
                                                    fontSize = 12.sp,
                                                    color = AppColors.textPrimary,
                                                    maxLines = 1,
                                                    overflow =
                                                            androidx.compose.ui.text.style
                                                                    .TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (!isLast) {
                                        HorizontalDivider(
                                                color = AppColors.border.copy(alpha = 0.3f),
                                                thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showDbViewer = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Close", fontFamily = interFamily)
                    }
                }
            }
        }
    }

    // Long-press detail popup — shows all fields for the selected row
    if (selectedEntry != null) {
        AlertDialog(
                onDismissRequest = { selectedEntry = null },
                title = {
                    Text(
                            text =
                                    selectedEntry!!.table.replace("_", " ").replaceFirstChar {
                                        it.uppercase()
                                    },
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedEntry!!.fields.forEach { (col, value) ->
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                        text = col,
                                        fontFamily = interFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.textSecondary,
                                        modifier = Modifier.weight(0.4f)
                                )
                                Text(
                                        text = value,
                                        fontFamily = interFamily,
                                        fontSize = 12.sp,
                                        color = AppColors.textPrimary,
                                        modifier = Modifier.weight(0.6f),
                                        maxLines = 2,
                                        overflow =
                                                androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(
                                    color = AppColors.border.copy(alpha = 0.4f),
                                    thickness = 0.5.dp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEntry = null }) {
                        Text("Done", fontFamily = interFamily)
                    }
                }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
            text = title,
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = AppColors.textPrimary
    )
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = interFamily, fontSize = 14.sp, color = AppColors.textSecondary)
        Text(
                value,
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AppColors.textPrimary
        )
    }
}

@Composable
private fun MacroGoalChip(
        label: String,
        grams: String,
        color: androidx.compose.ui.graphics.Color,
        modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
                modifier =
                        Modifier.background(
                                        color.copy(alpha = 0.15f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
        ) {
            Text(
                    text = "${grams}g",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontFamily = interFamily, fontSize = 11.sp, color = AppColors.textSecondary)
    }
}
