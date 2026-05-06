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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.theme.AppColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold
import com.fittrack.app.util.fmtNum
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun StepsScreen(viewModel: StepsViewModel = hiltViewModel()) {
        val context = LocalContext.current
        val steps by viewModel.steps.collectAsStateWithLifecycle()
        val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
        val stepSource by viewModel.stepSource.collectAsStateWithLifecycle()
        val stepsHistory by viewModel.stepsHistory.collectAsStateWithLifecycle()
        val caloriesHistory by viewModel.caloriesHistory.collectAsStateWithLifecycle()
        val caloriesConsumed by viewModel.caloriesConsumed.collectAsStateWithLifecycle()
        val caloriesBurned by viewModel.caloriesBurned.collectAsStateWithLifecycle()
        val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
        val showGoalDialog by viewModel.showGoalDialog.collectAsStateWithLifecycle()
        val addStepsText by viewModel.addStepsText.collectAsStateWithLifecycle()
        val addMode by viewModel.addMode.collectAsStateWithLifecycle()
        val goalText by viewModel.goalText.collectAsStateWithLifecycle()
        val needsHealthConnectPermissions by
                viewModel.needsHealthConnectPermissions.collectAsStateWithLifecycle()

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
                                label = "Cals from Steps"
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
                                                        goal = stepGoal,
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
                                                        viewModel.updateAddStepsText(
                                                                it.filter { c -> c.isDigit() }
                                                        )
                                                },
                                                label = { Text("Steps", fontFamily = interFamily) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                keyboardOptions =
                                                        androidx.compose.foundation.text
                                                                .KeyboardOptions(
                                                                        keyboardType =
                                                                                androidx.compose.ui
                                                                                        .text.input
                                                                                        .KeyboardType
                                                                                        .Number
                                                                )
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
        goal: Int,
        formatValue: (Int) -> String
) {
        val textSecondaryArgb =
                android.graphics.Color.argb(
                        (AppColors.textSecondary.alpha * 255).toInt(),
                        (AppColors.textSecondary.red * 255).toInt(),
                        (AppColors.textSecondary.green * 255).toInt(),
                        (AppColors.textSecondary.blue * 255).toInt()
                )

        // Color ramp: red → orange → green based on % of goal
        fun valueColor(v: Int): Int {
                val pct = if (goal > 0) v.toFloat() / goal else 0f
                return when {
                        pct >= 1.0f -> android.graphics.Color.parseColor("#4CAF50") // green
                        pct >= 0.75f -> {
                                // lerp orange→green over 0.75..1.0
                                val t = (pct - 0.75f) / 0.25f
                                lerpArgb(
                                        android.graphics.Color.parseColor("#FF9800"),
                                        android.graphics.Color.parseColor("#4CAF50"),
                                        t
                                )
                        }
                        pct >= 0.4f -> {
                                // lerp red→orange over 0.4..0.75
                                val t = (pct - 0.4f) / 0.35f
                                lerpArgb(
                                        android.graphics.Color.parseColor("#F44336"),
                                        android.graphics.Color.parseColor("#FF9800"),
                                        t
                                )
                        }
                        else -> android.graphics.Color.parseColor("#F44336") // red
                }
        }

        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(goal).coerceAtLeast(1)

        val revealProgress = remember { Animatable(0f) }
        LaunchedEffect(values) {
                revealProgress.snapTo(0f)
                revealProgress.animateTo(
                        1f,
                        animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val count = values.size
                if (count == 0) return@Canvas

                val bottomPadding = 32f
                val topPadding = 20f
                val chartHeight = size.height - bottomPadding - topPadding
                val slotWidth = size.width / count

                val points = values.mapIndexed { i, v ->
                        val x = slotWidth * i + slotWidth / 2f
                        val y = topPadding + chartHeight - (v.toFloat() / maxValue) * chartHeight
                        x to y
                }

                val t = revealProgress.value
                val totalPoints = points.size.toFloat()

                // ── Goal reference line ────────────────────────────────────────
                if (goal > 0) {
                        val goalY = topPadding + chartHeight - (goal.toFloat() / maxValue) * chartHeight
                        val goalPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(80, 255, 255, 255)
                                strokeWidth = 2f
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
                        }
                        drawContext.canvas.nativeCanvas.drawLine(
                                0f, goalY, size.width * t, goalY, goalPaint
                        )
                }

                // ── Gradient fill area ─────────────────────────────────────────
                if (points.size >= 2) {
                        val fillPath = android.graphics.Path()
                        val firstVisible = (t * totalPoints).toInt().coerceIn(0, points.size - 1)
                        fillPath.moveTo(points[0].first, size.height - bottomPadding)
                        for (i in 0..firstVisible) {
                                val segProgress = ((t * totalPoints) - i).coerceIn(0f, 1f)
                                val (x, y) = points[i]
                                val nextPt = points.getOrNull(i + 1)
                                val drawX = if (nextPt != null && segProgress < 1f) {
                                        x + (nextPt.first - x) * segProgress
                                } else x
                                val drawY = if (nextPt != null && segProgress < 1f) {
                                        y + (nextPt.second - y) * segProgress
                                } else y
                                fillPath.lineTo(drawX, drawY)
                        }
                        fillPath.lineTo(
                                points[firstVisible.coerceAtMost(points.size - 1)].first,
                                size.height - bottomPadding
                        )
                        fillPath.close()

                        val fillPaint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                shader = android.graphics.LinearGradient(
                                        0f, topPadding,
                                        0f, size.height - bottomPadding,
                                        intArrayOf(
                                                android.graphics.Color.argb(60, 255, 255, 255),
                                                android.graphics.Color.TRANSPARENT
                                        ),
                                        null,
                                        android.graphics.Shader.TileMode.CLAMP
                                )
                                alpha = (180 * t).toInt()
                        }
                        drawContext.canvas.nativeCanvas.drawPath(fillPath, fillPaint)
                }

                // ── Gradient line segments ─────────────────────────────────────
                for (i in 0 until points.size - 1) {
                        val segProgress = ((t * totalPoints) - i).coerceIn(0f, 1f)
                        if (segProgress <= 0f) continue

                        val (x1, y1) = points[i]
                        val (x2, y2) = points[i + 1]
                        val ex = x1 + (x2 - x1) * segProgress
                        val ey = y1 + (y2 - y1) * segProgress

                        val c1 = valueColor(values[i])
                        val c2 = valueColor(values[i + 1])
                        val blendedC2 = lerpArgb(c1, c2, segProgress)

                        val segPaint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                strokeWidth = 8f
                                style = android.graphics.Paint.Style.STROKE
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                shader = android.graphics.LinearGradient(
                                        x1, y1, ex, ey,
                                        intArrayOf(c1, blendedC2),
                                        null,
                                        android.graphics.Shader.TileMode.CLAMP
                                )
                                alpha = (255 * segProgress.coerceAtMost(1f)).toInt()
                        }
                        drawContext.canvas.nativeCanvas.drawLine(x1, y1, ex, ey, segPaint)
                }

                // ── Dots + labels ──────────────────────────────────────────────
                val labelPaint = android.graphics.Paint().apply {
                        color = textSecondaryArgb
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 30f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT,
                                android.graphics.Typeface.BOLD
                        )
                }
                val valuePaint = android.graphics.Paint().apply {
                        color = textSecondaryArgb
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 24f
                        isAntiAlias = true
                }

                points.forEachIndexed { i, (x, y) ->
                        val pointProgress = ((t * totalPoints) - i).coerceIn(0f, 1f)
                        if (pointProgress <= 0f) return@forEachIndexed

                        val dotColor = valueColor(values[i])
                        val scale = if (pointProgress < 0.5f) pointProgress * 2f * 1.3f
                                    else 1.3f - (pointProgress - 0.5f) * 2f * 0.3f
                        val dotRadius = 9f * scale

                        // Outer glow
                        val glowPaint = android.graphics.Paint().apply {
                                color = dotColor
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                alpha = (80 * pointProgress).toInt()
                                maskFilter = android.graphics.BlurMaskFilter(
                                        dotRadius * 1.5f,
                                        android.graphics.BlurMaskFilter.Blur.NORMAL
                                )
                        }
                        drawContext.canvas.nativeCanvas.drawCircle(x, y, dotRadius * 1.4f, glowPaint)

                        // Dot fill
                        val dotPaint = android.graphics.Paint().apply {
                                color = dotColor
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                alpha = (255 * pointProgress).toInt()
                        }
                        drawContext.canvas.nativeCanvas.drawCircle(x, y, dotRadius, dotPaint)

                        // White center
                        drawContext.canvas.nativeCanvas.drawCircle(
                                x, y, dotRadius * 0.45f,
                                android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        isAntiAlias = true
                                        style = android.graphics.Paint.Style.FILL
                                        alpha = (255 * pointProgress).toInt()
                                }
                        )

                        // Day label
                        labelPaint.alpha = (255 * pointProgress).toInt()
                        drawContext.canvas.nativeCanvas.drawText(
                                dayNames.getOrElse(i) { "" },
                                x, size.height - 4f, labelPaint
                        )

                        // Value label above dot
                        if (values[i] > 0 && pointProgress > 0.3f) {
                                valuePaint.color = dotColor
                                valuePaint.alpha = (255 * ((pointProgress - 0.3f) / 0.7f).coerceIn(0f, 1f)).toInt()
                                drawContext.canvas.nativeCanvas.drawText(
                                        formatValue(values[i]),
                                        x, y - 14f, valuePaint
                                )
                        }
                }
        }
}

/** Linearly interpolate between two ARGB colors. */
private fun lerpArgb(c1: Int, c2: Int, t: Float): Int {
        val r1 = (c1 shr 16) and 0xFF; val g1 = (c1 shr 8) and 0xFF; val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF; val g2 = (c2 shr 8) and 0xFF; val b2 = c2 and 0xFF
        val r  = (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255)
        val g  = (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255)
        val b  = (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
}

