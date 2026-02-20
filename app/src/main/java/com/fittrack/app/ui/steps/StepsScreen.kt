package com.fittrack.app.ui.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
import com.fittrack.app.ui.common.ScreenScaffold
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
    val caloriesHistory by viewModel.caloriesHistory.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showGoalDialog by viewModel.showGoalDialog.collectAsState()
    val addStepsText by viewModel.addStepsText.collectAsState()
    val addMode by viewModel.addMode.collectAsState()
    val goalText by viewModel.goalText.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val sourceLabel = when (stepSource) {
        "health_connect" -> "Health Connect"
        "pedometer" -> "Pedometer"
        else -> "Manual"
    }

    val distance = steps * 0.000762f
    val activeMinutes = steps / 100

    ScreenScaffold {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Steps",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = AppColors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main step card
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
                    text = "of ${fmtNum(stepGoal)} goal",
                    fontFamily = interFamily,
                    fontSize = 14.sp,
                    color = AppColors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                val progress = (steps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppColors.primary,
                    trackColor = AppColors.border,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                value = String.format(java.util.Locale.US, "%.1f", distance),
                label = "Miles"
            )
            StatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.LocalFireDepartment,
                value = fmtNum(caloriesBurned),
                label = "Cal Burned"
            )
            StatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.Default.Timer,
                value = activeMinutes.toString(),
                label = "Active Min"
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
                Spacer(modifier = Modifier.height(8.dp))
                // Calorie balance bar — consumed (orange) vs burned (blue)
                val total = (caloriesConsumed + caloriesBurned).coerceAtLeast(1)
                val consumedRatio = (caloriesConsumed.toFloat() / total).coerceIn(0.05f, 0.95f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(consumedRatio)
                            .fillMaxHeight()
                            .background(AppColors.calorie)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - consumedRatio)
                            .fillMaxHeight()
                            .background(AppColors.success)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("● Consumed", fontFamily = interFamily, fontSize = 10.sp, color = AppColors.calorie)
                    Text("● Burned", fontFamily = interFamily, fontSize = 10.sp, color = AppColors.success)
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
                    HistoryChart(
                        values = stepValues,
                        dayNames = dayNames,
                        barColor = AppColors.steps,
                        formatValue = { if (it >= 1000) "${it / 1000}k" else it.toString() }
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

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Calorie History card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calorie History",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = AppColors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                val revCalHistory = caloriesHistory.reversed()
                val dayNames = revCalHistory.map { (date, _) ->
                    LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                val calorieValues = revCalHistory.map { it.second }
                if (calorieValues.isNotEmpty() && calorieValues.any { it > 0 }) {
                    HistoryChart(
                        values = calorieValues,
                        dayNames = dayNames,
                        barColor = AppColors.calorie,
                        formatValue = { it.toString() }
                    )
                } else {
                    Text(
                        text = "No calorie data yet",
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.steps,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = AppColors.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = label,
                fontFamily = interFamily,
                fontSize = 10.sp,
                color = AppColors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryChart(
    values: List<Int>,
    dayNames: List<String>,
    barColor: androidx.compose.ui.graphics.Color,
    formatValue: (Int) -> String
) {
    val textSecondaryArgb = android.graphics.Color.argb(
        (AppColors.textSecondary.alpha * 255).toInt(),
        (AppColors.textSecondary.red * 255).toInt(),
        (AppColors.textSecondary.green * 255).toInt(),
        (AppColors.textSecondary.blue * 255).toInt()
    )
    val lineArgb = android.graphics.Color.argb(
        (barColor.alpha * 255).toInt(),
        (barColor.red * 255).toInt(),
        (barColor.green * 255).toInt(),
        (barColor.blue * 255).toInt()
    )
    val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val count = values.size
        if (count == 0) return@Canvas

        val bottomPadding = 28f
        val topPadding = 16f
        val chartHeight = size.height - bottomPadding - topPadding
        val slotWidth = size.width / count

        // Compute point positions
        val points = values.mapIndexed { i, v ->
            val x = slotWidth * i + slotWidth / 2f
            val y = topPadding + chartHeight - (v.toFloat() / maxValue) * chartHeight
            x to y
        }

        val linePaint = android.graphics.Paint().apply {
            color = lineArgb
            strokeWidth = 8f
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        val dotPaint = android.graphics.Paint().apply {
            color = lineArgb
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        val labelPaint = android.graphics.Paint().apply {
            color = textSecondaryArgb
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 32f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val valuePaint = android.graphics.Paint().apply {
            color = textSecondaryArgb
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 24f
            isAntiAlias = true
        }

        // Draw connecting lines
        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            drawContext.canvas.nativeCanvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        // Draw dots and labels
        points.forEachIndexed { i, (x, y) ->
            // Dot
            drawContext.canvas.nativeCanvas.drawCircle(x, y, 8f, dotPaint)
            // White center
            drawContext.canvas.nativeCanvas.drawCircle(
                x, y, 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                }
            )
            // Day label
            val label = dayNames.getOrElse(i) { "" }
            drawContext.canvas.nativeCanvas.drawText(label, x, size.height - 4f, labelPaint)
            // Value above dot (only if > 0)
            if (values[i] > 0) {
                drawContext.canvas.nativeCanvas.drawText(formatValue(values[i]), x, y - 10f, valuePaint)
            }
        }
    }
}
