package com.hopehealth.app.ui.family

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hopehealth.app.data.CyclePhase
import com.hopehealth.app.data.CervicalMucusType
import com.hopehealth.app.data.FlowIntensity
import com.hopehealth.app.data.MoodType
import com.hopehealth.app.data.SexDriveLevel
import com.hopehealth.app.data.SexualActivityType
import com.hopehealth.app.data.Symptom
import com.hopehealth.app.data.db.TemperatureReadingEntity
import com.hopehealth.app.theme.FamilyColors
import com.hopehealth.app.theme.interFamily
import com.hopehealth.app.ui.common.ScreenScaffold
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogger by remember { mutableStateOf(false) }
    var loggerDate by remember { mutableStateOf(LocalDate.now()) }
    var loggerExisting by remember { mutableStateOf<com.hopehealth.app.data.db.DailyCycleLogEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Family", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = FamilyColors.primary)
            }
            Spacer(Modifier.height(16.dp))
            CycleStatusCard(state)
            Spacer(Modifier.height(16.dp))

            // TTC toggle
            run {
                var showTtcInfo by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trying to Conceive", fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.Info, contentDescription = "TTC info",
                            modifier = Modifier.size(18.dp).clip(CircleShape).clickable { showTtcInfo = !showTtcInfo },
                            tint = if (showTtcInfo) FamilyColors.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    Switch(checked = state.isTtcMode, onCheckedChange = { viewModel.toggleTtcMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = FamilyColors.ovulatory, checkedTrackColor = FamilyColors.ovulatory.copy(alpha = 0.3f)))
                }
                androidx.compose.animation.AnimatedVisibility(visible = showTtcInfo) {
                    Text(
                        "Enables conception probability tracking based on your cycle phase. Shows daily conception odds and optimized tips when toggled on.",
                        fontFamily = interFamily, fontSize = 12.sp, lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(10.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Calendar
            CycleCalendar(state.viewingMonth, state.monthLogs, state.prediction, state.currentCycle,
                state.futurePredictions,
                { viewModel.navigateMonth(-1) }, { viewModel.navigateMonth(1) },
                allCycles = state.allCycles,
                onLogDay = { date ->
                    loggerDate = date
                    viewModel.loadLogForDate(date) { log ->
                        loggerExisting = log
                        showLogger = true
                    }
                },
                onPeriodStart = { date ->
                    viewModel.logPeriodStart(date)
                },
                onDeleteLog = { date ->
                    viewModel.deleteLogForDate(date)
                },
            )
            Spacer(Modifier.height(16.dp))

            // Coach insight
            InsightCard(state, { viewModel.generateAiInsight() })
            Spacer(Modifier.height(16.dp))

            // Temperature chart (always shown)
            TemperatureChart(state.temperatureReadings, state.prediction?.coverlineTemp)
            Spacer(Modifier.height(16.dp))

            // Symptom signals
            if (state.symptomSignals.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FamilyColors.ovulatory.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Symptom Signals", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FamilyColors.ovulatory)
                        Spacer(Modifier.height(6.dp))
                        state.symptomSignals.forEach { Text(it, fontFamily = interFamily, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // TTC conception card
            if (state.isTtcMode) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FamilyColors.fertileHigh.copy(alpha = 0.12f))) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Conception Probability", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, color = FamilyColors.fertileHigh)
                        if (state.conceptionProbability > 0f) {
                            Text("${(state.conceptionProbability * 100).toInt()}%", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = FamilyColors.fertileHigh)
                            Text("Time intercourse every 1-2 days during your fertile window.", fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text("Probability updates as you log more cycle data. Keep tracking to get accurate estimates.", fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }




            // Cycle stats
            state.cycleStats?.let { stats ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cycle History", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            StatPill("Avg", "${stats.averageLength}d")
                            StatPill("Short", "${stats.shortestCycle}d")
                            StatPill("Long", "${stats.longestCycle}d")
                            StatPill("Period", "${stats.averagePeriodLength}d")
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Regularity: ${stats.cycleRegularity}", fontFamily = interFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        stats.anomalies.forEach { Text(it, fontFamily = interFamily, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }

        FloatingActionButton(onClick = {
            loggerDate = LocalDate.now()
            loggerExisting = state.todayLog
            showLogger = true
        }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = FamilyColors.primary, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, "Log Today"); Spacer(Modifier.width(8.dp))
                Text("Log Today", fontFamily = interFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLogger) {
        DailyLoggerSheet(
            existingLog = loggerExisting,
            targetDate = loggerDate,
            onDismiss = { showLogger = false },
            onSave = { flow, mucus, symptoms, mood, sexDrive, sexActivity, temp ->
                viewModel.saveDailyLog(
                    date = loggerDate,
                    flowIntensity = flow, cervicalMucus = mucus, symptoms = symptoms,
                    mood = mood, sexDrive = sexDrive, sexualActivity = sexActivity, temperature = temp,
                    temperatureSource = if (temp != null) "MANUAL" else null,
                )
                showLogger = false
            },
            onStartPeriod = { viewModel.logPeriodStart(loggerDate); showLogger = false },
            onEndPeriod = { date -> viewModel.logPeriodEnd(date); showLogger = false },
            hasActiveCycle = state.currentCycle?.endDate == null && state.currentCycle != null,
            periodStartDate = state.currentCycle?.startDate?.let { LocalDate.parse(it) },
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FamilyColors.primary)
        Text(label, fontFamily = interFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══ Temperature Chart ═══
@Composable
private fun TemperatureChart(readings: List<TemperatureReadingEntity>, coverline: Float?) {
    val dailyTemps = readings.groupBy { it.date }.mapValues { (_, r) -> r.maxByOrNull { it.timestamp }!!.temperatureF }.toSortedMap().entries.toList()

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Temperature Trend", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (coverline != null) Text("Coverline: ${"%.1f".format(coverline)}°F", fontFamily = interFamily, fontSize = 11.sp, color = FamilyColors.coverline)

            if (dailyTemps.size < 3) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Log at least 3 days of temperature to see your trend chart.",
                    fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Card
            }
            Spacer(Modifier.height(8.dp))
            val lineColor = FamilyColors.tempLine
            val coverColor = FamilyColors.coverline
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val temps = dailyTemps.map { it.value }
                val minT = (temps.min() - 0.3f)
                val maxT = (temps.max() + 0.3f)
                val range = (maxT - minT).coerceAtLeast(0.5f)
                val stepX = size.width / (temps.size - 1).coerceAtLeast(1)

                // Coverline
                if (coverline != null) {
                    val cy = size.height - ((coverline - minT) / range) * size.height
                    drawLine(coverColor, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                }

                // Line
                val path = Path()
                temps.forEachIndexed { i, t ->
                    val x = i * stepX; val y = size.height - ((t - minT) / range) * size.height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                // Dots
                temps.forEachIndexed { i, t ->
                    val x = i * stepX; val y = size.height - ((t - minT) / range) * size.height
                    val aboveCoverline = coverline != null && t >= coverline + 0.2f
                    drawCircle(if (aboveCoverline) FamilyColors.ovulatory else lineColor, 4f, Offset(x, y))
                }
            }
        }
    }
}

// ═══ Cycle Status Card ═══
@Composable
private fun CycleStatusCard(state: FamilyUiState) {
    val phaseColor = when (state.currentPhase) { CyclePhase.MENSTRUAL -> FamilyColors.menstrual; CyclePhase.FOLLICULAR -> FamilyColors.follicular; CyclePhase.OVULATORY -> FamilyColors.ovulatory; CyclePhase.LUTEAL -> FamilyColors.luteal }
    val animatedColor by animateColorAsState(phaseColor, spring())
    val phaseName = state.currentPhase.name.lowercase().replaceFirstChar { it.uppercase() }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(0.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(animatedColor.copy(alpha = 0.15f), animatedColor.copy(alpha = 0.05f))), RoundedCornerShape(20.dp)).border(1.dp, animatedColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(20.dp)) {
            Column {
                if (state.currentCycle != null) {
                    Text("Cycle Day ${state.currentCycleDay} · $phaseName", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = animatedColor)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(FamilyColors.menstrual, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Period in ~${state.daysUntilPeriod} days", fontFamily = interFamily, fontSize = 15.sp)
                        Spacer(Modifier.width(12.dp))
                        val confLabel = state.confidence.name.lowercase().replaceFirstChar { it.uppercase() }
                        Text("· $confLabel confidence", fontFamily = interFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fertColor = when (state.fertilityLevel) { "High" -> FamilyColors.fertileHigh; "Medium" -> FamilyColors.fertileMedium; else -> FamilyColors.fertileLow }
                        Box(Modifier.size(8.dp).background(fertColor, CircleShape)); Spacer(Modifier.width(6.dp))
                        Text("Fertility: ${state.fertilityScore}/10", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("· ${state.fertilityAccuracy}", fontFamily = interFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("No active cycle", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FamilyColors.primary)
                    Spacer(Modifier.height(4.dp)); Text("Tap \"Log Today\" to start tracking your period", fontFamily = interFamily, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ═══ Cycle Calendar ═══
@Composable
private fun CycleCalendar(
    viewingMonth: YearMonth,
    monthLogs: List<com.hopehealth.app.data.db.DailyCycleLogEntity>,
    prediction: com.hopehealth.app.services.CyclePredictionEngine.CyclePrediction?,
    currentCycle: com.hopehealth.app.data.db.CycleRecordEntity?,
    projectedCycles: List<ProjectedCycle>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLogDay: (LocalDate) -> Unit,
    onPeriodStart: (LocalDate) -> Unit,
    onDeleteLog: (LocalDate) -> Unit,
    allCycles: List<com.hopehealth.app.data.db.CycleRecordEntity> = emptyList(),
) {
    val logsByDate = monthLogs.associateBy { it.date }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val cycleStartDates = remember(allCycles) { allCycles.map { it.startDate }.toSet() }

    // Helper: check a date against all projected cycles
    fun dateInfo(date: LocalDate): Triple<Boolean, Boolean, Boolean> {
        val logMenstrual = logsByDate[date.toString()]?.flowIntensity != null
        var isProjPeriod = false
        var isProjFertile = false
        var isProjOvulation = false
        for (p in projectedCycles) {
            if (date in p.periodStart..p.periodEnd) isProjPeriod = true
            if (date == p.ovulationDate) isProjOvulation = true
            if (date in p.fertileStart..p.fertileEnd) isProjFertile = true
        }
        return Triple(
            logMenstrual || isProjPeriod,
            isProjFertile,
            isProjOvulation,
        )
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            // Month navigation
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = FamilyColors.primary) }
                Text("${viewingMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${viewingMonth.year}", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = FamilyColors.primary) }
            }
            Spacer(Modifier.height(8.dp))

            // Day-of-week headers
            Row(Modifier.fillMaxWidth()) {
                for (d in listOf("M","T","W","T","F","S","S")) {
                    Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))

            // Calendar grid
            val firstDay = viewingMonth.atDay(1)
            val offset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = viewingMonth.lengthOfMonth()
            val rows = (offset + daysInMonth + 6) / 7

            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val dayNum = row * 7 + col - offset + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = viewingMonth.atDay(dayNum)
                            val isToday = date == today
                            val isSelected = date == selectedDate
                            val (isMenstrual, isFertile, isOvulation) = dateInfo(date)
                            val isFuture = date.isAfter(today)
                            val dotColor = when {
                                isMenstrual -> FamilyColors.menstrual
                                isOvulation -> Color(0xFF1E88E5)
                                isFertile -> FamilyColors.ovulatory
                                else -> null
                            }
                            val finalDotColor = if (isFuture && dotColor != null) dotColor.copy(alpha = 0.55f) else dotColor

                            val isCycleStart = date.toString() in cycleStartDates

                            Box(
                                Modifier
                                    .weight(1f).aspectRatio(1f).padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCycleStart -> FamilyColors.menstrual
                                            isSelected -> FamilyColors.primarySurface.copy(alpha = 0.3f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(if (isToday) Modifier.border(1.5.dp, FamilyColors.primary, CircleShape) else Modifier)
                                    .clickable { selectedDate = if (selectedDate == date) null else date },
                                Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$dayNum", fontFamily = interFamily, fontSize = 13.sp,
                                        fontWeight = if (isToday || isCycleStart) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isCycleStart -> Color.White
                                            isToday -> FamilyColors.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (finalDotColor != null && !isCycleStart) Box(Modifier.size(5.dp).background(finalDotColor, CircleShape))
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Selected day detail with action buttons
            if (selectedDate != null) {
                val sel = selectedDate!!
                val selLog = logsByDate[sel.toString()]
                val (selMenstrual, selFertile, selOvulation) = dateInfo(sel)
                val isFutureSel = sel.isAfter(today)

                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${sel.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${sel.dayOfMonth}" +
                                if (isFutureSel) " (predicted)" else "",
                            fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (selMenstrual) {
                            val flowNote = if (!isFutureSel) selLog?.flowIntensity?.let { " — ${it.lowercase()} flow" } ?: "" else ""
                            DotLegendRow(FamilyColors.menstrual, if (isFutureSel) "Predicted period" else "Period day$flowNote")
                        }
                        if (selOvulation) {
                            DotLegendRow(Color(0xFF1E88E5), "Predicted ovulation day")
                        } else if (selFertile) {
                            DotLegendRow(FamilyColors.ovulatory, "Fertile window — higher chance of conception")
                        }
                        if (!selMenstrual && !selFertile && !selOvulation) {
                            Text("No cycle events", fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // Fertility score for this day
                        if (projectedCycles.isNotEmpty()) {
                            val dayScore = FamilyUiState.fertilityScoreForDate(sel, projectedCycles)
                            val accuracy = if (currentCycle != null && prediction != null) FamilyUiState.accuracyLabel(prediction.confidence) else "Low accuracy"
                            val scoreColor = when {
                                dayScore >= 8 -> FamilyColors.fertileHigh
                                dayScore >= 5 -> FamilyColors.fertileMedium
                                else -> FamilyColors.fertileLow
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fertility: $dayScore/10", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scoreColor)
                                Spacer(Modifier.width(6.dp))
                                Text("· $accuracy", fontFamily = interFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (selLog != null) {
                            val logged = mutableListOf<String>()
                            selLog.cervicalMucus?.let { logged.add("CM: ${it.lowercase().replace("_", " ")}") }
                            selLog.mood?.let { logged.add("Mood: ${it.lowercase()}") }
                            selLog.temperature?.let { logged.add("Temp: ${it}°F") }
                            if (logged.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(logged.joinToString(" · "), fontFamily = interFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Action buttons
                        if (!isFutureSel) {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onLogDay(sel) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, FamilyColors.primary),
                                ) {
                                    Text(
                                        if (selLog != null) "Edit Log" else "Log Feelings",
                                        fontFamily = interFamily, fontSize = 12.sp, color = FamilyColors.primary,
                                    )
                                }
                                if (!selMenstrual) {
                                    OutlinedButton(
                                        onClick = { onPeriodStart(sel); selectedDate = null },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, FamilyColors.menstrual),
                                    ) {
                                        Text(
                                            "Period Started",
                                            fontFamily = interFamily, fontSize = 12.sp, color = FamilyColors.menstrual,
                                        )
                                    }
                                }
                                if (selLog != null) {
                                    OutlinedButton(
                                        onClick = { onDeleteLog(sel); selectedDate = null },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    ) {
                                        Text(
                                            "Remove",
                                            fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun DotLegendRow(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = interFamily, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══ Insight Card ═══
@Composable
private fun InsightCard(state: FamilyUiState, onGenerateAi: () -> Unit) {
    val fallback = when (state.currentPhase) {
        CyclePhase.MENSTRUAL -> "Rest when needed — light movement helps ease cramps."
        CyclePhase.FOLLICULAR -> "Rising energy! Great time for workouts and creativity."
        CyclePhase.OVULATORY -> "Near your fertile window — energy and libido often peak."
        CyclePhase.LUTEAL -> "Progesterone is dominant — fatigue and mood shifts are normal."
    }
    val rawText = (state.aiInsight ?: fallback).replace(Regex(" {2,}"), " ").trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = FamilyColors.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Coach",
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = FamilyColors.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = boldFamilyInsight(rawText),
                    fontFamily = interFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            if (state.isLoadingInsight) {
                CircularProgressIndicator(Modifier.size(18.dp), color = FamilyColors.primary, strokeWidth = 2.dp)
            }
        }
    }
}

private fun boldFamilyInsight(text: String) = buildAnnotatedString {
    if (text.contains("**")) {
        val regex = Regex("""\*\*(.+?)\*\*""")
        var cursor = 0
        for (match in regex.findAll(text)) {
            append(text.substring(cursor, match.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    } else {
        append(text)
    }
}

// ═══ Daily Logger Bottom Sheet ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyLoggerSheet(
    existingLog: com.hopehealth.app.data.db.DailyCycleLogEntity?,
    targetDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (String?, String?, String, String?, String?, String?, Float?) -> Unit,
    onStartPeriod: () -> Unit,
    onEndPeriod: (LocalDate) -> Unit,
    hasActiveCycle: Boolean,
    periodStartDate: LocalDate? = null,
) {
    var flow by remember { mutableStateOf(existingLog?.flowIntensity) }
    var mucus by remember { mutableStateOf(existingLog?.cervicalMucus) }
    var selectedSymptoms by remember { mutableStateOf(
        existingLog?.symptoms?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    ) }
    var mood by remember { mutableStateOf(existingLog?.mood) }
    var sexDrive by remember { mutableStateOf(existingLog?.sexDrive) }
    var sexActivity by remember { mutableStateOf(existingLog?.sexualActivity) }
    var tempText by remember { mutableStateOf(existingLog?.temperature?.toString() ?: "") }
    var expandedInfo by remember { mutableStateOf<String?>(null) }

    val isToday = targetDate == LocalDate.now()
    val dateLabel = if (isToday) "Today" else "${targetDate.monthValue}/${targetDate.dayOfMonth}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Log · $dateLabel",
                fontFamily = interFamily, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = FamilyColors.primary,
            )
            Spacer(Modifier.height(16.dp))

            // Period button
            if (hasActiveCycle) {
                var showEndDatePicker by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FamilyColors.menstrual),
                ) { Text("Mark Period End", fontFamily = interFamily, fontWeight = FontWeight.SemiBold) }

                if (showEndDatePicker) {
                    val startMillis = periodStartDate
                        ?.atStartOfDay(ZoneOffset.UTC)
                        ?.toInstant()?.toEpochMilli() ?: 0L
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = targetDate
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant().toEpochMilli(),
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return utcTimeMillis >= startMillis
                            }
                        },
                    )
                    DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val picked = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.of("UTC"))
                                        .toLocalDate()
                                    onEndPeriod(picked)
                                }
                                showEndDatePicker = false
                            }) { Text("Confirm", fontFamily = interFamily, fontWeight = FontWeight.SemiBold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndDatePicker = false }) {
                                Text("Cancel", fontFamily = interFamily)
                            }
                        },
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = {
                                Text(
                                    "When did your period end?",
                                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                                    fontFamily = interFamily, fontWeight = FontWeight.SemiBold,
                                )
                            },
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onStartPeriod, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FamilyColors.menstrual),
                ) { Text("Period Started Today", fontFamily = interFamily, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(16.dp))

            // ── Temperature (most predictive) ──
            InfoSectionLabel(
                "Temperature (°F)", expandedInfo == "temp", { expandedInfo = if (expandedInfo == "temp") null else "temp" },
                "Basal body temperature (BBT) rises ~0.2–0.5°F after ovulation due to progesterone. This is the most reliable way to confirm ovulation. Measure orally each morning before getting out of bed.",
            )
            OutlinedTextField(
                tempText, { tempText = it.filter { c -> c.isDigit() || c == '.' } },
                Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. 97.8", fontFamily = interFamily) },
            )
            Spacer(Modifier.height(12.dp))

            // ── Cervical Mucus (#1 non-temp predictor) ──
            InfoSectionLabel(
                "Cervical Mucus", expandedInfo == "mucus", { expandedInfo = if (expandedInfo == "mucus") null else "mucus" },
                "Check when you wipe — notice the texture. Dry/sticky = low fertility. Slippery, stretchy, egg-white = peak fertility. This is the strongest non-temperature sign of approaching ovulation.",
            )
            ChipRow(
                CervicalMucusType.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                mucus?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() },
                { val raw = it.uppercase().replace(" ", "_"); mucus = if (mucus == raw) null else raw },
                FamilyColors.follicular,
            )
            Spacer(Modifier.height(12.dp))

            // ── Flow ──
            InfoSectionLabel(
                "Flow", expandedInfo == "flow", { expandedInfo = if (expandedInfo == "flow") null else "flow" },
                "Tracking flow intensity helps estimate period length, spot irregularities, and anchor the start of each cycle for predictions.",
            )
            ChipRow(FlowIntensity.entries.map { it.name }, flow,
                { flow = if (flow == it) null else it }, FamilyColors.menstrual)
            Spacer(Modifier.height(12.dp))

            // ── Symptoms ──
            InfoSectionLabel(
                "Symptoms", expandedInfo == "symptoms", { expandedInfo = if (expandedInfo == "symptoms") null else "symptoms" },
                "Physical symptoms correlate with cycle phases. Breast tenderness and fatigue often signal rising progesterone after ovulation. Cramps and bloating are common during menstruation.",
            )
            ChipRow(
                Symptom.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                null,
                { val raw = it.uppercase().replace(" ", "_"); selectedSymptoms = if (raw in selectedSymptoms) selectedSymptoms - raw else selectedSymptoms + raw },
                FamilyColors.luteal, true,
                selectedSymptoms.map { it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } }.toSet(),
            )
            Spacer(Modifier.height(12.dp))

            // ── Mood ──
            InfoSectionLabel(
                "Mood", expandedInfo == "mood", { expandedInfo = if (expandedInfo == "mood") null else "mood" },
                "Mood shifts are driven by hormonal changes — estrogen boosts energy pre-ovulation, while progesterone can cause irritability in the luteal phase.",
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                listOf("🤩" to "ENERGETIC", "😊" to "GOOD", "😐" to "NEUTRAL", "😔" to "LOW", "😤" to "IRRITABLE").forEach { (emoji, value) ->
                    val isSelected = mood == value
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FamilyColors.primaryLight.copy(alpha = 0.3f) else Color.Transparent)
                            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) FamilyColors.primary else Color.Transparent, CircleShape)
                            .clickable { mood = if (mood == value) null else value },
                        Alignment.Center,
                    ) { Text(emoji, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── Sex Drive ──
            InfoSectionLabel(
                "Sex Drive", expandedInfo == "sexdrive", { expandedInfo = if (expandedInfo == "sexdrive") null else "sexdrive" },
                "Libido naturally peaks near ovulation with rising estrogen. A soft fertility signal that's effortless to track.",
            )
            ChipRow(
                SexDriveLevel.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                sexDrive?.lowercase()?.replaceFirstChar { it.uppercase() },
                { val raw = it.uppercase(); sexDrive = if (sexDrive == raw) null else raw },
                FamilyColors.primaryLight,
            )
            Spacer(Modifier.height(12.dp))

            // ── Sexual Activity (least predictive, context only) ──
            InfoSectionLabel(
                "Sexual Activity", expandedInfo == "sex", { expandedInfo = if (expandedInfo == "sex") null else "sex" },
                "Helps with conception tracking in TTC mode and provides context for cycle-related symptoms. Not used for fertility predictions.",
            )
            ChipRow(
                SexualActivityType.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                sexActivity?.lowercase()?.replaceFirstChar { it.uppercase() },
                { val raw = it.uppercase(); sexActivity = if (sexActivity == raw) null else raw },
                FamilyColors.primary,
            )
            Spacer(Modifier.height(20.dp))

            // ── Save ──
            Button(
                onClick = { onSave(flow, mucus, selectedSymptoms.joinToString(","), mood, sexDrive, sexActivity, tempText.toFloatOrNull()) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamilyColors.primary),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Save", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoSectionLabel(
    text: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    info: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = if (isExpanded) 2.dp else 6.dp),
    ) {
        Text(
            text, fontFamily = interFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Outlined.Info, contentDescription = "Info about $text",
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle),
            tint = if (isExpanded) FamilyColors.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
    androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
        Text(
            info, fontFamily = interFamily, fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp),
                )
                .padding(10.dp),
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit, color: Color, multiSelect: Boolean = false, selectedSet: Set<String> = emptySet()) {
    options.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) { row.forEach { label -> val isSelected = if (multiSelect) label in selectedSet else label == selected
        FilterChip(isSelected, { onSelect(label) }, { Text(label, fontFamily = interFamily, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = color),
            border = FilterChipDefaults.filterChipBorder(borderColor = color.copy(alpha = 0.3f), selectedBorderColor = color, enabled = true, selected = isSelected), modifier = Modifier.weight(1f)) }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
}
