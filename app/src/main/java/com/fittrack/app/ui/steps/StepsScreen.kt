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
        // ── Color helpers ───────────────────────────────────────────────────────
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

        val textSecondaryArgb = android.graphics.Color.argb(
                (AppColors.textSecondary.alpha * 255).toInt(),
                (AppColors.textSecondary.red * 255).toInt(),
                (AppColors.textSecondary.green * 255).toInt(),
                (AppColors.textSecondary.blue * 255).toInt()
        )

        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(goal).coerceAtLeast(1)

        // Animation
        val revealProgress = remember { Animatable(0f) }
        LaunchedEffect(values) {
                revealProgress.snapTo(0f)
                revealProgress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
        }

        // Catmull-Rom → cubic bezier control points
        fun catmullToBezier(pts: List<Pair<Float,Float>>): List<FloatArray> {
                // Returns list of [x1,y1, cx1,cy1, cx2,cy2, x2,y2] for each segment
                val n = pts.size
                return (0 until n - 1).map { i ->
                        val p0 = pts.getOrElse(i - 1) { pts[i] }
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val p3 = pts.getOrElse(i + 2) { pts[i + 1] }
                        val tension = 0.35f
                        val cp1x = p1.first  + (p2.first  - p0.first)  * tension
                        val cp1y = p1.second + (p2.second - p0.second) * tension
                        val cp2x = p2.first  - (p3.first  - p1.first)  * tension
                        val cp2y = p2.second - (p3.second - p1.second) * tension
                        floatArrayOf(p1.first, p1.second, cp1x, cp1y, cp2x, cp2y, p2.first, p2.second)
                }
        }

        // Evaluate cubic bezier at t ∈ [0,1]
        fun bezierAt(seg: FloatArray, t: Float): Pair<Float,Float> {
                val mt = 1f - t
                val x = mt*mt*mt * seg[0] + 3*mt*mt*t * seg[2] + 3*mt*t*t * seg[4] + t*t*t * seg[6]
                val y = mt*mt*mt * seg[1] + 3*mt*mt*t * seg[3] + 3*mt*t*t * seg[5] + t*t*t * seg[7]
                return x to y
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val count = values.size
                if (count == 0) return@Canvas

                val bottomPad = 36f   // day labels
                val topPad    = 28f   // value labels
                val leftPad   = 12f
                val rightPad  = 12f
                val chartW = size.width  - leftPad - rightPad
                val chartH = size.height - topPad  - bottomPad

                fun valueToY(v: Int)   = topPad + chartH - (v.toFloat() / maxValue) * chartH
                fun indexToX(i: Int)   = leftPad + chartW * i / (count - 1).coerceAtLeast(1)

                val pts = values.mapIndexed { i, v -> indexToX(i) to valueToY(v) }
                val bezSegs = catmullToBezier(pts)

                val t = revealProgress.value
                val totalSegs = bezSegs.size.toFloat()

                val nativeCanvas = drawContext.canvas.nativeCanvas

                // ── Grid lines ────────────────────────────────────────────────────
                val gridPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1.5f
                        color = android.graphics.Color.argb(25, 255, 255, 255)
                }
                for (fraction in listOf(0.25f, 0.5f, 0.75f, 1.0f)) {
                        val gridV = (maxValue * fraction).toInt()
                        val gridY = valueToY(gridV)
                        if (gridY < topPad || gridY > topPad + chartH) continue
                        gridPaint.color = if (fraction == 1.0f)
                                android.graphics.Color.argb(50, 76, 175, 80)   // green tint at goal
                        else
                                android.graphics.Color.argb(20, 255, 255, 255)
                        nativeCanvas.drawLine(leftPad, gridY, size.width - rightPad, gridY, gridPaint)
                }

                // ── Goal band ─────────────────────────────────────────────────────
                if (goal > 0) {
                        val goalY = valueToY(goal)
                        // Thin green shimmering line
                        val goalLinePaint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                strokeWidth = 2.5f
                                style = android.graphics.Paint.Style.STROKE
                                shader = android.graphics.LinearGradient(
                                        leftPad, goalY, size.width - rightPad, goalY,
                                        intArrayOf(
                                                android.graphics.Color.argb(0,   76, 175, 80),
                                                android.graphics.Color.argb(160, 76, 175, 80),
                                                android.graphics.Color.argb(160, 76, 175, 80),
                                                android.graphics.Color.argb(0,   76, 175, 80),
                                        ),
                                        floatArrayOf(0f, 0.15f, 0.85f, 1f),
                                        android.graphics.Shader.TileMode.CLAMP
                                )
                                alpha = (200 * t).toInt()
                                pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 6f), 0f)
                        }
                        nativeCanvas.drawLine(leftPad, goalY, size.width - rightPad, goalY, goalLinePaint)

                        // "Goal" label on the right edge
                        val goalLabelPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((160 * t).toInt(), 76, 175, 80)
                                textSize = 22f
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        nativeCanvas.drawText("goal", size.width - rightPad - 2f, goalY - 6f, goalLabelPaint)
                }

                // ── Build the animated bezier path ────────────────────────────────
                // We sample the curve to build the fill path and per-segment gradient strokes
                val SAMPLES = 30  // subdivisions per segment for smooth fill

                if (bezSegs.isNotEmpty()) {
                        // ── Fill area ──────────────────────────────────────────────
                        val fillPath = android.graphics.Path()
                        fillPath.moveTo(pts[0].first, topPad + chartH)  // bottom-left anchor
                        fillPath.lineTo(pts[0].first, pts[0].second)

                        for (si in bezSegs.indices) {
                                val seg = bezSegs[si]
                                val segReveal = ((t * totalSegs) - si).coerceIn(0f, 1f)
                                if (segReveal <= 0f) break
                                for (s in 1..SAMPLES) {
                                        val frac = (s.toFloat() / SAMPLES) * segReveal
                                        val (bx, by) = bezierAt(seg, frac)
                                        fillPath.lineTo(bx, by)
                                }
                                if (segReveal < 1f) {
                                        val (endX, _) = bezierAt(seg, segReveal)
                                        // close back to bottom
                                        fillPath.lineTo(endX, topPad + chartH)
                                        break
                                }
                                if (si == bezSegs.lastIndex) {
                                        fillPath.lineTo(pts.last().first, topPad + chartH)
                                }
                        }
                        fillPath.close()

                        val fillPaint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                shader = android.graphics.LinearGradient(
                                        0f, topPad,
                                        0f, topPad + chartH,
                                        intArrayOf(
                                                android.graphics.Color.argb(55, 255, 255, 255),
                                                android.graphics.Color.argb(18, 255, 255, 255),
                                                android.graphics.Color.TRANSPARENT
                                        ),
                                        floatArrayOf(0f, 0.45f, 1f),
                                        android.graphics.Shader.TileMode.CLAMP
                                )
                                alpha = (220 * t).toInt()
                        }
                        nativeCanvas.drawPath(fillPath, fillPaint)

                        // ── Bezier strokes with gradient per segment ───────────────
                        for (si in bezSegs.indices) {
                                val seg = bezSegs[si]
                                val segReveal = ((t * totalSegs) - si).coerceIn(0f, 1f)
                                if (segReveal <= 0f) break

                                val c1 = valueColor(values[si])
                                val c2 = valueColor(values[(si + 1).coerceAtMost(values.lastIndex)])

                                // Draw the bezier segment as a series of short strokes
                                // using a gradient paint from c1→c2
                                val (startX, startY) = bezierAt(seg, 0f)
                                val (endX,   endY  ) = bezierAt(seg, segReveal)

                                val strokePaint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        strokeWidth = 7f
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        shader = android.graphics.LinearGradient(
                                                startX, startY, endX, endY,
                                                intArrayOf(c1, lerpArgb(c1, c2, segReveal)),
                                                null,
                                                android.graphics.Shader.TileMode.CLAMP
                                        )
                                        alpha = (255 * segReveal).toInt()
                                }

                                val segPath = android.graphics.Path()
                                var prev = bezierAt(seg, 0f)
                                segPath.moveTo(prev.first, prev.second)
                                for (s in 1..SAMPLES) {
                                        val frac = (s.toFloat() / SAMPLES) * segReveal
                                        val (bx, by) = bezierAt(seg, frac)
                                        segPath.cubicTo(
                                                prev.first + (bx - prev.first) * 0.33f,
                                                prev.second,
                                                bx - (bx - prev.first) * 0.33f,
                                                by,
                                                bx, by
                                        )
                                        prev = bx to by
                                }
                                nativeCanvas.drawPath(segPath, strokePaint)

                                // Soft glow pass (wider, lower alpha)
                                val glowStrokePaint = android.graphics.Paint(strokePaint).apply {
                                        strokeWidth = 18f
                                        alpha = (40 * segReveal).toInt()
                                        maskFilter = android.graphics.BlurMaskFilter(
                                                14f, android.graphics.BlurMaskFilter.Blur.NORMAL
                                        )
                                }
                                nativeCanvas.drawPath(segPath, glowStrokePaint)
                        }
                }

                // ── Dots ───────────────────────────────────────────────────────────
                val labelPaint = android.graphics.Paint().apply {
                        color = textSecondaryArgb
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
                        )
                }
                val valuePaint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 26f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val pillPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        color = android.graphics.Color.argb(120, 20, 20, 30)
                }

                pts.forEachIndexed { i, (x, y) ->
                        val ptProg = ((t * totalSegs) - (i - 1)).coerceIn(0f, 1f)
                        if (ptProg <= 0f) return@forEachIndexed

                        val dotColor = valueColor(values[i])
                        val isLast = i == pts.lastIndex
                        val baseRadius = if (isLast) 11f else 8f

                        val bounce = if (ptProg < 0.6f) ptProg / 0.6f * 1.25f
                                     else 1.25f - (ptProg - 0.6f) / 0.4f * 0.25f
                        val dotR = baseRadius * bounce * ptProg

                        // ① Outer soft glow
                        nativeCanvas.drawCircle(x, y, dotR * 2.4f,
                                android.graphics.Paint().apply {
                                        color = dotColor; isAntiAlias = true
                                        style = android.graphics.Paint.Style.FILL
                                        alpha = (35 * ptProg).toInt()
                                        maskFilter = android.graphics.BlurMaskFilter(
                                                dotR * 2.2f, android.graphics.BlurMaskFilter.Blur.NORMAL
                                        )
                                })

                        // ② Ring (outer border)
                        nativeCanvas.drawCircle(x, y, dotR + 3.5f,
                                android.graphics.Paint().apply {
                                        color = dotColor; isAntiAlias = true
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = 2f
                                        alpha = (130 * ptProg).toInt()
                                })

                        // ③ Filled dot
                        nativeCanvas.drawCircle(x, y, dotR,
                                android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        style = android.graphics.Paint.Style.FILL
                                        shader = android.graphics.RadialGradient(
                                                x - dotR * 0.3f, y - dotR * 0.3f, dotR * 1.2f,
                                                intArrayOf(
                                                        lerpArgb(dotColor, android.graphics.Color.WHITE, 0.4f),
                                                        dotColor,
                                                        lerpArgb(dotColor, android.graphics.Color.BLACK, 0.25f)
                                                ),
                                                floatArrayOf(0f, 0.55f, 1f),
                                                android.graphics.Shader.TileMode.CLAMP
                                        )
                                        alpha = (255 * ptProg).toInt()
                                })

                        // ④ White specular highlight
                        nativeCanvas.drawCircle(x - dotR * 0.28f, y - dotR * 0.28f, dotR * 0.28f,
                                android.graphics.Paint().apply {
                                        color = android.graphics.Color.argb((190 * ptProg).toInt(), 255, 255, 255)
                                        isAntiAlias = true
                                        style = android.graphics.Paint.Style.FILL
                                })

                        // ⑤ Today: extra pulsing outer ring
                        if (isLast && ptProg > 0.8f) {
                                nativeCanvas.drawCircle(x, y, dotR + 7f,
                                        android.graphics.Paint().apply {
                                                color = dotColor; isAntiAlias = true
                                                style = android.graphics.Paint.Style.STROKE
                                                strokeWidth = 1.5f
                                                alpha = (60 * ptProg).toInt()
                                        })
                        }

                        // ⑥ Day label below
                        labelPaint.alpha = (220 * ptProg).toInt()
                        nativeCanvas.drawText(
                                dayNames.getOrElse(i) { "" },
                                x, size.height - 6f, labelPaint
                        )

                        // ⑦ Value label above dot — on a pill background
                        if (values[i] > 0 && ptProg > 0.4f) {
                                val labelFade = ((ptProg - 0.4f) / 0.6f).coerceIn(0f, 1f)
                                val text = formatValue(values[i])
                                val textY = y - dotR - 10f

                                // Pill bg
                                val tw = valuePaint.measureText(text)
                                val pillH = 30f
                                pillPaint.alpha = (100 * labelFade).toInt()
                                nativeCanvas.drawRoundRect(
                                        x - tw / 2f - 8f, textY - pillH * 0.75f,
                                        x + tw / 2f + 8f, textY + pillH * 0.25f,
                                        8f, 8f, pillPaint
                                )

                                valuePaint.color = dotColor
                                valuePaint.alpha = (255 * labelFade).toInt()
                                nativeCanvas.drawText(text, x, textY, valuePaint)
                        }
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


