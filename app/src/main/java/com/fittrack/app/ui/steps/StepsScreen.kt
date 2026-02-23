package com.fittrack.app.ui.steps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold
import com.fittrack.app.util.fmtNum
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun StepsScreen(viewModel: StepsViewModel = viewModel()) {
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
        val needsHealthConnectPermissions by
                viewModel.needsHealthConnectPermissions.collectAsState()

        val healthConnectContract = PermissionController.createRequestPermissionResultContract()
        val healthConnectPermissionLauncher =
                rememberLauncherForActivityResult(healthConnectContract) { viewModel.loadData() }

        LaunchedEffect(Unit) { viewModel.loadData() }

        LaunchedEffect(needsHealthConnectPermissions) {
                if (needsHealthConnectPermissions) {
                        healthConnectPermissionLauncher.launch(
                                viewModel.healthConnectService.requiredPermissions
                        )
                }
        }

        val sourceLabel =
                when (stepSource) {
                        "health_connect" -> "Health Connect"
                        "pedometer" -> "Pedometer"
                        else -> "Manual"
                }

        val distance = steps * 0.000762f
        val activeMinutes = steps / 100

        val cardCount = 5
        val cardVisible = remember { List(cardCount) { mutableStateOf(false) } }
        LaunchedEffect(Unit) {
                for (i in 0 until cardCount) {
                        delay(80L * i)
                        cardVisible[i].value = true
                }
        }

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

                AnimatedVisibility(
                        visible = cardVisible[0].value,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
                ) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(containerColor = AppColors.surface),
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
                                        val progress =
                                                (steps.toFloat() / stepGoal.coerceAtLeast(1))
                                                        .coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                                progress = { progress },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp)),
                                                color = AppColors.primary,
                                                trackColor = AppColors.border,
                                                drawStopIndicator = {}
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                }
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
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

                if (stepSource == "manual" ||
                                stepSource == "pedometer" ||
                                stepSource == "health_connect"
                ) {
                        Button(
                                onClick = { viewModel.showAddDialog() },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.steps
                                        )
                        ) {
                                Text(
                                        text = "Add Steps",
                                        fontFamily = interFamily,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step History card
                AnimatedVisibility(
                        visible = cardVisible[2].value,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
                ) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(containerColor = AppColors.surface),
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
                                        val dayNames =
                                                reversedHistory.map { (date, _) ->
                                                        LocalDate.parse(date)
                                                                .dayOfWeek
                                                                .getDisplayName(
                                                                        TextStyle.SHORT,
                                                                        Locale.getDefault()
                                                                )
                                                }
                                        val stepValues = reversedHistory.map { it.second }
                                        if (stepValues.isNotEmpty()) {
                                                HistoryChart(
                                                        values = stepValues,
                                                        dayNames = dayNames,
                                                        barColor = AppColors.steps,
                                                        formatValue = {
                                                                if (it >= 1000) "${it / 1000}k"
                                                                else it.toString()
                                                        }
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
                                                onValueChange = {
                                                        viewModel.updateAddStepsText(it)
                                                },
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
                                                Text(text = "Set total", fontFamily = interFamily)
                                        }
                                }
                        },
                        confirmButton = {
                                Button(
                                        onClick = { viewModel.addSteps() },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = AppColors.steps
                                                )
                                ) { Text("Add", fontFamily = interFamily) }
                        },
                        dismissButton = {
                                TextButton(onClick = { viewModel.dismissAddDialog() }) {
                                        Text(
                                                "Cancel",
                                                fontFamily = interFamily,
                                                color = AppColors.textSecondary
                                        )
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
                        modifier =
                                Modifier.fillMaxWidth()
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
        val textSecondaryArgb =
                android.graphics.Color.argb(
                        (AppColors.textSecondary.alpha * 255).toInt(),
                        (AppColors.textSecondary.red * 255).toInt(),
                        (AppColors.textSecondary.green * 255).toInt(),
                        (AppColors.textSecondary.blue * 255).toInt()
                )
        val lineArgb =
                android.graphics.Color.argb(
                        (barColor.alpha * 255).toInt(),
                        (barColor.red * 255).toInt(),
                        (barColor.green * 255).toInt(),
                        (barColor.blue * 255).toInt()
                )
        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)

        val revealProgress = remember { Animatable(0f) }
        LaunchedEffect(values) {
                revealProgress.snapTo(0f)
                revealProgress.animateTo(
                        1f,
                        animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val count = values.size
                if (count == 0) return@Canvas

                val bottomPadding = 28f
                val topPadding = 16f
                val chartHeight = size.height - bottomPadding - topPadding
                val slotWidth = size.width / count

                val points =
                        values.mapIndexed { i, v ->
                                val x = slotWidth * i + slotWidth / 2f
                                val y =
                                        topPadding + chartHeight -
                                                (v.toFloat() / maxValue) * chartHeight
                                x to y
                        }

                val linePaint =
                        android.graphics.Paint().apply {
                                color = lineArgb
                                strokeWidth = 8f
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                val dotPaint =
                        android.graphics.Paint().apply {
                                color = lineArgb
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                        }
                val labelPaint =
                        android.graphics.Paint().apply {
                                color = textSecondaryArgb
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 32f
                                isAntiAlias = true
                                typeface =
                                        android.graphics.Typeface.create(
                                                android.graphics.Typeface.DEFAULT,
                                                android.graphics.Typeface.BOLD
                                        )
                        }
                val valuePaint =
                        android.graphics.Paint().apply {
                                color = textSecondaryArgb
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 24f
                                isAntiAlias = true
                        }

                val t = revealProgress.value
                val totalPoints = points.size.toFloat()

                for (i in 0 until points.size - 1) {
                        val segProgress = ((t * totalPoints) - i).coerceIn(0f, 1f)
                        if (segProgress <= 0f) continue
                        val (x1, y1) = points[i]
                        val (x2, y2) = points[i + 1]
                        val ex = x1 + (x2 - x1) * segProgress
                        val ey = y1 + (y2 - y1) * segProgress
                        linePaint.alpha = (255 * segProgress.coerceAtMost(1f)).toInt()
                        drawContext.canvas.nativeCanvas.drawLine(x1, y1, ex, ey, linePaint)
                }

                points.forEachIndexed { i, (x, y) ->
                        val pointProgress = ((t * totalPoints) - i).coerceIn(0f, 1f)
                        if (pointProgress <= 0f) return@forEachIndexed

                        val scale =
                                if (pointProgress < 0.5f) {
                                        pointProgress * 2f * 1.3f
                                } else {
                                        1.3f - (pointProgress - 0.5f) * 2f * 0.3f
                                }

                        val dotRadius = 8f * scale
                        dotPaint.alpha = (255 * pointProgress).toInt()
                        drawContext.canvas.nativeCanvas.drawCircle(x, y, dotRadius, dotPaint)
                        drawContext.canvas.nativeCanvas.drawCircle(
                                x,
                                y,
                                dotRadius * 0.5f,
                                android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        isAntiAlias = true
                                        style = android.graphics.Paint.Style.FILL
                                }
                        )

                        labelPaint.alpha = (255 * pointProgress).toInt()
                        val label = dayNames.getOrElse(i) { "" }
                        drawContext.canvas.nativeCanvas.drawText(
                                label,
                                x,
                                size.height - 4f,
                                labelPaint
                        )

                        if (values[i] > 0 && pointProgress > 0.3f) {
                                valuePaint.alpha =
                                        (255 * ((pointProgress - 0.3f) / 0.7f).coerceIn(0f, 1f))
                                                .toInt()
                                drawContext.canvas.nativeCanvas.drawText(
                                        formatValue(values[i]),
                                        x,
                                        y - 10f,
                                        valuePaint
                                )
                        }
                }
        }
}
