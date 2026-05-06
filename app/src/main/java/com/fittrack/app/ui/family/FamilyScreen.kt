package com.fittrack.app.ui.family

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittrack.app.data.CyclePhase
import com.fittrack.app.data.CervicalMucusType
import com.fittrack.app.data.FlowIntensity
import com.fittrack.app.data.MoodType
import com.fittrack.app.data.SexDriveLevel
import com.fittrack.app.data.SexualActivityType
import com.fittrack.app.data.Symptom
import com.fittrack.app.data.db.TemperatureReadingEntity
import com.fittrack.app.theme.FamilyColors
import com.fittrack.app.theme.interFamily
import com.fittrack.app.ui.common.ScreenScaffold
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogger by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Family", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = FamilyColors.primary)
                Icon(Icons.Filled.Favorite, null, tint = FamilyColors.primaryLight, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(16.dp))
            CycleStatusCard(state)
            Spacer(Modifier.height(16.dp))

            // TTC toggle
            if (state.currentCycle != null) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Trying to Conceive", fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Switch(checked = state.isTtcMode, onCheckedChange = { viewModel.toggleTtcMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = FamilyColors.ovulatory, checkedTrackColor = FamilyColors.ovulatory.copy(alpha = 0.3f)))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Temperature chart
            if (state.temperatureReadings.isNotEmpty()) {
                TemperatureChart(state.temperatureReadings, state.prediction?.coverlineTemp)
                Spacer(Modifier.height(16.dp))
            }

            CycleCalendar(state.viewingMonth, state.monthLogs, state.prediction, state.currentCycle,
                { viewModel.navigateMonth(-1) }, { viewModel.navigateMonth(1) })
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
            if (state.isTtcMode && state.conceptionProbability > 0f) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FamilyColors.fertileHigh.copy(alpha = 0.12f))) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Conception Probability", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, color = FamilyColors.fertileHigh)
                        Text("${(state.conceptionProbability * 100).toInt()}%", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = FamilyColors.fertileHigh)
                        Text("Time intercourse every 1–2 days during your fertile window", fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // AI Insight
            InsightCard(state, onGenerateAi = { viewModel.generateAiInsight() })
            Spacer(Modifier.height(16.dp))

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

        FloatingActionButton(onClick = { showLogger = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = FamilyColors.primary, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, "Log Today"); Spacer(Modifier.width(8.dp))
                Text("Log Today", fontFamily = interFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLogger) {
        DailyLoggerSheet(state.todayLog, { showLogger = false },
            { flow, mucus, symptoms, mood, sexDrive, sexActivity, temp ->
                viewModel.saveDailyLog(flowIntensity = flow, cervicalMucus = mucus, symptoms = symptoms,
                    mood = mood, sexDrive = sexDrive, sexualActivity = sexActivity, temperature = temp,
                    temperatureSource = if (temp != null) "MANUAL" else null)
                showLogger = false
            }, { viewModel.logPeriodStart(); showLogger = false }, { viewModel.logPeriodEnd(); showLogger = false },
            state.currentCycle?.endDate == null && state.currentPhase == CyclePhase.MENSTRUAL)
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
    if (dailyTemps.size < 3) return

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Temperature (°F)", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (coverline != null) Text("Coverline: ${"%.1f".format(coverline)}°F", fontFamily = interFamily, fontSize = 11.sp, color = FamilyColors.coverline)
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
                    Row { Text("🩸 Period in ~${state.daysUntilPeriod} days", fontFamily = interFamily, fontSize = 15.sp); Spacer(Modifier.width(12.dp))
                        val confLabel = state.confidence.name.lowercase().replaceFirstChar { it.uppercase() }
                        Text("· $confLabel confidence", fontFamily = interFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val fertColor = when (state.fertilityLevel) { "High" -> FamilyColors.fertileHigh; "Medium" -> FamilyColors.fertileMedium; else -> FamilyColors.fertileLow }
                        Box(Modifier.size(10.dp).background(fertColor, CircleShape)); Spacer(Modifier.width(6.dp))
                        Text("Fertility: ${state.fertilityLevel}", fontFamily = interFamily, fontSize = 15.sp)
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
private fun CycleCalendar(viewingMonth: YearMonth, monthLogs: List<com.fittrack.app.data.db.DailyCycleLogEntity>, prediction: com.fittrack.app.services.CyclePredictionEngine.CyclePrediction?, currentCycle: com.fittrack.app.data.db.CycleRecordEntity?, onPrev: () -> Unit, onNext: () -> Unit) {
    val logsByDate = monthLogs.associateBy { it.date }; val today = LocalDate.now()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = FamilyColors.primary) }
                Text("${viewingMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${viewingMonth.year}", fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = FamilyColors.primary) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) { for (d in listOf("M","T","W","T","F","S","S")) { Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Spacer(Modifier.height(4.dp))
            val firstDay = viewingMonth.atDay(1); val offset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7; val daysInMonth = viewingMonth.lengthOfMonth(); val rows = (offset + daysInMonth + 6) / 7
            for (row in 0 until rows) { Row(Modifier.fillMaxWidth()) { for (col in 0..6) { val dayNum = row * 7 + col - offset + 1
                if (dayNum in 1..daysInMonth) { val date = viewingMonth.atDay(dayNum); val log = logsByDate[date.toString()]; val isToday = date == today
                    val dotColor = when { log?.flowIntensity != null -> FamilyColors.menstrual; prediction != null && date in prediction.fertileWindowStart..prediction.fertileWindowEnd -> FamilyColors.ovulatory; else -> null }
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).then(if (isToday) Modifier.border(1.5.dp, FamilyColors.primary, CircleShape) else Modifier), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$dayNum", fontFamily = interFamily, fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) FamilyColors.primary else MaterialTheme.colorScheme.onSurface)
                            if (dotColor != null) Box(Modifier.size(5.dp).background(dotColor, CircleShape)) } }
                } else Spacer(Modifier.weight(1f)) } } }
    } }
}

