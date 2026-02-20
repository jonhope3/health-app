package com.fittrack.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val weightLbs by viewModel.weightLbs.collectAsState()
    val heightFt by viewModel.heightFt.collectAsState()
    val heightIn by viewModel.heightIn.collectAsState()
    val healthConnectGranted by viewModel.healthConnectGranted.collectAsState()

    var showNukeConfirm by remember { mutableStateOf(false) }
    var showDbViewer by remember { mutableStateOf(false) }
    var dbEntries by remember { mutableStateOf<List<DbEntry>>(emptyList()) }
    var selectedEntry by remember { mutableStateOf<DbEntry?>(null) }

    // Check if Health Connect SDK is even available on this device
    val hcAvailability = HealthConnectClient.getSdkStatus(context)
    val hcInstalled = hcAvailability == HealthConnectClient.SDK_AVAILABLE

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d("FitTrack_HC", "Permission result: granted=$granted")
        viewModel.onHealthConnectResult(granted)
    }

    // Log HC status on every composition so we can verify from logcat
    Log.d("FitTrack_HC", "getSdkStatus=${hcAvailability}, SDK_AVAILABLE=${HealthConnectClient.SDK_AVAILABLE}, hcInstalled=$hcInstalled")

    ScreenScaffold {
        Text(
            text = "Settings",
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = AppColors.textPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Data Sources section
        SectionHeader("Data Sources")
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Primary action handled by buttons or state */ },
                        onLongClick = {
                            val service = com.fittrack.app.services.HealthConnectService()
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
                        tint = if (healthConnectGranted) AppColors.success else AppColors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Health Connect",
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = AppColors.textPrimary
                        )
                        Text(
                            text = if (healthConnectGranted) "Connected — syncing steps & calories" else "Not connected",
                            fontFamily = interFamily,
                            fontSize = 12.sp,
                            color = if (healthConnectGranted) AppColors.success else AppColors.textSecondary
                        )
                    }
                }
                if (!healthConnectGranted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                Log.d("FitTrack_HC", "Connect button tapped. hcInstalled=$hcInstalled, hcAvailability=$hcAvailability")
                                if (hcInstalled) {
                                    val permissions = setOf(
                                        HealthPermission.getReadPermission(StepsRecord::class),
                                        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
                                    )
                                    Log.d("FitTrack_HC", "Launching permission request with strings: $permissions")
                                    try {
                                        permissionLauncher.launch(permissions)
                                    } catch (e: Exception) {
                                        Log.e("FitTrack_HC", "permissionLauncher.launch threw: ${e.message}", e)
                                    }
                                } else {
                                    // Not installed — send to Play Store
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(
                                            "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                                        )
                                        setPackage("com.android.vending")
                                    }
                                    runCatching {
                                        context.startActivity(intent)
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Health Connect is not available on this device",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.primary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hcInstalled) "Connect" else "Install",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = AppColors.textOnPrimary
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                val service = com.fittrack.app.services.HealthConnectService()
                                val intents = service.getSettingsIntents(context)
                                var successfullyStarted = false
                                
                                for (intent in intents) {
                                    try {
                                        Log.d("FitTrack_HC", "Trying to launch settings with action: ${intent.action}")
                                        context.startActivity(intent)
                                        successfullyStarted = true
                                        break
                                    } catch (e: Exception) {
                                        Log.w("FitTrack_HC", "Failed to launch action ${intent.action}: ${e.message}")
                                    }
                                }
                                
                                if (!successfullyStarted) {
                                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Settings", fontFamily = interFamily, fontSize = 13.sp)
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Connected",
                        tint = AppColors.success,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(24.dp))

        // Goals section
        SectionHeader("Daily Goals")
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = calorieGoal,
                    onValueChange = { viewModel.setCalorieGoal(it) },
                    label = { Text("Calorie Goal", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Recommended: 1,500 – 3,000", fontFamily = interFamily) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = stepGoal,
                    onValueChange = { viewModel.setStepGoal(it) },
                    label = { Text("Step Goal", fontFamily = interFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Recommended: 8,000 – 12,000", fontFamily = interFamily) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(24.dp))

        // Body section
        SectionHeader("Body Measurements")
        Text(
            text = "Used to estimate calories burned from steps",
            fontFamily = interFamily,
            fontSize = 13.sp,
            color = AppColors.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = weightLbs,
                    onValueChange = { viewModel.setWeightLbs(it) },
                    label = { Text("Weight", fontFamily = interFamily) },
                    suffix = { Text("lbs", fontFamily = interFamily, color = AppColors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Height",
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
                        suffix = { Text("ft", fontFamily = interFamily, color = AppColors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = heightIn,
                        onValueChange = { viewModel.setHeightIn(it) },
                        label = { Text("Inches", fontFamily = interFamily) },
                        suffix = { Text("in", fontFamily = interFamily, color = AppColors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.save()
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primary,
                contentColor = AppColors.textOnPrimary
            )
        ) {
            Text(
                text = "Save Settings",
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = AppColors.border)
        Spacer(modifier = Modifier.height(24.dp))

        // Developer section
        SectionHeader("Developer Settings")
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showNukeConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nuke DB", fontFamily = interFamily)
                }
                
                OutlinedButton(
                    onClick = { 
                        dbEntries = viewModel.getDbOverview()
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

    if (showNukeConfirm) {
        AlertDialog(
            onDismissRequest = { showNukeConfirm = false },
            title = { Text("Nuke Database?", fontFamily = interFamily, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all food logs, custom foods, and goal settings. This action cannot be undone.", fontFamily = interFamily) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.nukeDb()
                        showNukeConfirm = false
                        Toast.makeText(context, "Database nuked", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.error)
                ) {
                    Text("Nuke It", fontFamily = interFamily)
                }
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
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Database Explorer", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val grouped = dbEntries.groupBy { it.table }
                        grouped.forEach { (table, entries) ->
                            item {
                                Surface(
                                    color = AppColors.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = table.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            items(entries) { entry ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { /* Could toggle expand */ },
                                            onLongClick = { if (entry.key != "(empty)") selectedEntry = entry }
                                        )
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = entry.key,
                                        fontFamily = interFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (entry.key == "(empty)") AppColors.textSecondary else AppColors.textPrimary
                                    )
                                    Text(
                                        text = entry.value,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        color = AppColors.textSecondary
                                    )
                                }
                                HorizontalDivider(color = AppColors.border.copy(alpha = 0.5f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDbViewer = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Close", fontFamily = interFamily)
                    }
                }
            }
        }
    }

    if (selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text("Record Details", fontFamily = interFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Table: ${selectedEntry!!.table}", fontSize = 12.sp, color = AppColors.textSecondary)
                    Text("Key: ${selectedEntry!!.key}", fontSize = 12.sp, color = AppColors.textSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = selectedEntry!!.value,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp
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
