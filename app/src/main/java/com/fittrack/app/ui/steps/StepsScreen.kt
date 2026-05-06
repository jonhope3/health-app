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
import androidx.compose.ui.platform.LocalDensity
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
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
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
                                val avgSteps = if (stepValues.isNotEmpty())
                                        stepValues.average().toInt() else 0

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "Step History",
                                                fontFamily = interFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp,
                                                color = AppColors.textPrimary
                                        )
                                        if (avgSteps > 0) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                                text = if (avgSteps >= 1000) "${avgSteps / 1000}k" else "$avgSteps",
                                                                fontFamily = interFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                color = AppColors.textPrimary
                                                        )
                                                        Text(
                                                                text = "7-day avg",
                                                                fontFamily = interFamily,
                                                                fontSize = 10.sp,
                                                                color = AppColors.textSecondary
                                                        )
                                                }
                                        }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
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
        if (values.isEmpty()) return

        // ── Semantic color based on goal % ───────────────────────────────────
        fun valueColor(v: Int): Int {
                val pct = if (goal > 0) v.toFloat() / goal else 0f
                return when {
                        pct >= 1.0f  -> android.graphics.Color.parseColor("#4CAF50")
                        pct >= 0.75f -> lerpArgb(
                                android.graphics.Color.parseColor("#FF9800"),
                                android.graphics.Color.parseColor("#4CAF50"),
                                (pct - 0.75f) / 0.25f
                        )
                        pct >= 0.4f  -> lerpArgb(
                                android.graphics.Color.parseColor("#F44336"),
                                android.graphics.Color.parseColor("#FF9800"),
                                (pct - 0.4f) / 0.35f
                        )
                        else -> android.graphics.Color.parseColor("#F44336")
                }
        }

        val maxValue = remember(values, goal) {
                ((values.maxOrNull() ?: 1).coerceAtLeast(goal).toFloat() * 1.08f).toInt().coerceAtLeast(1)
        }

        val revealProgress = remember { Animatable(0f) }
        LaunchedEffect(values) {
                revealProgress.snapTo(0f)
                revealProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
        }

        // Catmull-Rom → cubic bezier
        fun catmullToBezier(pts: List<Pair<Float, Float>>): List<FloatArray> =
                (0 until pts.size - 1).map { i ->
                        val p0 = pts.getOrElse(i - 1) { pts[i] }
                        val p1 = pts[i]; val p2 = pts[i + 1]
                        val p3 = pts.getOrElse(i + 2) { pts[i + 1] }
                        val ten = 0.3f
                        floatArrayOf(
                                p1.first, p1.second,
                                p1.first  + (p2.first  - p0.first)  * ten,
                                p1.second + (p2.second - p0.second) * ten,
                                p2.first  - (p3.first  - p1.first)  * ten,
                                p2.second - (p3.second - p1.second) * ten,
                                p2.first, p2.second
                        )
                }

        fun bezierAt(seg: FloatArray, t: Float): Pair<Float, Float> {
                val m = 1f - t
                return (m*m*m*seg[0] + 3*m*m*t*seg[2] + 3*m*t*t*seg[4] + t*t*t*seg[6]) to
                       (m*m*m*seg[1] + 3*m*m*t*seg[3] + 3*m*t*t*seg[5] + t*t*t*seg[7])
        }

        val density = LocalDensity.current

        // Pre-compute all dp/sp → px conversions outside Canvas (avoids DrawScope ambiguity)
        val axisTextPx: Float
        val dayTextPx: Float
        val lpPx: Float; val rpPx: Float; val tpPx: Float; val bpPx: Float
        val strokeWidthPx: Float
        val goalStrokeWidthPx: Float
        val dashPx: Float; val gapPx: Float
        val outerRTodayPx: Float; val outerRNormalPx: Float
        val innerRTodayPx: Float; val innerRNormalPx: Float
        val todayRingGapPx: Float; val todayRingStrokePx: Float
        val axisGapPx: Float; val dayBottomPx: Float

        val dpToPx = density.density
        val spToPx = density.density * density.fontScale
        axisTextPx        = 10f * spToPx
        dayTextPx         = 10f * spToPx
        lpPx              = 44f * dpToPx
        rpPx              = 8f  * dpToPx
        tpPx              = 12f * dpToPx
        bpPx              = 32f * dpToPx
        strokeWidthPx     = 2.5f * dpToPx
        goalStrokeWidthPx = 1.5f * dpToPx
        dashPx            = 7f  * dpToPx
        gapPx             = 4f  * dpToPx
        outerRTodayPx     = 7f  * dpToPx
        outerRNormalPx    = 4.5f * dpToPx
        innerRTodayPx     = 4.5f * dpToPx
        innerRNormalPx    = 2.5f * dpToPx
        todayRingGapPx    = 3f  * dpToPx
        todayRingStrokePx = 1f  * dpToPx
        axisGapPx         = 6f  * dpToPx
        dayBottomPx       = 4f  * dpToPx

        Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                val nc = drawContext.canvas.nativeCanvas
                val n  = values.size

                val lp = lpPx; val rp = rpPx; val tp = tpPx
                val cw = size.width - lp - rp
                val ch = size.height - tp - bpPx

                fun vy(v: Int) = tp + ch * (1f - v.toFloat() / maxValue)
                fun ix(i: Int) = lp + cw * i.toFloat() / (n - 1).coerceAtLeast(1)

                val pts    = values.mapIndexed { i, v -> ix(i) to vy(v) }
                val segs   = if (n >= 2) catmullToBezier(pts) else emptyList()
                val t      = revealProgress.value
                val totSeg = segs.size.toFloat()
                val SAMP   = 24

                // ── Y-axis grid + labels (3 levels) ────────────────────────
                val gridPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1f
                }
                val axisTextPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = axisTextPx
                        textAlign = android.graphics.Paint.Align.RIGHT
                        color = android.graphics.Color.argb(110, 180, 180, 210)
                }
                listOf(0.33f, 0.67f, 1.0f).forEach { frac ->
                        val v = (maxValue * frac).toInt()
                        val y = vy(v)
                        if (y < tp || y > tp + ch) return@forEach
                        // Skip this grid label if it's within 8% of maxValue of the goal —
                        // the goal's own dashed line + label already covers that position.
                        val tooCloseToGoal = goal > 0 && kotlin.math.abs(v - goal).toFloat() / maxValue < 0.08f
                        if (tooCloseToGoal) return@forEach
                        gridPaint.color = android.graphics.Color.argb(18, 200, 200, 240)
                        nc.drawLine(lp, y, size.width - rp, y, gridPaint)
                        nc.drawText(formatValue(v), lp - axisGapPx, y + axisTextPx * 0.4f, axisTextPaint)
                }

                // ── Goal reference line ─────────────────────────────────────
                if (goal > 0) {
                        val gy = vy(goal)
                        if (gy in tp..(tp + ch)) {
                                val goalAlpha = (210 * t).toInt()
                                val localDashPx = dashPx; val localGapPx = gapPx
                                nc.drawLine(lp, gy, size.width - rp, gy,
                                        android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                style = android.graphics.Paint.Style.STROKE
                                                strokeWidth = goalStrokeWidthPx
                                                color = android.graphics.Color.argb(goalAlpha, 76, 175, 80)
                                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(localDashPx, localGapPx), 0f)
                                        })
                                // "GOAL Xk" label right-aligned at goal line
                                nc.drawText(
                                        "GOAL ${formatValue(goal)}",
                                        lp - axisGapPx,
                                        gy - axisTextPx * 0.5f,
                                        android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                textSize = axisTextPx
                                                textAlign = android.graphics.Paint.Align.RIGHT
                                                color = android.graphics.Color.argb(goalAlpha, 100, 210, 110)
                                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        })
                        }
                }

                if (segs.isEmpty()) return@Canvas

                // ── Build animated fill path ────────────────────────────────
                val fillPath = android.graphics.Path()
                fillPath.moveTo(pts[0].first, tp + ch)
                fillPath.lineTo(pts[0].first, pts[0].second)
                var lastRevX = pts[0].first

                for (si in segs.indices) {
                        val seg = segs[si]
                        val rev = ((t * totSeg) - si).coerceIn(0f, 1f)
                        if (rev <= 0f) break
                        for (s in 1..SAMP) {
                                val (bx, by) = bezierAt(seg, (s.toFloat() / SAMP) * rev)
                                fillPath.lineTo(bx, by)
                                lastRevX = bx
                        }
                        if (rev < 1f) break
                        if (si == segs.lastIndex) lastRevX = pts.last().first
                }
                fillPath.lineTo(lastRevX, tp + ch)
                fillPath.close()

                // Fill tint = semantic color of average performance
                val avgPct  = if (goal > 0) (values.average() / goal).toFloat() else 0f
                val avgColor = valueColor((avgPct * goal).toInt())
                nc.drawPath(fillPath, android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        shader = android.graphics.LinearGradient(
                                0f, tp, 0f, tp + ch,
                                intArrayOf(
                                        (avgColor and 0x00FFFFFF) or (0x40 shl 24),  // 25% alpha top
                                        android.graphics.Color.TRANSPARENT
                                ),
                                floatArrayOf(0f, 1f),
                                android.graphics.Shader.TileMode.CLAMP
                        )
                })

                // ── Line strokes (per segment, gradient c1→c2) ──────────────
                for (si in segs.indices) {
                        val seg = segs[si]
                        val rev = ((t * totSeg) - si).coerceIn(0f, 1f)
                        if (rev <= 0f) break

                        val c1 = valueColor(values[si])
                        val c2 = valueColor(values[(si + 1).coerceAtMost(values.lastIndex)])
                        val (sx, sy) = bezierAt(seg, 0f)
                        val (ex, ey) = bezierAt(seg, rev)

                        val segPath = android.graphics.Path()
                        segPath.moveTo(sx, sy)
                        for (s in 1..SAMP) {
                                val (bx, by) = bezierAt(seg, (s.toFloat() / SAMP) * rev)
                                segPath.lineTo(bx, by)
                        }
                        nc.drawPath(segPath, android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = strokeWidthPx
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                strokeCap  = android.graphics.Paint.Cap.ROUND
                                shader = android.graphics.LinearGradient(
                                        sx, sy, if (ex == sx) ex + 0.01f else ex, ey,
                                        intArrayOf(c1, if (rev < 1f) lerpArgb(c1, c2, rev) else c2),
                                        null, android.graphics.Shader.TileMode.CLAMP
                                )
                        })
                }

                // ── Dots + day labels ───────────────────────────────────────
                val dayPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize  = dayTextPx
                }

                pts.forEachIndexed { i, (x, y) ->
                        val prog = ((t * totSeg) - (i - 1)).coerceIn(0f, 1f)
                        if (prog <= 0f) return@forEachIndexed

                        val dotColor = valueColor(values[i])
                        val isToday  = i == pts.lastIndex
                        val outerR   = if (isToday) outerRTodayPx else outerRNormalPx
                        val innerR   = if (isToday) innerRTodayPx else innerRNormalPx

                        // Dark background disc (creates contrast ring effect)
                        nc.drawCircle(x, y, outerR, android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                color = android.graphics.Color.argb((230 * prog).toInt(), 16, 16, 28)
                        })
                        // Colored inner fill
                        nc.drawCircle(x, y, innerR, android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                color = dotColor
                                alpha = (255 * prog).toInt()
                        })
                        // Today: subtle outer ring
                        if (isToday) {
                                nc.drawCircle(x, y, outerR + todayRingGapPx,
                                        android.graphics.Paint().apply {
                                                isAntiAlias = true
                                                style = android.graphics.Paint.Style.STROKE
                                                strokeWidth = todayRingStrokePx
                                                color = android.graphics.Color.argb(
                                                        (70 * prog).toInt(),
                                                        android.graphics.Color.red(dotColor),
                                                        android.graphics.Color.green(dotColor),
                                                        android.graphics.Color.blue(dotColor)
                                                )
                                        })
                        }

                        // Day label
                        dayPaint.color = if (isToday)
                                android.graphics.Color.argb((230 * prog).toInt(), 210, 210, 230)
                        else
                                android.graphics.Color.argb((150 * prog).toInt(), 160, 160, 185)
                        dayPaint.typeface = if (isToday) android.graphics.Typeface.DEFAULT_BOLD
                                           else android.graphics.Typeface.DEFAULT
                        nc.drawText(
                                dayNames.getOrElse(i) { "" },
                                x, size.height - dayBottomPx,
                                dayPaint
                        )
                }
        }
}


/** Linearly interpolate between two ARGB colors. */
private fun lerpArgb(c1: Int, c2: Int, t: Float): Int {
        val r1 = (c1 shr 16) and 0xFF; val g1 = (c1 shr 8) and 0xFF; val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF; val g2 = (c2 shr 8) and 0xFF; val b2 = c2 and 0xFF
        return android.graphics.Color.rgb(
                (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255),
                (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255),
                (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)
        )
}