// ═══ Insight Card ═══
@Composable
private fun InsightCard(state: FamilyUiState, onGenerateAi: () -> Unit) {
    val fallback = when (state.currentPhase) { CyclePhase.MENSTRUAL -> "Rest when needed — light movement helps ease cramps."; CyclePhase.FOLLICULAR -> "Rising energy! Great time for workouts and creativity."; CyclePhase.OVULATORY -> "Near your fertile window — energy and libido often peak."; CyclePhase.LUTEAL -> "Progesterone is dominant — fatigue and mood shifts are normal." }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = FamilyColors.primarySurface.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("🤖", fontSize = 24.sp)
                if (!state.isLoadingInsight) IconButton(onClick = onGenerateAi) { Icon(Icons.Default.AutoAwesome, "AI Insight", tint = FamilyColors.primary) }
                else CircularProgressIndicator(Modifier.size(20.dp), color = FamilyColors.primary, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(6.dp))
            Text(state.aiInsight ?: fallback, fontFamily = interFamily, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

// ═══ Daily Logger Bottom Sheet ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyLoggerSheet(existingLog: com.fittrack.app.data.db.DailyCycleLogEntity?, onDismiss: () -> Unit, onSave: (String?, String?, String, String?, String?, String?, Float?) -> Unit, onStartPeriod: () -> Unit, onEndPeriod: () -> Unit, hasPeriod: Boolean) {
    var flow by remember { mutableStateOf(existingLog?.flowIntensity) }; var mucus by remember { mutableStateOf(existingLog?.cervicalMucus) }
    var selectedSymptoms by remember { mutableStateOf(existingLog?.symptoms?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var mood by remember { mutableStateOf(existingLog?.mood) }; var sexDrive by remember { mutableStateOf(existingLog?.sexDrive) }; var sexActivity by remember { mutableStateOf(existingLog?.sexualActivity) }
    var tempText by remember { mutableStateOf(existingLog?.temperature?.toString() ?: "") }; var showMucusInfo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Log · ${LocalDate.now().monthValue}/${LocalDate.now().dayOfMonth}", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FamilyColors.primary)
            Spacer(Modifier.height(16.dp))
            if (hasPeriod) OutlinedButton(onClick = onEndPeriod, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = FamilyColors.menstrual)) { Text("Mark Period End", fontFamily = interFamily, fontWeight = FontWeight.SemiBold) }
            else OutlinedButton(onClick = onStartPeriod, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = FamilyColors.menstrual)) { Text("Period Started Today", fontFamily = interFamily, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))

            SectionLabel("Flow"); ChipRow(FlowIntensity.entries.map { it.name }, flow, { flow = if (flow == it) null else it }, FamilyColors.menstrual); Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) { SectionLabel("Cervical Mucus"); Spacer(Modifier.width(4.dp)); Text("ℹ️", Modifier.clickable { showMucusInfo = !showMucusInfo }, fontSize = 14.sp) }
            if (showMucusInfo) Text("Check when you wipe — notice the texture. Dry/sticky = low fertility. Slippery, stretchy, egg-white = peak fertility.", fontFamily = interFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            ChipRow(CervicalMucusType.entries.map { it.name.replace("_"," ").lowercase().replaceFirstChar { c -> c.uppercase() } }, mucus?.replace("_"," ")?.lowercase()?.replaceFirstChar { it.uppercase() }, { val raw = it.uppercase().replace(" ","_"); mucus = if (mucus == raw) null else raw }, FamilyColors.follicular); Spacer(Modifier.height(12.dp))

            SectionLabel("Symptoms"); ChipRow(Symptom.entries.map { it.name.replace("_"," ").lowercase().replaceFirstChar { c -> c.uppercase() } }, null, { val raw = it.uppercase().replace(" ","_"); selectedSymptoms = if (raw in selectedSymptoms) selectedSymptoms - raw else selectedSymptoms + raw }, FamilyColors.luteal, true, selectedSymptoms.map { it.replace("_"," ").lowercase().replaceFirstChar { c -> c.uppercase() } }.toSet()); Spacer(Modifier.height(12.dp))

            SectionLabel("Mood"); Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) { listOf("😊" to "GOOD","😐" to "NEUTRAL","😔" to "LOW","😤" to "IRRITABLE","⚡" to "ENERGETIC").forEach { (e,v) -> val s = mood == v; Box(Modifier.size(44.dp).clip(CircleShape).background(if (s) FamilyColors.primaryLight.copy(alpha = 0.3f) else Color.Transparent).border(if (s) 2.dp else 0.dp, if (s) FamilyColors.primary else Color.Transparent, CircleShape).clickable { mood = if (mood == v) null else v }, Alignment.Center) { Text(e, fontSize = 22.sp) } } }; Spacer(Modifier.height(12.dp))

            SectionLabel("Sex Drive"); ChipRow(SexDriveLevel.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, sexDrive?.lowercase()?.replaceFirstChar { it.uppercase() }, { val raw = it.uppercase(); sexDrive = if (sexDrive == raw) null else raw }, FamilyColors.primaryLight); Spacer(Modifier.height(12.dp))
            SectionLabel("Sexual Activity"); ChipRow(SexualActivityType.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }, sexActivity?.lowercase()?.replaceFirstChar { it.uppercase() }, { val raw = it.uppercase(); sexActivity = if (sexActivity == raw) null else raw }, FamilyColors.primary); Spacer(Modifier.height(12.dp))
            SectionLabel("Temperature (°F)"); OutlinedTextField(tempText, { tempText = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. 97.8", fontFamily = interFamily) }); Spacer(Modifier.height(20.dp))
            Button({ onSave(flow, mucus, selectedSymptoms.joinToString(","), mood, sexDrive, sexActivity, tempText.toFloatOrNull()) }, Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = FamilyColors.primary), shape = RoundedCornerShape(14.dp)) { Text("Save", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun SectionLabel(text: String) { Text(text, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp)) }

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit, color: Color, multiSelect: Boolean = false, selectedSet: Set<String> = emptySet()) {
    options.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) { row.forEach { label -> val isSelected = if (multiSelect) label in selectedSet else label == selected
        FilterChip(isSelected, { onSelect(label) }, { Text(label, fontFamily = interFamily, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color.copy(alpha = 0.2f), selectedLabelColor = color),
            border = FilterChipDefaults.filterChipBorder(borderColor = color.copy(alpha = 0.3f), selectedBorderColor = color, enabled = true, selected = isSelected), modifier = Modifier.weight(1f)) }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
}
