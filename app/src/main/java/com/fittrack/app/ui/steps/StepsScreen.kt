package com.fittrack.app.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.util.fmtNum
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = viewModel()
) {
    val context = LocalContext.current
    val steps by viewModel.steps.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val stepSource by viewModel.stepSource.collectAsState()
    val stepsHistory by viewModel.stepsHistory.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showGoalDialog by viewModel.showGoalDialog.collectAsState()
    val addStepsText by viewModel.addStepsText.collectAsState()
    val addMode by viewModel.addMode.collectAsState()
    val goalText by viewModel.goalText.collectAsState()

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.loadData() }

    val healthConnectContract = PermissionController.createRequestPermissionResultContract()
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        healthConnectContract
    ) { viewModel.loadData() }

    LaunchedEffect(Unit) {
        val hasActivityRecognition = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasActivityRecognition) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else if (viewModel.healthConnectService.isAvailable(context)) {
            healthConnectPermissionLauncher.launch(viewModel.healthConnectService.requiredPermissions)
        } else {
            viewModel.loadData()
        }
    }

    val sourceLabel = when (stepSource) {
        "health_connect" -> "Health Connect"
        "pedometer" -> "Pedometer"
        else -> "Manual"
    }

    val distance = steps * 0.000762f
    val activeMinutes = steps / 100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Steps",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = AppColors.textPrimary
            )
            FilterChip(
                selected = true,
                onClick = { },
                label = {
                    Text(
                        text = sourceLabel,
                        fontFamily = interFamily,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.steps.copy(alpha = 0.2f),
                    selectedLabelColor = AppColors.steps
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Main step card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = fmtNum(steps),
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = AppColors.textPrimary
                )
                Text(
                    text = "/ ${fmtNum(stepGoal)} steps",
                    fontFamily = interFamily,
                    fontSize = 16.sp,
                    color = AppColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AppColors.steps,
                    trackColor = AppColors.border,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsWalk,
                value = String.format("%.2f", distance),
                label = "Distance (km)"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                value = fmtNum(caloriesBurned),
                label = "Calories Burned"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                value = activeMinutes.toString(),
                label = "Active Minutes"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Add Steps button (manual, pedometer, or to override health connect)
        if (stepSource == "manual" || stepSource == "pedometer" || stepSource == "health_connect") {
            Button(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.steps)
            ) {
                Text(
                    text = "Add Steps",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Calorie Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calorie Balance",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Consumed",
                            fontFamily = interFamily,
                            fontSize = 12.sp,
                            color = AppColors.textSecondary
                        )
                        Text(
                            text = fmtNum(caloriesConsumed),
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.calorie
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Burned",
                            fontFamily = interFamily,
                            fontSize = 12.sp,
                            color = AppColors.textSecondary
                        )
                        Text(
                            text = fmtNum(caloriesBurned),
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.success
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Net",
                            fontFamily = interFamily,
                            fontSize = 12.sp,
                            color = AppColors.textSecondary
                        )
                        val net = caloriesConsumed - caloriesBurned
                        Text(
                            text = fmtNum(net),
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (net > 0) AppColors.calorie else AppColors.success
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val total = (caloriesConsumed + caloriesBurned).coerceAtLeast(1)
                val consumedRatio = (caloriesConsumed.toFloat() / total).coerceIn(0.01f, 0.99f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(consumedRatio)
                            .background(AppColors.calorie, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - consumedRatio)
                            .background(AppColors.success, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Step History card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Step History",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                val reversedHistory = stepsHistory.reversed()
                val dayNames = reversedHistory.map { (date, _) ->
                    LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                val stepValues = reversedHistory.map { it.second }
                if (stepValues.isNotEmpty()) {
                    StepsHistoryChart(
                        stepValues = stepValues,
                        dayNames = dayNames
                    )
                } else {
                    Text(
                        text = "No step data yet",
                        fontFamily = interFamily,
                        fontSize = 14.sp,
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }

    // Add Steps dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAddDialog() },
            title = {
                Text(
                    text = "Add Steps",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = addStepsText,
                        onValueChange = { viewModel.updateAddStepsText(it) },
                        label = { Text("Steps", fontFamily = interFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = addMode == "add",
                            onClick = { viewModel.setAddMode("add") }
                        )
                        Text(
                            text = "Add to total",
                            fontFamily = interFamily,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        RadioButton(
                            selected = addMode == "set",
                            onClick = { viewModel.setAddMode("set") }
                        )
                        Text(
                            text = "Set total",
                            fontFamily = interFamily
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addSteps() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.steps)
                ) {
                    Text("Add", fontFamily = interFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAddDialog() }) {
                    Text("Cancel", fontFamily = interFamily, color = AppColors.textSecondary)
                }
            }
        )
    }

}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.steps,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.textPrimary
            )
            Text(
                text = label,
                fontFamily = interFamily,
                fontSize = 10.sp,
                color = AppColors.textSecondary
            )
        }
    }
}

@Composable
private fun StepsHistoryChart(
    stepValues: List<Int>,
    dayNames: List<String>
) {
    val barColor = AppColors.steps
    val textColor = AppColors.textSecondary
    val maxSteps = (stepValues.maxOrNull() ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val barCount = stepValues.size
        if (barCount == 0) return@Canvas

        val bottomPadding = 28f
        val topPadding = 20f
        val chartHeight = size.height - bottomPadding - topPadding
        val barWidth = (size.width / barCount) * 0.6f
        val gap = (size.width / barCount) * 0.4f

        stepValues.forEachIndexed { index, steps ->
            val barHeight = (steps.toFloat() / maxSteps) * chartHeight
            val x = index * (barWidth + gap) + gap / 2
            val y = topPadding + chartHeight - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            val label = dayNames.getOrElse(index) { "" }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barWidth / 2,
                size.height - 4f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 24f
                }
            )

            if (steps > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    if (steps >= 1000) "${steps / 1000}k" else steps.toString(),
                    x + barWidth / 2,
                    y - 4f,
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 20f
                    }
                )
            }
        }
    }
}
